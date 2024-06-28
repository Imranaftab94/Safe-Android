package com.example.besafe.activities

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.besafe.activities.Constants.Companion.subStatus
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener


object Utilities {

    lateinit var user: User

    fun alertDialog(context: Context, msg:String,alert:String,ok:String){
        val builder1 = AlertDialog.Builder(context)
        builder1.setTitle(alert)
        builder1.setMessage(msg)
        builder1.setCancelable(false)

        builder1.setPositiveButton(
            ok
        ) { dialog, id -> dialog.cancel() }

        val alert11 = builder1.create()
        alert11.show()
    }

    fun showToast(message: String,context: Context) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view: View? = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0)
    }

    fun setUserData(user: User){
        this.user=user
    }

    fun getUserData():User{
        return this.user
    }

    fun getUserDB(user: FirebaseUser,context: Context,database:DatabaseReference) {
        val userId = user.uid
        database.child(Constants.url).child(userId).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val userData = dataSnapshot.getValue(User::class.java)
                    userData?.let { setUserData(it) }
                } else {
//                    Toast.makeText(this@MainActivity, "User data not found in user table", Toast.LENGTH_SHORT).show()
                    Toast.makeText(context, "Some error occurred", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    fun setSubscriptionStatus(isSubscribed: Boolean = false, data: QueryPurchaseResponse?, database: DatabaseReference, userId:String) {
        val userRef = database.child(Constants.url).child(userId)
        val updates = hashMapOf<String, Any>("isSubscribe" to isSubscribed)
        userRef.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Update successful
//                Toast.makeText(this, "value updated successfully.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun checkTrialPeriod(database: DatabaseReference,userId:String) {
        database.child(Constants.url).child(userId).child("createdAt")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val createdAt = dataSnapshot.getValue(Double::class.java)
                    createdAt?.let {
                        val currentTime = System.currentTimeMillis().toDouble()
                        val oneWeekMillis = 7 * 24 * 60 * 60 * 1000
                        if (currentTime - createdAt > oneWeekMillis) {
                            Constants.freeTrial=false
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle possible errors.
                }
            })
    }

    fun checkSubscription(database: DatabaseReference,userId:String) {
        database.child(Constants.url).child(userId).child("isSubscribe")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val v = dataSnapshot.getValue(Boolean::class.java)
                    v?.let {
                        subStatus = v
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle possible errors.
                }
            })
    }
}