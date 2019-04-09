package com.v2d.trackme.data

import android.content.SharedPreferences
import com.v2d.trackme.application.MyApplication
import com.v2d.trackme.utilities.Constants

class MyPreferences {
    companion object {
        val instance = MyPreferences()
        val myPref: SharedPreferences = MyApplication.context.getSharedPreferences(Constants.PREFS_FILENAME, 0)
        val editor = myPref.edit()
    }
    fun setCanAccessMyLocation(value: Boolean){
        editor.putBoolean(Constants.CAN_ACCESS_MYLOCATION, value)
        editor.apply()
    }
    fun getCanAccessMyLocation(): Boolean{
        return myPref.getBoolean(Constants.CAN_ACCESS_MYLOCATION, true)
    }
    fun getMyDeviceName(): String? {
        return myPref.getString(Constants.MY_DEVICE_NAME, null)
    }
    fun setMyDeviceName(name: String){
        editor.putString(Constants.MY_DEVICE_NAME, name)
        editor.apply()
    }
    fun saveMyToken(token: String)
    {
        editor.putString(Constants.MY_TOKEN, token)
        editor.apply()
    }
    fun getMyToken() : String
    {
        return myPref.getString(Constants.MY_TOKEN, null)
    }
}