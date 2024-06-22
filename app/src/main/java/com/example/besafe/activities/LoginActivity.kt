package com.example.besafe.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Toast
import com.example.besafe.R
import com.example.besafe.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        clickListner()

        binding.rememberMe.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                val sharedPref = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putBoolean("remember_me", true)
                editor.apply()
            } else {
                val sharedPref = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putBoolean("remember_me", false)
                editor.apply()
            }
        })

    }

    private fun clickListner() {
        binding.createAccount.setOnClickListener {
            startActivity(Intent(this,SignUpActivity::class.java))
            finish()
        }

        binding.forgot.setOnClickListener {
            startActivity(Intent(this,ForgotPasswordActivity::class.java))
            finish()
        }

        binding.login.setOnClickListener {

            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Utilities.showToast(getString(R.string.enter_mail_pass),this)
            }
        }

        binding.google.setOnClickListener {
            signIn()
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Utilities.showToast(getString(R.string.login_succ),this)
                    // Navigate to another activity or do something
                    val user = auth.currentUser
                    user?.let { checkUserInUserTest(it) }
                } else {
                    // If sign in fails
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
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
                    Toast.makeText(this@LoginActivity, getString(R.string.some_error), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToConfigurationActivity() {
        val intent = Intent(this, ConfigurationActivity::class.java)
        intent.putExtra("fromLogin",true)
        startActivity(intent)
        finish()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign-In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign-In failed, update UI appropriately
                Utilities.showToast(getString(R.string.some_error),this)
                // ...
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    if (user != null) {
                        checkUserIdExists(user)

                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Utilities.showToast(getString(R.string.some_error),this)
                    // Update UI
                }
            }
    }

    private fun storeUserData(userId: String, email: String,name:String) {
        val userId = auth.currentUser?.uid ?: return
        val user = User(
            admin = false,
            createdAt = System.currentTimeMillis().toDouble(),
            currentUserPhoneNumber = "",
            email = email,
            firstFriendPhoneNumber = "",
            fourthFriendPhoneNumber = "",
            name = name,
            pin = "",
            secondFriendPhoneNumber = "",
            thirdFriendPhoneNumber = "",
            userDescription = "",
            userId = auth.currentUser?.uid ?: ""
        )
        database.child("user_test").child(userId).setValue(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
//                    Utilities.showToast("User data inserted into user_test",this)
                    navigateToConfigurationActivity()
                } else {
                    Toast.makeText(this, "Failed to insert user data: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }

    }

    private fun checkUserIdExists(user: FirebaseUser) {
        database.child("user_test").child(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // User ID exists
                    user.let { checkUserInUserTest(it) }
//                    Toast.makeText(this@LoginActivity, "User ID exists", Toast.LENGTH_SHORT).show()
                } else {
                    storeUserData(user.uid,user.email.toString(),user.displayName.toString())
                    // User ID does not exist
//                    Toast.makeText(this@LoginActivity, "User ID does not exist", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
                Toast.makeText(this@LoginActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}