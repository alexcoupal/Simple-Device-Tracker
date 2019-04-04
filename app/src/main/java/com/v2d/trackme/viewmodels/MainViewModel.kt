package com.v2d.trackme.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2d.trackme.utilities.Constants
import com.v2d.trackme.data.MyHistory
import com.v2d.trackme.data.MRepository
import kotlinx.coroutines.launch

/**
 * Created by acoupal on 3/11/2019.
 */
class MainViewModel(private val repository: MRepository) : ViewModel() {

    val deviceName : MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    var myDeviceName : MutableLiveData<String> = MutableLiveData<String>()

    fun getMyDeviceName(context: Context, android_id: String?) {
        val prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, 0)
        val deviceName = prefs!!.getString(Constants.MY_DEVICE_NAME, null)
        if(deviceName == null) {
            loadMyDeviceName(android_id)
        }
        else
            myDeviceName.value = deviceName
    }

    private fun loadMyDeviceName(android_id: String?) {
        repository.addListener(android_id, object: MRepository.FirebaseDatabaseRepositoryCallback {
            override fun onSuccess(result: String?) {
                myDeviceName.value = result
            }

            override fun onError() {
                myDeviceName.value = ""
            }

        })
    }

    var allHistory : LiveData<List<MyHistory>> = repository.getAll()

    fun addToHistory(name: String) {
        viewModelScope.launch {
            repository.insert(name)
        }
        allHistory = repository.getAll()
    }
}