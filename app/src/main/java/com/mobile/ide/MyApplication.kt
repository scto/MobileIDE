package com.mobile.ide // Passend zum Paket in deiner App.kt

import android.app.Application
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
