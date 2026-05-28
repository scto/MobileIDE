// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.utils

import android.content.Context
import android.content.SharedPreferences

import com.mobile.ide.core.utils.*
import com.mobile.ide.core.resources.R 
import com.mobile.ide.core.resources.Res

/** Utility class for managing welcome page display state */
object WelcomePreferences {
    private const val PREFS_NAME = "welcome_prefs"
    private const val KEY_WELCOME_COMPLETED = "welcome_completed"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Check if the welcome flow is completed */
    fun isWelcomeCompleted(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_WELCOME_COMPLETED, false)
    }

    /** Mark the welcome flow as completed */
    fun setWelcomeCompleted(context: Context) {
        getPreferences(context).edit().putBoolean(KEY_WELCOME_COMPLETED, true).apply()
        LogCatcher.i("WelcomePreferences", context.getString(R.string.welcome_log_completed))
    }

    /** Reset welcome flow state (for testing) */
    fun resetWelcome(context: Context) {
        getPreferences(context).edit().putBoolean(KEY_WELCOME_COMPLETED, false).apply()
        LogCatcher.i("WelcomePreferences", context.getString(R.string.welcome_log_reset))
    }
}
