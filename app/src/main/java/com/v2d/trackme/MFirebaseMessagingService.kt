package com.v2d.trackme

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.*
import com.google.android.gms.location.LocationRequest
import com.v2d.trackme.utilities.Constants


class MFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String?) {
        super.onNewToken(token)

        if (token != null) {
            FCMService.instance.saveMyToken(this, token)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Log.d(Constants.TAG, "From: " + remoteMessage!!.from)

        // Check if message contains a data payload.
        if (remoteMessage.data.size > 0) {
            Log.d(Constants.TAG, "Message data payload: " + remoteMessage.data)
            var payload = remoteMessage.data
            var type = payload.get(Constants.TYPE)

            if(type.equals(Constants.TRACKME))
            {
                val fromToken = payload[Constants.FROMTOKEN]
                if (fromToken != null) {
                    requestLocationUpdate(fromToken)
                }
            }
            else {
                val longitude = payload.get(Constants.LONGITUDE)!!.toDouble()
                val latitude = payload.get(Constants.LATITUDE)!!.toDouble()
                val address = payload.get(Constants.ADDRESS)
                val fromDeviceName = payload.get(Constants.FROM_DEVICE_NAME)

                val intent = Intent("android.intent.action.MAIN")
                intent.putExtra(Constants.LONGITUDE, longitude)
                intent.putExtra(Constants.LATITUDE, latitude)
                intent.putExtra(Constants.ADDRESS, address)
                intent.putExtra(Constants.FROM_DEVICE_NAME, fromDeviceName)
                sendBroadcast(intent)
            }
        }

    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdate(fromToken: String) {
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

                        val prefs = getSharedPreferences(Constants.PREFS_FILENAME, 0)
                        val myDeviceName = prefs!!.getString(Constants.MY_DEVICE_NAME, null) ?: return

                        val address = getAddress(location)
                        val dataJson = JSONObject()
                        dataJson.put(Constants.TYPE, Constants.LOCATION)
                        dataJson.put(Constants.LONGITUDE, location!!.getLongitude())
                        dataJson.put(Constants.LATITUDE, location!!.getLatitude())
                        dataJson.put(Constants.ADDRESS, address)
                        dataJson.put(Constants.FROM_DEVICE_NAME, myDeviceName)
                        FCMService.instance.send(fromToken, dataJson)

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
