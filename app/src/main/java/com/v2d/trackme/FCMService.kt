package com.v2d.trackme

import android.content.Context
import android.util.Log
import com.v2d.trackme.utilities.Constants
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import kotlin.concurrent.thread

/**
 * Created by acoupal on 3/7/2019.
 */
class FCMService {
    companion object {
        val instance = FCMService()
    }
    fun send(token: String, dataJson: JSONObject){
        thread {
            try {
                val client = OkHttpClient()
                val json = JSONObject()
                json.put("data", dataJson)
                json.put("to", token)
                val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json.toString())
                val request = Request.Builder()
                        .header("Authorization", "key=" + BuildConfig.FCMKey)
                        .url("https://fcm.googleapis.com/fcm/send")
                        .post(body)
                        .build()
                val response = client.newCall(request).execute()
                val finalResponse = response.body()!!.string()
                Log.d(Constants.TAG, "Status Code = " + response.code() + ", Message = " + finalResponse)
            } catch (e: Exception) {
                Log.d("EXCEPTION",e.message)
            }
        }

    }
    fun saveMyToken(context: Context, topic: String)
    {
        val prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, 0)
        val editor = prefs!!.edit()
        editor.putString(Constants.MY_TOKEN, topic)
        editor.apply()
    }
    fun getMyToken(context: Context) : String
    {
        val prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, 0)
        return prefs!!.getString(Constants.MY_TOKEN, null)
    }
}