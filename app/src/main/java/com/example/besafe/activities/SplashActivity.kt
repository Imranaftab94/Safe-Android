package com.example.besafe.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.besafe.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({

            auth = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance().reference
            // Check if user is already logged in
            if (auth.currentUser != null) {
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
                    Toast.makeText(this@SplashActivity, "Some error occured", Toast.LENGTH_SHORT).show()
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
}