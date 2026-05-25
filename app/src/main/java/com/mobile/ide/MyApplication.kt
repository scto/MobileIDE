package com.mobile.ide // Passend zum Paket in deiner App.kt

import android.app.Application

import dagger.hilt.android.HiltAndroidApp

import com.mobile.ide.core.resources.Res

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        application = this
        // Initialisierung des statischen Res-Objekts
        Res.application = this
    }
}