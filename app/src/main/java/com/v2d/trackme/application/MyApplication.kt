package com.v2d.trackme.application

import android.app.Application
import android.content.Context

class MyApplication : Application() {
    companion object {
        internal lateinit var context : Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}