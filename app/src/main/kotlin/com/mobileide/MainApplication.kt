package com.mobileide

import android.app.Application

import com.mobileide.debug.DebugLogger

import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {

    @Inject
    lateinit var debugLogger: DebugLogger

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(debugLogger)
        }
    }
}
