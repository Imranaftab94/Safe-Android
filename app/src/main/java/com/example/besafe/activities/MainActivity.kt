package com.example.besafe.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.content.pm.PackageManager
import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.location.Location
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.example.besafe.R
import com.example.besafe.databinding.ActivityMainBinding
import android.view.inputmethod.InputMethodManager
import android.widget.TextView.OnEditorActionListener
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var activated:Boolean=false
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    var time:Long=11000
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var sharedPreferences: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
    var allowLocation=false
    private var isLongClick = false
    private val locationRequest = LocationRequest.Builder(
        LocationRequest.PRIORITY_HIGH_ACCURACY,
        1000L
    ).build()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        auth.currentUser?.let { Utilities.checkTrialPeriod(database, it.uid) }
        auth.currentUser?.let { Utilities.checkSubscription(database, it.uid) }
        sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
        allowLocation = sharedPreferences.getBoolean("allow_location",false)
        setViews()
        val user = auth.currentUser
        user?.let { getUserData(it) }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        checkLocationPermissions()

        // Handle drawer item clicks
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_configuration -> {
                    startActivity(Intent(this,ConfigurationActivity::class.java))
                    true
                }
                R.id.nav_plans -> {
                    startActivity(Intent(this,SubscriptionActivity::class.java))
                    true
                }
                R.id.nav_delete -> {
                    deleteDialog()
                    true
                }
                R.id.nav_logout ->{
                    logoutUser()
                    true
                }
                else -> false
            }.also {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

        setEditTextListeners()
        binding.etDigit4.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                var s:String= binding.etDigit1.text.toString()+binding.etDigit2.text.toString()+binding.etDigit3.text.toString()+binding.etDigit4.text.toString()
                if (s.equals(Utilities.getUserData().pin)){
                    setViews()
                    countDownTimer.cancel()
                }else{
                    binding.etDigit4.setError(getString(R.string.invalid_pin))
                }

                true
            } else {
                false
            }
        })

        binding.sideMenu.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }

//        binding.circleTextView.setOnLongClickListener {
//            if(Constants.freeTrial || Constants.subStatus){
//            activated= !activated
//            val drawable = binding.circleTextView.background as GradientDrawable
//            if (activated){
//                binding.txtDescription.text= getString(R.string.pin_safe)
//                binding.circleTextView.text=getString(R.string.activated)
//                binding.circleTextView.setTextColor(Color.parseColor("#ffffff"));
//                binding.txtO.visibility=View.VISIBLE
//                binding.otpLayout.visibility=View.VISIBLE
//                binding.pinLayout.visibility=View.VISIBLE
//                binding.etDigit1.requestFocus()
//                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.showSoftInput(binding.etDigit1, InputMethodManager.SHOW_IMPLICIT)
//                startCountdownTimer()
//                drawable.setColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
//            }else{
//                binding.txtDescription.text=getString(R.string.release_des)
//                binding.circleTextView.text=getString(R.string.hold)
//                binding.circleTextView.setTextColor(Color.parseColor("#000000"));
//                binding.txtO.visibility=View.GONE
//                binding.otpLayout.visibility=View.GONE
//                binding.pinLayout.visibility=View.GONE
//                drawable.setColor(ContextCompat.getColor(this, android.R.color.white))
//            }
//            }else{
//                notSubscribeDialog()
//            }
//            true
//        }

        binding.circleTextView.setOnLongClickListener {
            if(Constants.freeTrial || Constants.subStatus){
                isLongClick = true
                binding.circleTextView.text=getString(R.string.activated)
                binding.circleTextView.setTextColor(Color.parseColor("#ffffff"))
                val drawable = binding.circleTextView.background as GradientDrawable
                drawable.setColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            }else{
                notSubscribeDialog()
            }
            true // Return true to indicate that the event is consumed
        }

        binding.circleTextView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    if(Constants.freeTrial || Constants.subStatus){
                        if (isLongClick) {
                            binding.txtDescription.text= getString(R.string.pin_safe)
                            binding.txtO.visibility=View.VISIBLE
                            binding.otpLayout.visibility=View.VISIBLE
                            binding.pinLayout.visibility=View.VISIBLE
                            binding.etDigit1.requestFocus()
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(binding.etDigit1, InputMethodManager.SHOW_IMPLICIT)
                            startCountdownTimer()
                            isLongClick = false
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setViews() {
        Utilities.hideKeyboard(this)
        binding.circleTextView.isEnabled=true
        binding.txtDescription.text=getString(R.string.release_des)
        binding.circleTextView.text=getString(R.string.hold)
        binding.circleTextView.setTextColor(Color.parseColor("#000000"));
        binding.txtO.visibility=View.GONE
        binding.otpLayout.visibility=View.GONE
        binding.pinLayout.visibility=View.GONE
        time=11000
        binding.etDigit1.setText("")
        binding.etDigit2.setText("")
        binding.etDigit3.setText("")
        binding.etDigit4.setText("")
        val drawable = binding.circleTextView.background as GradientDrawable
        drawable.setColor(ContextCompat.getColor(this, android.R.color.white))
    }

    private fun setEditTextListeners() {
        binding.etDigit1.addTextChangedListener(PinTextWatcher(binding.etDigit1, binding.etDigit2))
        binding.etDigit2.addTextChangedListener(PinTextWatcher(binding.etDigit2, binding.etDigit3, binding.etDigit1))
        binding.etDigit3.addTextChangedListener(PinTextWatcher(binding.etDigit3, binding.etDigit4, binding.etDigit2))
        binding.etDigit4.addTextChangedListener(PinTextWatcher(binding.etDigit4, null, binding.etDigit3))
    }

    inner class PinTextWatcher(
        private val currentEditText: EditText,
        private val nextEditText: EditText?,
        private val previousEditText: EditText? = null
    ) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s?.length == 1) {
                nextEditText?.requestFocus()
            } else if (s?.length == 0) {
                previousEditText?.requestFocus()
            }
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    private fun startCountdownTimer() {
        binding.circleTextView.isEnabled=false
        countDownTimer = object : CountDownTimer(time, 1000) { // 10 seconds in milliseconds
            override fun onTick(millisUntilFinished: Long) {
                // Update the TextView with the remaining time
                binding.count.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                // Timer finished
                binding.count.text = "0"
                binding.circleTextView.isEnabled=true
                setViews()
                getLocationAndSendMessage()
            }
        }
        countDownTimer.start()
    }

    private fun logoutUser() {
        auth.signOut()
        googleSignInClient.signOut()
        Constants.freeTrial=true
        Constants.subStatus=false
        val sharedPref = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("remember_me", false)
        editor.apply()
        // Navigate back to login screen or any other appropriate screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun deleteDialog(){
        val builder1 = AlertDialog.Builder(this)
        builder1.setTitle(getString(R.string.alert))
        builder1.setMessage(getString(R.string.confirm_delete))
        builder1.setCancelable(false)

        builder1.setPositiveButton(
            getString(R.string.ok)
        ) { dialog, id ->
            deleteUserAccount()
            dialog.cancel()
        }

        builder1.setNegativeButton(
            getString(R.string.cancel)
        ) { dialog, id -> dialog.cancel() }

        val alert11 = builder1.create()
        alert11.show()
    }

    private fun deleteUserAccount() {
        val user: FirebaseUser? = auth.currentUser

        if (user != null) {
            // Delete user data from Realtime Database
            val userId = user.uid
            database.child("user_test").child(userId).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Delete user authentication record
                    user.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Toast.makeText(this, getString(R.string.acc_delete), Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to delete account: ${deleteTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Failed to delete user data: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getUserData(user: FirebaseUser) {
        val userId = user.uid
        database.child("user_test").child(userId).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val userData = dataSnapshot.getValue(User::class.java)
                    userData?.let { Utilities.setUserData(it) }
                    val h=binding.navView.getHeaderView(0)
                    val titleTextView: TextView = h.findViewById(R.id.tvUserName)
                    titleTextView.setText(userData?.name ?:"")
                } else {
//                    Toast.makeText(this@MainActivity, "User data not found in user_test table", Toast.LENGTH_SHORT).show()
                    Toast.makeText(this@MainActivity, getString(R.string.some_error), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@MainActivity, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun getLocationAndSendMessage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            sendMessageWithEmptyLocation()
            return
        }
//        if (allowLocation){
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        val locationUrl = "https://www.google.com/maps?q=$latitude,$longitude"

                        sendMessage(locationUrl)
                    } else {
                        sendMessageWithEmptyLocation()
                    }
                }
                .addOnFailureListener {
                    sendMessageWithEmptyLocation()
                }
//        }else{
//            sendMessageWithEmptyLocation()
//        }

    }

    private fun sendMessage(locationUrl: String) {
        val request = MessageRequest(
            phoneNumbers = listOf(Utilities.getUserData().firstFriendPhoneNumber, Utilities.getUserData().secondFriendPhoneNumber, Utilities.getUserData().thirdFriendPhoneNumber, Utilities.getUserData().fourthFriendPhoneNumber),
            isTest = false,
            location = locationUrl,
            name = Utilities.getUserData().name
        )

//        var map= HashMap<String,Any>()
//        map.put("phoneNumbers","[${Utilities.getUserData().firstFriendPhoneNumber},${Utilities.getUserData().secondFriendPhoneNumber},${Utilities.getUserData().thirdFriendPhoneNumber},${Utilities.getUserData().fourthFriendPhoneNumber}]")
//        map.put("isTest",false)
//        map.put("location",locationUrl)
//        map.put("name",Utilities.getUserData().name)
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val call = apiService.sendMessage(request)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, getString(R.string.mess_sent), Toast.LENGTH_SHORT).show()
                }
                else {
//                    Toast.makeText(this@MainActivity, "Failed to send message. Response code: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
//                Toast.makeText(this@MainActivity, "Failed to send message. Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendMessageWithEmptyLocation() {
        sendMessage("")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val sharedPref = getSharedPreferences("MySharedPref", MODE_PRIVATE)
        val b = sharedPref.getBoolean("remember_me",false)
        if (!b){
            auth.signOut()
            googleSignInClient.signOut()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val sharedPref = getSharedPreferences("MySharedPref", MODE_PRIVATE)
        val b = sharedPref.getBoolean("remember_me",false)
        if (!b){
            auth.signOut()
            googleSignInClient.signOut()
        }
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            checkLocationSettings()
        }
    }

    private fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { response ->
            val states = response.locationSettingsStates
            if (states != null) {
                if (!states.isLocationPresent) {
                    promptEnableLocation()
                }
            }
        }

        task.addOnFailureListener { exception ->
            promptEnableLocation()
        }
    }

    private fun promptEnableLocation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.enable_loc))
            .setMessage(getString(R.string.loc_des))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
//                Toast.makeText(this, "Location services are required to use this app", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                checkLocationSettings()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private fun notSubscribeDialog() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.layout_not_subscribe, null)

        // Create the alert dialog
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        alertDialog.setCancelable(true)

        // Get references to the views in the custom layout
        val btnSubscribe: TextView = dialogView.findViewById(R.id.subscribe)
        val btnOk: TextView = dialogView.findViewById(R.id.ok)

        // Set up the buttons
        btnOk.setOnClickListener {
            alertDialog.dismiss()
        }

        btnSubscribe.setOnClickListener {
            startActivity(Intent(this,SubscriptionActivity::class.java))
            alertDialog.dismiss()
        }

        // Show the dialog
        alertDialog.show()
    }
}