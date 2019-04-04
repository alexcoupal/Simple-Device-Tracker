package com.v2d.trackme.utilities

import android.content.Context
import com.v2d.trackme.data.AppDatabase
import com.v2d.trackme.data.MRepository
import com.v2d.trackme.viewmodels.MainViewModelFactory

/**
 * Created by acoupal on 3/11/2019.
 */
object InjectorUtils {

    private fun getMyHistoryRepository(context: Context): MRepository {
        return MRepository.getInstance(
                AppDatabase.getInstance(context.applicationContext).myHistoryDao())
    }

    fun provideMyHistoryViewModelFactory(context: Context): MainViewModelFactory {
        val repository = getMyHistoryRepository(context)
        return MainViewModelFactory(repository)
    }
}