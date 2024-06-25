package com.example.besafe.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.example.besafe.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var billingClient: BillingClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({

            auth = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance().reference
            // Check if user is already logged in
            if (auth.currentUser != null) {
                getPurchaseDetail()
                checkUserInUserTest(auth.currentUser!!)
            }else{
                startActivity(Intent(this, LoginActivity::class.java))
                // Close this activity
                finish()
            }

        }, 3000)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun checkUserInUserTest(user: FirebaseUser) {
        val userId = user.uid
        database.child("user_test").child(userId).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val currentUserPhoneNumber = dataSnapshot.child("currentUserPhoneNumber").getValue(String::class.java)
                    if (currentUserPhoneNumber.isNullOrEmpty()) {
                        navigateToConfigurationActivity()
                    } else {
                        navigateToMainActivity()
                    }
                } else {
//                    Toast.makeText(this@LoginActivity, "User data not found in user_test table", Toast.LENGTH_SHORT).show()
                    Toast.makeText(this@SplashActivity, getString(R.string.some_error), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@SplashActivity, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToConfigurationActivity() {
        val intent = Intent(this, ConfigurationActivity::class.java)
        intent.putExtra("fromLogin",true)
        startActivity(intent)
        finish()
    }

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
                                this@SplashActivity,
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
                            this@SplashActivity,
                            getString(R.string.bill_disconnected),
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
                    this@SplashActivity,
                    "Feature not Supported",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else if (billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
            runOnUiThread {
                Toast.makeText(
                    this@SplashActivity,
                    "Service Disconnected, Please try again",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else if (billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
            runOnUiThread {
                Toast.makeText(
                    this@SplashActivity,
                    "Service Unavailable, Please try again later",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else if (billingResult.responseCode == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
            runOnUiThread {
                Toast.makeText(
                    this@SplashActivity,
                    "Service Error, Please contact to support",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else if (billingResult.responseCode == BillingClient.BillingResponseCode.ERROR) {
            runOnUiThread {
                Toast.makeText(
                    this@SplashActivity,
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
                        auth.currentUser?.let {
                            Utilities.setSubscriptionStatus(
                                true,
                                null,
                                database,
                                it.uid
                            )
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@SplashActivity,
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
                auth.currentUser?.let {
                    Utilities.setSubscriptionStatus(
                        true,
                        null,
                        database,
                        it.uid
                    )
                }
            }
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
                    auth.currentUser?.let {
                        Utilities.setSubscriptionStatus(
                            false,
                            null,
                            database,
                            it.uid
                        )
                    }
                }
            } else {
                auth.currentUser?.let {
                    Utilities.setSubscriptionStatus(
                        false,
                        null,
                        database,
                        it.uid
                    )
                }
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
            auth.currentUser?.let { Utilities.setSubscriptionStatus(false, null, database, it.uid) }
        }
    }

    private fun handlePurchases(purchase: Purchase?) {
        if (purchase?.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val response: QueryPurchaseResponse = Gson().fromJson(
                purchase.originalJson,
                QueryPurchaseResponse::class.java
            )
            if (response.productId.equals(Constants.subscriptionId)) {
                if(response.autoRenewing == true) {
                    auth.currentUser?.let {
                        Utilities.setSubscriptionStatus(
                            true,
                            response,
                            database,
                            it.uid
                        )
                    }
                }
                else {
                    auth.currentUser?.let {
                        Utilities.setSubscriptionStatus(
                            false,
                            response,
                            database,
                            it.uid
                        )
                    }
                }
            }
            else {
                auth.currentUser?.let {
                    Utilities.setSubscriptionStatus(
                        false,
                        response,
                        database,
                        it.uid
                    )
                }
            }
            Log.d("Subscription Details", purchase.originalJson)
        } else {
            auth.currentUser?.let { Utilities.setSubscriptionStatus(false, null, database, it.uid) }
            purchase?.let { Log.d("Subscription Details (${purchase.purchaseState})", it.originalJson) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingClient?.endConnection()
    }
}