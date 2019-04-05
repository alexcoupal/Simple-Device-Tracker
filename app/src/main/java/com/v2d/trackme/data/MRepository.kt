package com.v2d.trackme.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.v2d.trackme.utilities.Constants
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Created by acoupal on 3/11/2019.
 */
class MRepository private constructor( private val myHistoryDao: MyHistoryDao ) {
    lateinit var firebaseCallback: FirebaseDatabaseRepositoryCallback

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: MRepository? = null

        fun getInstance(myHistoryDao: MyHistoryDao) =
                instance ?: synchronized(this) {
                    instance ?: MRepository(myHistoryDao).also { instance = it }
                }
    }

    fun getAll() = myHistoryDao.getAll()

    suspend fun insert(name: String){
        withContext(IO) {
            if(myHistoryDao.getByName(name) == null)
                myHistoryDao.insert(MyHistory(null, name, Date()))
        }
    }



    fun addListener(android_id: String?, firebaseCallback : FirebaseDatabaseRepositoryCallback) {
        this.firebaseCallback = firebaseCallback

        //Check if exists in firebase database
        val database = FirebaseDatabase.getInstance().getReference(Constants.DATABASE_REF)

        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                if(dataSnapshot.hasChildren()) {
                    for (data: DataSnapshot in dataSnapshot.children) {
                        val deviceUID = data.child(Constants.DB_DEVICE_UID).value as String
                        if (deviceUID.equals(android_id)) {
                            firebaseCallback.onSuccess(data.key.toString())
                            return
                        }
                    }
                }

                firebaseCallback.onSuccess(null)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(Constants.TAG, "loadPost:onCancelled", databaseError.toException())
                firebaseCallback.onError()
            }
        }

        database.addListenerForSingleValueEvent(postListener)
    }

    interface FirebaseDatabaseRepositoryCallback {
        fun onSuccess(result: String?)

        fun onError()
    }
}