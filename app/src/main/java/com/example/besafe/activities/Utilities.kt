package com.example.besafe.activities

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.besafe.activities.Constants.Companion.subStatus
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
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

    fun setUserData(user: User){
        this.user=user
    }

    fun getUserData():User{
        return this.user
    }

    fun getUserDB(user: FirebaseUser,context: Context,database:DatabaseReference) {
        val userId = user.uid
        database.child("user_test").child(userId).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val userData = dataSnapshot.getValue(User::class.java)
                    userData?.let { setUserData(it) }
                } else {
//                    Toast.makeText(this@MainActivity, "User data not found in user_test table", Toast.LENGTH_SHORT).show()
                    Toast.makeText(context, "Some error occurred", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun setSubscriptionStatus(isSubscribed: Boolean = false, data: QueryPurchaseResponse?,database: DatabaseReference,userId:String) {
        val userRef = database.child("user_test").child(userId)
        val updates = hashMapOf<String, Any>("isSubscribe" to isSubscribed)
        userRef.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Update successful
//                Toast.makeText(this, "value updated successfully.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun checkTrialPeriod(database: DatabaseReference,userId:String) {
        database.child("user_test").child(userId).child("createdAt")
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
        database.child("user_test").child(userId).child("isSubscribe")
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