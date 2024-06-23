package com.example.besafe.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.besafe.R
import com.example.besafe.databinding.ActivitySignUpBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.initialize

class SignUpActivity : AppCompatActivity() {
    lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        clickListner()

    }

    private fun clickListner() {
        binding.login.setOnClickListener {
            startActivity(Intent(this,LoginActivity::class.java))
            finish()
        }

        binding.createAccount.setOnClickListener {
            if (verify()){
                registerUser(binding.email.text.toString(),binding.password.text.toString())
            }

        }
    }

    private fun verify() :Boolean{
        if (binding.name.text.toString().isEmpty()){
            Utilities.alertDialog(this,getString(R.string.p_name),getString(R.string.alert),getString(R.string.ok))
            return false
        }
        if (binding.email.text.toString().isEmpty()){
            Utilities.alertDialog(this,getString(R.string.p_email),getString(R.string.alert),getString(R.string.ok))
            return false
        }
        if (binding.password.text.toString().isEmpty()){
            Utilities.alertDialog(this,getString(R.string.p_pass),getString(R.string.alert),getString(R.string.ok))
            return false
        }
        if (binding.confirmPassword.text.toString().isEmpty()){
            Utilities.alertDialog(this,getString(R.string.p_confirm_pass),getString(R.string.alert),getString(R.string.ok))
            return false
        }
        if (!binding.password.text.toString().equals(binding.confirmPassword.text.toString())){
            Utilities.alertDialog(this,getString(R.string.same_password),getString(R.string.alert),getString(R.string.ok))
            return false
        }

        return true
    }

    fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration successful
                    Utilities.showToast(getString(R.string.acc_create),this)
                    val user = auth.currentUser
                    user?.let {
                        val userId = it.uid
                        // Store user data in the database
                        storeUserData(userId, email)
                    }
                    startActivity(Intent(this,LoginActivity::class.java))
                    finish()
                } else {
                    // If registration fails, display a message to the user.
                    task.exception?.message?.let {
                        Utilities.alertDialog(this,it,getString(R.string.alert),getString(R.string.ok))
                    }
                }
            }
    }

    private fun storeUserData(userId: String, email: String) {
        val userId = auth.currentUser?.uid ?: return
        val user = User(
            admin = false,
            createdAt = System.currentTimeMillis().toDouble(),
            currentUserPhoneNumber = "",
            email = email,
            firstFriendPhoneNumber = "",
            fourthFriendPhoneNumber = "",
            name = binding.name.text.toString(),
            pin = "",
            secondFriendPhoneNumber = "",
            thirdFriendPhoneNumber = "",
            userDescription = "",
            userId = auth.currentUser?.uid ?: "",
            isSubscribe = false
        )
        database.child("user_test").child(userId).setValue(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
//                    Utilities.showToast("User data inserted into user_test",this)
                } else {
                    Toast.makeText(this, "Failed to insert user data: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }

    }
}