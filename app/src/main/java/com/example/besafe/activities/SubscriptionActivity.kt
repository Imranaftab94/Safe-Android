package com.example.besafe.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.besafe.R
import com.example.besafe.databinding.ActivitySubscriptionBinding
import com.android.billingclient.api.*

class SubscriptionActivity : AppCompatActivity() {

    private lateinit var billingClient: BillingClient
    lateinit var binding: ActivitySubscriptionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener {
            finish()
        }

        billingClient = BillingClient.newBuilder(this)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            .enablePendingPurchases()
            .build()

        startConnection()

        binding.plan.setOnClickListener {
            queryAvailableProductsAndLaunchBillingFlow()
        }

        binding.manage.setOnClickListener {
            val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Connection to the service was established successfully
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry connection
            }
        })
    }

    private fun queryAvailableProductsAndLaunchBillingFlow() {
        val skuList = listOf("your_subscription_product_id")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)
        billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                for (skuDetails in skuDetailsList) {
                    if (skuDetails.sku == "your_subscription_product_id") {
                        launchBillingFlow(skuDetails)
                    }
                }
            }
        }
    }

    private fun launchBillingFlow(skuDetails: SkuDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        billingClient.launchBillingFlow(this, flowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                billingClient.acknowledgePurchase(acknowledgePurchaseParams.build()) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Grant entitlement to the user
                    }
                }
            }
        }
    }
}