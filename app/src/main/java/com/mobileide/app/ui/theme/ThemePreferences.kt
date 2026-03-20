package com.mobileide.app.ui.theme

import android.content.Context
import androidx.core.content.edit

/**
 * Synchronous SharedPreferences wrapper for the Material3 theme settings.
 *
 * SharedPreferences are used intentionally here instead of DataStore:
 * these values must be readable synchronously during the very first
 * Composition frame, before any coroutine has had a chance to run.
 *
 * Call [ThemePreferences.init] once in Application.onCreate() or at
 * the top of MainActivity.onCreate(), before setContent {}.
 */
object ThemePreferences {

    private const val PREFS_NAME = "mobileide_m3_theme"
    private const val KEY_MONET  = "monet"
    private const val KEY_AMOLED = "amoled"
    private const val KEY_THEME  = "m3_theme_id"

    private lateinit var appContext: Context

    /** Must be called before any composable reads theme state. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Whether Material You / dynamic colour is enabled (requires API 31+). */
    var monet: Boolean
        get() = prefs.getBoolean(KEY_MONET, false)
        set(value) = prefs.edit { putBoolean(KEY_MONET, value) }

    /** Whether AMOLED / pure-black background is enabled. */
    var amoled: Boolean
        get() = prefs.getBoolean(KEY_AMOLED, false)
        set(value) = prefs.edit { putBoolean(KEY_AMOLED, value) }

    /** ID of the currently selected Material3 theme (matches [ThemeHolder.id]). */
    var themeId: String
        get() = prefs.getString(KEY_THEME, "blueberry-default") ?: "blueberry-default"
        set(value) = prefs.edit { putString(KEY_THEME, value) }
}
