package com.example.besafe.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.besafe.R
import com.example.besafe.databinding.ActivityConfigurationBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class ConfigurationActivity : AppCompatActivity() {

    lateinit var binding: ActivityConfigurationBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var user:User
    var fromLogin=false
    var configDone=false
    var count=1
    private var isLongClick = false
    lateinit var sharedPreferences:SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
    var activated:Boolean=false
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var time:Long=11000

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        auth.currentUser?.let { Utilities.checkTrialPeriod(database, it.uid) }
        auth.currentUser?.let { Utilities.checkSubscription(database, it.uid) }
        sharedPreferences = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fromLogin= intent.getBooleanExtra("fromLogin",false)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        initializeSwitch()
        binding.back.setOnClickListener {
            finish()
        }

        if (!fromLogin){
            user=Utilities.getUserData()
            binding.layoutUsername.visibility=View.VISIBLE
            binding.layoutPn.visibility=View.VISIBLE
            binding.layoutRpn.visibility=View.VISIBLE
            binding.layoutSp.visibility=View.VISIBLE
            binding.layoutRsp.visibility=View.VISIBLE
            binding.layoutFirstFriend.visibility=View.VISIBLE
            binding.layoutSecondFriend.visibility=View.VISIBLE
            binding.layoutThirdFriend.visibility=View.VISIBLE
            binding.layoutFourthFriend.visibility=View.VISIBLE
            binding.circleTextView.visibility=View.VISIBLE
            binding.nextButton.text=getString(R.string.confirm)
            showCustomAlertDialog()

            binding.edUsername.setText(user.name)
            binding.edPn.setText(user.currentUserPhoneNumber)
            binding.edRpn.setText(user.currentUserPhoneNumber)
            binding.edFirstPn.setText(user.firstFriendPhoneNumber)
            binding.edSecondPn.setText(user.secondFriendPhoneNumber)
            if (user.thirdFriendPhoneNumber.isNotEmpty()) binding.edThirdPn.setText(user.thirdFriendPhoneNumber)
            if (user.fourthFriendPhoneNumber.isNotEmpty()) binding.edFourthPn.setText(user.fourthFriendPhoneNumber)

            binding.nextButton.setOnClickListener {
                if (binding.edUsername.text.isEmpty()){
                    Utilities.alertDialog(this,getString(R.string.p_username),getString(R.string.alert),getString(R.string.ok))
                    return@setOnClickListener
                }
                if (binding.edPn.text.isEmpty()){
                    Utilities.alertDialog(this,getString(R.string.p_pn),getString(R.string.alert),getString(R.string.ok))
                    return@setOnClickListener
                }
                if (binding.edRpn.text.isEmpty()){
                    Utilities.alertDialog(this,getString(R.string.p_rpn),getString(R.string.alert),getString(R.string.ok))
                    return@setOnClickListener
                }
                if (!binding.edRpn.text.toString().equals(binding.edPn.text.toString())){
                    Utilities.alertDialog(this,getString(R.string.s_pn),getString(R.string.alert),getString(R.string.ok))
                    return@setOnClickListener
                }
                if (binding.edPin.text.isNotEmpty() || binding.edRpin.text.isNotEmpty()){
                    if (!binding.edPin.text.toString().equals(binding.edRpin.text.toString())){
                        Utilities.alertDialog(this,getString(R.string.pin_same),getString(R.string.alert),getString(R.string.ok))
                        return@setOnClickListener
                    }
                }

                if (binding.edFirstPn.text.isEmpty()){
                    Utilities.alertDialog(this,getString(R.string.please_first),getString(R.string.alert),getString(R.string.ok))
                    return@setOnClickListener
                }

                if (binding.edSecondPn.text.isEmpty()){
                    Utilities.alertDialog(this,getString(R.string.please_second),getString(R.string.alert),getString(R.string.ok))
                    return@setOnClickListener
                }
                updateUserData2()
            }

        }else{
            binding.nextButton.setOnClickListener {
                count=count+1
                if (count==2){
                    binding.layoutUsername.visibility= View.VISIBLE
                    val anim= AnimationUtils.loadAnimation(this,R.anim.slide_in)
                    binding.layoutUsername.startAnimation(anim)
                }else if (count==3){

                    if (binding.edUsername.text.isEmpty()){
                        count=2
                        Utilities.alertDialog(this@ConfigurationActivity,getString(R.string.p_username),getString(R.string.alert),getString(R.string.ok))
                    }else{
                        binding.layoutPn.visibility= View.VISIBLE
                        val anim= AnimationUtils.loadAnimation(this,R.anim.slide_in)
                        binding.layoutPn.startAnimation(anim)
                    }
                }else if (count==4){

                    val v=binding.edPn.text.toString()
                    if(!v.startsWith("+")){
                        count=3
                        Utilities.alertDialog(this@ConfigurationActivity,getString(R.string.number_valid),getString(R.string.alert),getString(R.string.ok))
                    }else{
                        binding.layoutRpn.visibility= View.VISIBLE
                        val anim= AnimationUtils.loadAnimation(this,R.anim.slide_in)
                        binding.layoutRpn.startAnimation(anim)
                    }
                }else if (count==5){

                    val v=binding.edRpn.text.toString()
                    if(!v.startsWith("+")){
                        count=4
                        Utilities.alertDialog(this@ConfigurationActivity,getString(R.string.number_valid),getString(R.string.alert),getString(R.string.ok))
                    }else if (!v.equals(binding.edPn.text.toString())){
                        count=4
                        Utilities.alertDialog(this@ConfigurationActivity,getString(R.string.phone_same),getString(R.string.alert),getString(R.string.ok))
                    }else{
                        binding.layoutSp.visibility= View.VISIBLE
                        val anim= AnimationUtils.loadAnimation(this,R.anim.slide_in)
                        binding.layoutSp.startAnimation(anim)
                    }

                }else if (count==6){
                    val v=binding.edPin.text.toString()
                    if (v.length==0){
                        count=5
                        Utilities.alertDialog(this@ConfigurationActivity,getString(R.string.please_pin),getString(R.string.alert),getString(R.string.ok))
                    }else{
                        binding.layoutRsp.visibility= View.VISIBLE
                        val anim= AnimationUtils.loadAnimation(this,R.anim.slide_in)
                        binding.layoutRsp.startAnimation(anim)
                    }
                }else if (count==7){
                    val v=binding.edPin.text.toString()
                    if (!v.equals(binding.edRpin.text.toString())){
                        count=6
                        Utilities.alertDialog(this@ConfigurationActivity,getString(R.string.pin_same),getString(R.string.alert),getString(R.string.ok))
                    }else{
                        binding.layoutFirstFriend.visibility= View.VISIBLE
                        val anim= AnimationUtils.loadAnimation(this,R.anim.slide_in)
                        binding.layoutFirstFriend.startAnimation(anim)
                    }


                }else if (count==8){
                    if (binding.edFirstPn.text.toString().isEmpty()){
                        count=7
                        Utilities.alertDialog(this@ConfigurationActivity,getString(R.string.please_first),getString(R.string.alert),getString(R.string.ok))
                    }else{
                        binding.layoutSecondFriend.visibility= View.VISIBLE
                        val anim= AnimationUtils.loadAnimation(this,R.anim.slide_in)
                        binding.layoutSecondFriend.startAnimation(anim)
                    }
                }else if (count==9){
                    if (binding.edSecondPn.text.toString().isEmpty()){
                        count=8
                        Utilities.alertDialog(this@ConfigurationActivity,getString(R.string.please_second),getString(R.string.alert),getString(R.string.ok))
                    }else{
                        binding.layoutThirdFriend.visibility= View.VISIBLE
                        val anim= AnimationUtils.loadAnimation(this,R.anim.slide_in)
                        binding.layoutThirdFriend.startAnimation(anim)
                    }
                }else if (count==10){
                    binding.layoutFourthFriend.visibility= View.VISIBLE
                    val anim= AnimationUtils.loadAnimation(this,R.anim.slide_in)
                    binding.layoutFourthFriend.startAnimation(anim)
                }
                if (count==11){
                    binding.nextButton.text=getString(R.string.confirm)
                }
                if (binding.nextButton.text.equals(getString(R.string.confirm)) && count>=12){
                    updateUserData()
                }
            }
        }

//        binding.sw.isChecked=sharedPreferences.getBoolean("allow_location",false)

        binding.sw.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                if (!checkLocationPermission()) {
                    requestLocationPermission();
                } else {
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("allow_location", true)
                    editor.apply()
                    // Location permission already granted, proceed with location-based functionality
                }
            } else {
                // If the user turns off the switch, you might want to stop location-based functionality
                // or handle accordingly
                val editor = sharedPreferences.edit()
                editor.putBoolean("allow_location", false)
                editor.apply()
                if (!checkLocationPermission()){
                    Toast.makeText(this, getString(R.string.allow_loc), Toast.LENGTH_SHORT).show();
                }
            }
        })

        binding.circleTextView.setOnLongClickListener {
            if(Constants.freeTrial || Constants.subStatus){
                activated= !activated
                val drawable = binding.circleTextView.background as GradientDrawable
                if (activated){
                    isLongClick = true
                    binding.circleTextView.text=getString(R.string.activated)
                    binding.circleTextView.setTextColor(Color.parseColor("#ffffff"));
                    drawable.setColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
//                    showTestingAlertDialog()
                }else{
                    binding.circleTextView.text=getString(R.string.hold)
                    binding.circleTextView.setTextColor(Color.parseColor("#000000"));
                    drawable.setColor(ContextCompat.getColor(this, android.R.color.white))
                }
            }else{
               notSubscribeDialog()
            }

            true
        }

        binding.circleTextView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    if(Constants.freeTrial || Constants.subStatus){
                        if (isLongClick) {
                            showTestingAlertDialog()
                            isLongClick = false
                        }
                    }
                    true
                }
                else -> false
            }
        }

    }

    private fun initializeSwitch() {
        binding.sw.setChecked(checkLocationPermission())
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun updateUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userUpdates = mutableMapOf<String, Any>()

        if (binding.edUsername.text.isNotEmpty()) {
            userUpdates["name"] = binding.edUsername.text.toString()
        }
        userUpdates["currentUserPhoneNumber"] = binding.edPn.text.toString()
        userUpdates["firstFriendPhoneNumber"] = binding.edFirstPn.text.toString()
        userUpdates["fourthFriendPhoneNumber"] = binding.edFourthPn.text.toString()
        userUpdates["thirdFriendPhoneNumber"] = binding.edThirdPn.text.toString()
        userUpdates["secondFriendPhoneNumber"] = binding.edSecondPn.text.toString()
        userUpdates["pin"] = binding.edPin.text.toString()

        database.child(Constants.url).child(userId).updateChildren(userUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Utilities.getUserDB(auth.currentUser!!,this@ConfigurationActivity,database)
                    configDone=true
                    Toast.makeText(this, getString(R.string.data_update), Toast.LENGTH_SHORT).show()
                    showWelcomeDialog()
                    binding.circleTextView.visibility=View.VISIBLE
                } else {
                    Toast.makeText(this, "Failed to update user data: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUserData2() {
        val userId = auth.currentUser?.uid ?: return
        val userUpdates = mutableMapOf<String, Any>()

        userUpdates["name"] = binding.edUsername.text.toString()
        userUpdates["currentUserPhoneNumber"] = binding.edPn.text.toString()
        userUpdates["firstFriendPhoneNumber"] = binding.edFirstPn.text.toString()
        userUpdates["fourthFriendPhoneNumber"] = binding.edFourthPn.text.toString()
        userUpdates["thirdFriendPhoneNumber"] = binding.edThirdPn.text.toString()
        userUpdates["secondFriendPhoneNumber"] = binding.edSecondPn.text.toString()
        if (binding.edPin.text.isEmpty()){
            userUpdates["pin"] = user.pin
        }else{
            userUpdates["pin"] = binding.edPin.text.toString()
        }


        database.child(Constants.url).child(userId).updateChildren(userUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.data_update), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update user data: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showCustomAlertDialog() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.layout_pin_alert_dialog, null)

        // Create the alert dialog
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        alertDialog.setCancelable(false)

        // Get references to the views in the custom layout
        val etPin: EditText = dialogView.findViewById(R.id.etPin)
        val btnCancel: Button = dialogView.findViewById(R.id.btnCancel)
        val btnOk: Button = dialogView.findViewById(R.id.btnOk)

        // Set up the buttons
        btnCancel.setOnClickListener {
            alertDialog.dismiss()
            finish()
        }

        btnOk.setOnClickListener {
            val pin = etPin.text.toString()
            // Handle the OK button click, for example validate the PIN
            if (validatePin(pin)) {
                // Proceed with the action
                alertDialog.dismiss()
            } else {
                etPin.error = getString(R.string.invalid_pin)
            }
        }

        // Show the dialog
        alertDialog.show()
    }

    private fun showWelcomeDialog() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.layout_welcome_dialog, null)

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
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }

        btnSubscribe.setOnClickListener {
            startActivity(Intent(this,SubscriptionActivity::class.java))
//            alertDialog.dismiss()
        }

        // Show the dialog
        alertDialog.show()
    }

    @SuppressLint("MissingInflatedId")
    private fun showTestingAlertDialog() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.alert_dialog_config, null)

        // Create the alert dialog
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        alertDialog.setCancelable(false)

        // Get references to the views in the custom layout
        val etPin: EditText = dialogView.findViewById(R.id.edtPin)
        val count: TextView = dialogView.findViewById(R.id.count)
        val btnOk: Button = dialogView.findViewById(R.id.btOk)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etPin, InputMethodManager.SHOW_IMPLICIT)

        countDownTimer = object : CountDownTimer(time, 1000) { // 10 seconds in milliseconds
            override fun onTick(millisUntilFinished: Long) {
                // Update the TextView with the remaining time
                count.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                // Timer finished
                count.text = "0"
                binding.circleTextView.text=getString(R.string.hold)
                binding.circleTextView.setTextColor(Color.parseColor("#000000"));
                val drawable = binding.circleTextView.background as GradientDrawable
                drawable.setColor(ContextCompat.getColor(this@ConfigurationActivity, android.R.color.white))
                getLocationAndSendMessage()
                alertDialog.dismiss()
            }
        }
        countDownTimer.start()

        btnOk.setOnClickListener {
            val pin = etPin.text.toString()
            // Handle the OK button click, for example validate the PIN
            if (validatePin(pin)) {
                countDownTimer.cancel()
                binding.circleTextView.text=getString(R.string.hold)
                binding.circleTextView.setTextColor(Color.parseColor("#000000"));
                val drawable = binding.circleTextView.background as GradientDrawable
                drawable.setColor(ContextCompat.getColor(this@ConfigurationActivity, android.R.color.white))
                alertDialogMessage(this@ConfigurationActivity,getString(R.string.msg_cancel))
                alertDialog.dismiss()
            } else {
                etPin.error = getString(R.string.invalid_pin)
            }
        }

        // Show the dialog
        alertDialog.show()
    }

    fun alertDialogMessage(context: Context, msg:String){
        val builder1 = AlertDialog.Builder(context)
        builder1.setTitle(getString(R.string.alert))
        builder1.setMessage(msg)
        builder1.setCancelable(false)

        builder1.setPositiveButton(
            getString(R.string.ok)
        ) { dialog, id ->
            if (fromLogin){
                startActivity(Intent(this@ConfigurationActivity,MainActivity::class.java))
                dialog.cancel()
                finish()
            }else{
                dialog.cancel()
                finish()
            }

        }

        val alert11 = builder1.create()
        alert11.show()
    }

    private fun validatePin(pin: String): Boolean {
        // Replace this with your actual PIN validation logic
        return if (Utilities.getUserData().pin.equals(pin)) true else false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            binding.sw.isChecked = (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onDestroy() {
        super.onDestroy()
//        if (fromLogin){
//            val sharedPref = getSharedPreferences("MySharedPref", MODE_PRIVATE)
//            val b = sharedPref.getBoolean("remember_me",false)
//            if (!b){
//                auth.signOut()
//                googleSignInClient.signOut()
//            }
//        }

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
            isTest = true,
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
                    Toast.makeText(this@ConfigurationActivity, getString(R.string.mess_sent), Toast.LENGTH_SHORT).show()
                } else {
//                    Toast.makeText(this@ConfigurationActivity, "Failed to send message. Response code: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
                finish()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
//                Toast.makeText(this@ConfigurationActivity, "Failed to send message. Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendMessageWithEmptyLocation() {
        sendMessage("")
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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}