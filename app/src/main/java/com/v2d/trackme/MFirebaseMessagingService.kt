package com.v2d.trackme

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import com.crashlytics.android.Crashlytics
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.*
import com.google.android.gms.location.LocationRequest
import com.google.firebase.database.FirebaseDatabase
import com.v2d.trackme.data.MyPreferences
import com.v2d.trackme.utilities.Constants
import java.text.SimpleDateFormat


class MFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String?) {
        super.onNewToken(token)

        Log.d(Constants.TAG, "onNewToken = $token")
        if (token != null) {
            Crashlytics.setString("newToken", token)
            MyPreferences.instance.saveMyToken(token)

            //Save to firebase locationRef
            if(MyPreferences.instance.getMyDeviceName() != null) { //If fresh start
                val database = FirebaseDatabase.getInstance().getReference(Constants.DATABASE_REF)
                database.child(MyPreferences.instance.getMyDeviceName()!!).child(Constants.DB_TOKEN).setValue(token)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Log.d(Constants.TAG, "From: " + remoteMessage!!.from)

        //The toggle is OFF
        if(!MyPreferences.instance.isOnline())
            return

        // Check if message contains a data payload.
        if (remoteMessage.data.size > 0) {
            Log.d(Constants.TAG, "Message data payload: " + remoteMessage.data)
            var payload = remoteMessage.data

            val fromToken = payload[Constants.FROMTOKEN]
            if (fromToken != null) {
                requestLocationUpdate()
            }

        }

    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdate() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest()
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        fusedLocationClient.requestLocationUpdates(locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    val location = locationResult.locations[0]
                    if (location != null) {

                        val myDeviceName = MyPreferences.instance.getMyDeviceName() ?: return

                        val address = getAddress(location)

                        //Save to firebase database
                        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                        val currentDate = sdf.format(Date())

                        var map = HashMap<String, Any?>()
                        map[Constants.DB_ADDRESS] = address
                        map[Constants.DB_LONGITUDE] = location!!.longitude
                        map[Constants.DB_LATITUDE] = location!!.latitude
                        map[Constants.DB_DATE] = currentDate
                        val database = FirebaseDatabase.getInstance().getReference(Constants.DATABASE_REF).child(myDeviceName).child(Constants.DB_LOCATION)
                        database.updateChildren(map)

                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            },
            Looper.getMainLooper())
    }

    private fun getAddress(location: Location): String {
        val geocoder = Geocoder(this, Locale.getDefault())

        var addresses: List<Address> = emptyList()

        try {
            addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    // In this sample, we get just a single address.
                    1)
        } catch (ioException: IOException) {
            // Catch network or other I/O problems.
            Log.e(Constants.TAG, "Service not available", ioException)
        } catch (illegalArgumentException: IllegalArgumentException) {
            // Catch invalid latitude or longitude values.
            Log.e(
                Constants.TAG, "Invalid location. Latitude = $location.latitude , " +
                    "Longitude =  $location.longitude", illegalArgumentException)
        }
        if (addresses.isEmpty()) {
            return "No address found"
        }

        return addresses[0].getAddressLine(0)
    }
}
