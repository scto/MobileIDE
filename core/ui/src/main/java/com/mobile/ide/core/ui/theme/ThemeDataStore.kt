// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.ui.theme

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb 
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mobileide_theme_settings")

data class ThemeState(
    val selectedModeIndex: Int,
    val selectedThemeIndex: Int,
    val isMonetEnabled: Boolean,
    val isCustomTheme: Boolean,
    val customColor: Color,
    val isLoaded: Boolean = false
)

class ThemeDataStoreRepository(private val context: Context) {
    private object PreferencesKeys {
        val SELECTED_MODE = intPreferencesKey("selected_mode")
        val SELECTED_THEME = intPreferencesKey("selected_theme")
        val IS_MONET_ENABLED = booleanPreferencesKey("is_monet_enabled")
        val IS_CUSTOM = booleanPreferencesKey("is_custom")
        val CUSTOM_COLOR = longPreferencesKey("custom_color")
    }

    val themeStateFlow: Flow<ThemeState> = context.dataStore.data
        .map { preferences ->
            val modeIndex = preferences[PreferencesKeys.SELECTED_MODE] ?: 0
            val themeIndex = preferences[PreferencesKeys.SELECTED_THEME] ?: 0
            val isMonet = preferences[PreferencesKeys.IS_MONET_ENABLED] ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            val isCustom = preferences[PreferencesKeys.IS_CUSTOM] ?: false

            val customColorValue = preferences[PreferencesKeys.CUSTOM_COLOR] ?: 0xFF6750A4
            val decodedColor = Color(customColorValue.toInt())

            LogCatcher.d("ThemeDebug_Repo", "Repo read: Monet=$isMonet, Custom=$isCustom, DecodedColorArg=${decodedColor.toArgb()}")

            ThemeState(
                selectedModeIndex = modeIndex,
                selectedThemeIndex = themeIndex,
                isMonetEnabled = isMonet,
                isCustomTheme = isCustom,
                customColor = decodedColor,
                isLoaded = true
            )
        }

    suspend fun saveThemeConfig(
        selectedModeIndex: Int,
        selectedThemeIndex: Int,
        customColor: Color,
        isMonetEnabled: Boolean,
        isCustom: Boolean
    ) {
        val colorInt = customColor.toArgb()

        LogCatcher.w("ThemeDebug_Repo", "Repo write: Monet=$isMonetEnabled, Custom=$isCustom, ColorInt=$colorInt")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_MODE] = selectedModeIndex
            preferences[PreferencesKeys.SELECTED_THEME] = selectedThemeIndex
            preferences[PreferencesKeys.IS_MONET_ENABLED] = isMonetEnabled
            preferences[PreferencesKeys.IS_CUSTOM] = isCustom
            preferences[PreferencesKeys.CUSTOM_COLOR] = colorInt.toLong() 
        }
    }
}