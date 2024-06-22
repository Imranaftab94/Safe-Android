package com.example.besafe.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.example.besafe.activities.Constants.Companion.subscriptionId
import com.example.besafe.databinding.ActivitySubscriptionBinding

class SubscriptionActivity : AppCompatActivity() {

    private var billingClient: BillingClient? = null
    lateinit var binding: ActivitySubscriptionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener {
            finish()
        }

        binding.plan.setOnClickListener {
            if (billingClient == null || billingClient?.isReady != true) {
                billingClient = BillingClient.newBuilder(this)
                    .enablePendingPurchases()
                    .setListener(purchaseUpdateListener)
                    .build()
                billingClient?.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            queryAvailableProducts(subscriptionId)
                        }
                        else {
                            Log.e(
                                "ZKB1989",
                                "Billing setup failed" + billingResult.debugMessage + billingResult
                            )

                            runOnUiThread {
                                Toast.makeText(
                                    this@SubscriptionActivity,
                                    billingResult.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        runOnUiThread {
                            Toast.makeText(
                                this@SubscriptionActivity,
                                "Billing Disconnected",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
            }
            else {
                queryAvailableProducts(subscriptionId)
            }
        }

        binding.manage.setOnClickListener {
            val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }

    private fun queryAvailableProducts(productId: String) {
        try {
            val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build())

                ).build()
            billingClient?.queryProductDetailsAsync(queryProductDetailsParams) {
                billingResult: BillingResult, productDetailsList: List<ProductDetails> ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (productDetailsList.isEmpty()) {
                        runOnUiThread {
                            Toast.makeText(
                                this@SubscriptionActivity,
                                "Product is not available on App Store",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        try {
                            for (i in productDetailsList.indices) {
                                if (productDetailsList[i].productId == productId) {
                                    val productDetailsParamsList: List<ProductDetailsParams> =
                                        listOf(
                                            ProductDetailsParams.newBuilder()
                                                .setProductDetails(productDetailsList[i])
                                                .setOfferToken(
                                                    productDetailsList[i]
                                                        .subscriptionOfferDetails!![1]
                                                        .offerToken
                                                ).build()
                                        )
                                    val billingFlowParams =
                                        BillingFlowParams.newBuilder()
                                            .setProductDetailsParamsList(productDetailsParamsList)
                                            .build()
                                    val responseCode = billingClient?.launchBillingFlow(
                                        this@SubscriptionActivity,
                                        billingFlowParams
                                    )?.responseCode
                                }
                            }
                        } catch (ex: Exception) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@SubscriptionActivity,
                                    ex.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            Log.e("ZKB1989", ex.message?:"")
                        }
                    }
                }
                else {
                    runOnUiThread {
                        Toast.makeText(
                            this@SubscriptionActivity,
                            "Failed to retrieve available products",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("ZKB1989", ex.message?:"")
        }
    }

    private val purchaseUpdateListener = PurchasesUpdatedListener { billingResult: BillingResult, purchases: List<Purchase?>? ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            for (purchase in purchases) {
                handlePurchase(purchase!!)
                break
            }
        }
        else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) { }
        else if (billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
            runOnUiThread {
                Toast.makeText(
                    this@SubscriptionActivity,
                    "Feature not Supported",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else if (billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
            runOnUiThread {
                Toast.makeText(
                    this@SubscriptionActivity,
                    "Service Disconnected, Please try again",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else if (billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
            runOnUiThread {
                Toast.makeText(
                    this@SubscriptionActivity,
                    "Service Unavailable, Please try again later",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else if (billingResult.responseCode == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
            runOnUiThread {
                Toast.makeText(
                    this@SubscriptionActivity,
                    "Service Error, Please contact to support",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else if (billingResult.responseCode == BillingClient.BillingResponseCode.ERROR) {
            runOnUiThread {
                Toast.makeText(
                    this@SubscriptionActivity,
                    "Error: " + billingResult.debugMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            Log.d("ZKB1989", purchase.purchaseToken)
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(
                    acknowledgePurchaseParams
                ) { billingResult: BillingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.e("ZKB1989", "ConsumeResponseListener Response Code:  " + billingResult.responseCode)
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@SubscriptionActivity,
                                billingResult.debugMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Log.e(
                            "ZKB1989",
                            "ConsumeResponseListener Response Code:  (NOT OK)" + billingResult.responseCode
                        )
                    }
                }
            } else {
                Log.d("ZKB1989", "Already Acknowledged")
            }
        }
    }
}