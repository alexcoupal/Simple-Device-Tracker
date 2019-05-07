package com.v2d.trackme

import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings.Secure
import android.view.View
import org.json.JSONObject
import android.Manifest
import android.location.LocationManager
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.v2d.trackme.data.MyHistory
import com.v2d.trackme.utilities.InjectorUtils
import com.v2d.trackme.viewmodels.MainViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.crashlytics.android.Crashlytics
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.v2d.trackme.adapters.MyHistoryAdapter
import com.v2d.trackme.data.MyPreferences
import com.v2d.trackme.databinding.ActivityMainBinding
import com.v2d.trackme.dialogs.AlertDialogFragment
import com.v2d.trackme.dialogs.DeviceNameDialogFragment
import com.v2d.trackme.dialogs.ConfirmDialogFragment
import com.v2d.trackme.dialogs.ProgressDialogFragment
import com.v2d.trackme.utilities.Constants


class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSIONS = 1
    enum class Action {
        NONE,
        GET_TOKEN
    }

    private var android_id: String? = null
    private var fcmToken: String? = null

    private var _bAllPermissionGranted: Boolean = false
    private var progressDialog: ProgressDialogFragment? = null

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MyHistoryAdapter
    private var locationRef: DatabaseReference? = null
    private var locationRefListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this // must call this

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        android_id = Secure.getString(contentResolver,  Secure.ANDROID_ID)
        Crashlytics.setString("DeviceUid", android_id)
        firebaseAnalytics.setUserId(android_id)

        //Permissions
        setupPermissions()

        showProgressDialog(false)
        //View model binding
        val factory = InjectorUtils.provideMyHistoryViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)
        binding.viewModel = viewModel

        binding.historyList.layoutManager = LinearLayoutManager(this)
        adapter = MyHistoryAdapter { myHistoryItem : MyHistory -> myHistoryItemClicked(myHistoryItem)}
        binding.historyList.adapter = adapter

        getFCMToken()
    }
    private fun getFCMToken() {
        if(!isConnected(Action.GET_TOKEN))
            return

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(Constants.TAG, "getInstanceId failed", task.exception)
                    showAlert(getString(R.string.no_connection_message), Action.GET_TOKEN)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                fcmToken = task.result?.token
                MyPreferences.instance.saveMyToken(fcmToken!!)
                firebaseAnalytics.setUserProperty("fcmToken", fcmToken)
                Crashlytics.setString("fcmToken", fcmToken)
                subscribeUi()
            })
    }

    private fun saveDeviceName(name : String){
        //Save to firebase locationRef
        firebaseAnalytics.setUserProperty("deviceName", name)
        Crashlytics.setString("deviceName", name)
        val database = FirebaseDatabase.getInstance().getReference(Constants.DATABASE_REF)
        database.child(name).child(Constants.DB_DEVICE_UID).setValue(android_id!!)
        database.child(name).child(Constants.DB_TOKEN).setValue(fcmToken)
        database.child(name).child(Constants.DB_IS_ONLINE).setValue(binding.toggleButton.isChecked)

        //Save to local pref.
        MyPreferences.instance.setMyDeviceName(name)
    }

    private fun subscribeUi() {
        //Toggle
        viewModel.toggleState.observe(this, Observer { onOff ->
            binding.toggleButton.isChecked = onOff
        })
        //History
        viewModel.allHistory.observe(this, Observer { myhistoryList ->
            if (myhistoryList != null)
                adapter.submitList(myhistoryList)
        })

        //Device Name
        val nameObserver = Observer<String> { newName ->
            binding.deviceName.setText(newName)
        }
        viewModel.deviceName.observe(this, nameObserver)

        //My Device Name
        val myDeviceNameObserver = Observer<String> { newName ->
            dismissProgressDialog()
            if (newName == null) { //First launch
                binding.myDeviceName.text = android_id
                saveDeviceName(android_id!!)
            } else {
                binding.myDeviceName.text = newName
                saveDeviceName(newName)
            }
        }
        viewModel.myDeviceName.observe(this, myDeviceNameObserver)

        viewModel.getMyDeviceName(android_id)

        viewModel.address.observe(this, Observer { address ->
            binding.address.text = address
        })

    }
    private fun myHistoryItemClicked(myHistoryItem : MyHistory) {
        viewModel.deviceName.value = myHistoryItem.name
    }
    override fun onResume() {
        super.onResume()

        isGPSEnable()
    }
    private fun setupPermissions() {
        _bAllPermissionGranted = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION),
                    REQUEST_PERMISSIONS)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isEmpty() || contains(grantResults, PackageManager.PERMISSION_DENIED)) {
                    _bAllPermissionGranted = false
                }
                return
            }
        }
    }

    private fun contains(array: IntArray, key: Int): Boolean {
        for (i in array) {
            if (i == key) {
                return true
            }
        }
        return false
    }

    fun track_onClick(@Suppress("UNUSED_PARAMETER")view: View) {
        if(!isConnected(Action.NONE))
            return

        if(_bAllPermissionGranted && !binding.deviceName.text.toString().isEmpty()) {
            binding.deviceName.hideKeyboard()
            showProgressDialog(true)
            findTokenByDeviceName(binding.deviceName.text.toString())
        }
        else if(!_bAllPermissionGranted)
            setupPermissions()
    }
    fun findTokenByDeviceName(deviceName: String)
    {
        val database = FirebaseDatabase.getInstance().getReference(Constants.DATABASE_REF).child(deviceName)
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val isOnline: Boolean = dataSnapshot.child(Constants.DB_IS_ONLINE).value as Boolean
                    if(!isOnline)
                    {
                        dismissProgressDialog()
                        showAlert(getString(R.string.device_name_is_offline), Action.NONE)
                    }

                    val token = dataSnapshot.child(Constants.DB_TOKEN).value.toString()
                    listenToDeviceLocationChange(deviceName)
                    sendRequestToTrackedDevice(token)
                    return
                }

                dismissProgressDialog()
                showAlert(getString(R.string.device_name_not_exist), Action.NONE)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(Constants.TAG, "findTokenByDeviceName:onCancelled", databaseError.toException())
                dismissProgressDialog()
            }
        }

        database.addListenerForSingleValueEvent(postListener)
    }

    private fun listenToDeviceLocationChange(deviceName: String) {
        var i = 0
        if(locationRef == null)
            locationRef = FirebaseDatabase.getInstance().getReference(Constants.DATABASE_REF).child(deviceName).child(Constants.LOCATION)

        locationRefListener = object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                //Hack to avoid getting the current data
                if (i == 0) {
                    i++
                    return
                }

                if (data.exists()) {
                    val address = data.child(Constants.DB_ADDRESS).value as String

                    val date = data.child(Constants.DB_DATE).value as String
                    Log.d(Constants.TAG, "date = $date")

                    viewModel.address.value = address

                    viewModel.addToHistory(deviceName)

                    //Stop listening
                    locationRef?.removeEventListener(this)

                    dismissProgressDialog()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(Constants.TAG, "listenToDeviceLocationChange:onCancelled", databaseError.toException())
                dismissProgressDialog()
            }

        }
        locationRef?.addValueEventListener(locationRefListener!!)
    }

    private fun sendRequestToTrackedDevice(token: String) {
        val dataJson = JSONObject()
        dataJson.put(Constants.TYPE, Constants.TRACKME)
        dataJson.put(Constants.FROMTOKEN, MyPreferences.instance.getMyToken())
        FCMService.instance.send(token, dataJson)
    }

    fun toggle_onClick(view: View) {
        if(!isConnected(Action.NONE)) {
            binding.toggleButton.isChecked = !binding.toggleButton.isChecked
            return
        }

        viewModel.setIsOnline(binding.toggleButton.isChecked)
    }
    fun copy_onClick(@Suppress("UNUSED_PARAMETER")view: View) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("deviceId", android_id)
        clipboard.primaryClip = clip
        Toast.makeText(this, "Copy to clipboard", Toast.LENGTH_SHORT).show()
    }
    fun changeDeviceName_onClick(@Suppress("UNUSED_PARAMETER")view: View) {
        if(!_bAllPermissionGranted) {
            setupPermissions()
            return
        }

        if(!isConnected(Action.NONE))
            return

        val ft = supportFragmentManager.beginTransaction()
        val newFragment = DeviceNameDialogFragment.newInstance(binding.myDeviceName.text.toString(), android_id!!)
        newFragment.show(ft, "loadingDialog")
    }
    fun map_onClick(view: View) {
        if(!_bAllPermissionGranted) {
            setupPermissions()
            return
        }

        if(!isConnected(Action.NONE))
            return
/*
        val intent = Intent(this, MapsActivity::class.java)
        intent.putExtra(MFirebaseMessagingService.LATITUDE, latitude!!)
        intent.putExtra(MFirebaseMessagingService.LONGITUDE, longitude!!)
        startActivity(intent)
*/

        // Create a Uri from an intent string. Use the result to create an Intent.
       // val gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude)
        val gmmIntentUri = Uri.parse("geo:" + 0 + "," + 0 + "?q=" + binding.address.text.toString().replace(" ", "+"))
        // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        // Make the Intent explicit by setting the Google Maps package
        mapIntent.`package` = "com.google.android.apps.maps"

        // Attempt to start an activity that can handle the Intent
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        }
        else
        {
            showAlert(getString(R.string.install_map_app), Action.NONE)
        }

    }
    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun showProgressDialog(cancelable: Boolean = false)
    {
        //If its already showing, then dismiss
        if (progressDialog != null
            && progressDialog?.dialog != null
            && progressDialog?.dialog!!.isShowing
            && !progressDialog?.isRemoving!!)
            progressDialog?.dismiss()

        val ft = supportFragmentManager.beginTransaction()
        progressDialog = ProgressDialogFragment.newInstance(cancelable)
        progressDialog?.setListener(object : ProgressDialogFragment.ProgressDialogFragmentListener {
            override fun onCancelClick() {
                if(locationRef != null && locationRefListener != null)
                    locationRef?.removeEventListener(locationRefListener!!)

                progressDialog?.dismiss()
            }
        })
        progressDialog?.show(ft, "progressDialog")
    }
    private fun dismissProgressDialog() {
        if(progressDialog != null)
            progressDialog?.dismiss()
    }
    fun isConnected(action: Action):Boolean {
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnected == true

        if(!isConnected)
        {
            showAlert(getString(R.string.no_connection_message), action)
        }
        return isConnected
    }

    private fun showAlert(message: String, action: Action) {
        val ft = supportFragmentManager.beginTransaction()
        val fragment = AlertDialogFragment.newInstance(message)
        fragment.isCancelable = false
        fragment.setListener(object: AlertDialogFragment.AlertDialogFragmentListener {
            override fun onOkClick() {
                when(action) {
                    Action.GET_TOKEN -> getFCMToken()
                }
            }

        })
        fragment.show(ft, "alertDialog")

    }
    private fun isGPSEnable(): Boolean{
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
            return false
        }
        return true
    }
    private fun buildAlertMessageNoGps() {
        val ft = supportFragmentManager.beginTransaction()
        val fragment = ConfirmDialogFragment.newInstance(getString(R.string.no_gps_error))
        fragment.setListener(object: ConfirmDialogFragment.ConfirmDialogFragmentListener {
            override fun onNegativeClick() {

            }

            override fun onPositiveClick() {
                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }

        })
        fragment.show(ft, "loadingDialog")
    }
}
