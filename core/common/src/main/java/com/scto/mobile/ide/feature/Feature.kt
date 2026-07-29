package com.scto.mobile.ide.feature

import android.app.Application

data class FeatureToggle(val nameRes: Int, val key: String, val default: Boolean, val iconRes: Int)

interface Feature {
    val toggle: FeatureToggle? get() = null
    fun init(application: Application)
    fun dispose(application: Application) {}
}

object FeatureRegistry {
    fun isEnabled(id: String): Boolean = true
}
