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
import com.example.besafe.activities.Utilities.setSubscriptionStatus
import com.example.besafe.databinding.ActivitySubscriptionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson

class SubscriptionActivity : AppCompatActivity() {

    private var billingClient: BillingClient? = null
    lateinit var binding: ActivitySubscriptionBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        ///////// This method is for getting subscription status, move this whole code to where you need this (bitch) //////
        getPurchaseDetail()

        binding.back.setOnClickListener {
            finish()
        }

        binding.plan.setOnClickListener {
            if (billingClient == null || billingClient?.isReady != true) {
                billingClient = BillingClient.newBuilder(this)
                    .enablePendingPurchases()
                    .setListener(purchaseUpdateListener)
                    .build()
                billingClient?.startConnection(object: BillingClientStateListener {
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
                        }
                        catch (ex: Exception) {
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

    //////////////////////////////////////////////////////
    fun getPurchaseDetail() {
        if (billingClient == null || !billingClient!!.isReady) {
            billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(purchaseUpdateListener)
                .build()
            billingClient!!.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryPurchasesDetail()
                    } else {
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
                        billingClient!!.connectionState
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
            queryPurchasesDetail()
        }
    }

    private fun queryPurchasesDetail() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { billingResult: BillingResult, purchases: List<Purchase?> ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchases.isNotEmpty()) {
                    handlePurchaseDetails(purchases)
                }
                else {
                    auth.currentUser?.let { setSubscriptionStatus(false, null,database, it.uid) }
                }
            } else {
                auth.currentUser?.let { setSubscriptionStatus(false, null,database, it.uid) }
                Log.e(
                    "Billing",
                    "Error querying purchases: " + billingResult.debugMessage
                )
            }
        }
    }

    private fun handlePurchaseDetails(purchases: List<Purchase?>) {
        if (purchases.isNotEmpty()) {
            for (purchase in purchases) {
                handlePurchases(purchase)
            }
        } else {
            Log.d("Subscription Details", "No Active Subscription were found")
            auth.currentUser?.let { setSubscriptionStatus(false, null,database, it.uid) }
        }
    }

    private fun handlePurchases(purchase: Purchase?) {
        if (purchase?.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val response: QueryPurchaseResponse = Gson().fromJson(
                purchase.originalJson,
                QueryPurchaseResponse::class.java
            )
            if (response.productId.equals(subscriptionId)) {
                if(response.autoRenewing == true) {
                    auth.currentUser?.let { setSubscriptionStatus(true, response,database, it.uid) }
                }
                else {
                    auth.currentUser?.let { setSubscriptionStatus(false, response,database, it.uid) }
                }
            }
            else {
                auth.currentUser?.let { setSubscriptionStatus(false, response,database, it.uid) }
            }
            Log.d("Subscription Details", purchase.originalJson)
        } else {
            auth.currentUser?.let { setSubscriptionStatus(false, null,database, it.uid) }
            purchase?.let { Log.d("Subscription Details (${purchase.purchaseState})", it.originalJson) }
        }
    }
}