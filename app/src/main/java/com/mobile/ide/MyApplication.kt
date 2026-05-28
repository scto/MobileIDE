package com.mobile.ide

import android.app.Application

import com.mobile.ide.application
import com.mobile.ide.core.resources.Res

import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        application = this

        // Initialisierung des statischen Res-Objekts
        Res.application = this
    }
}
