package com.v2d.trackme.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.v2d.trackme.data.MRepository

/**
 * Created by acoupal on 3/11/2019.
 */
class MainViewModelFactory(private val repository: MRepository) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) = MainViewModel(repository) as T
}