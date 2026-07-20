/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.scto.mobile.ide.utils

import com.scto.mobile.ide.core.common.utils.LogCatcher

import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb // This extension function must be imported
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mobileide_theme_settings")

data class ThemeState(
    val selectedModeIndex: Int,
    val selectedThemeIndex: Int,
    val isMonetEnabled: Boolean,
    val isCustomTheme: Boolean,
    val customColor: Color,
    val style: PaletteStyle = PaletteStyle.TonalSpot,
    val isLoaded: Boolean = false,
)

class ThemeDataStoreRepository(private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_MODE = intPreferencesKey("selected_mode")
        val SELECTED_THEME = intPreferencesKey("selected_theme")
        val IS_MONET_ENABLED = booleanPreferencesKey("is_monet_enabled")
        val IS_CUSTOM = booleanPreferencesKey("is_custom")
        val CUSTOM_COLOR = longPreferencesKey("custom_color")
        val SELECTED_STYLE = stringPreferencesKey("selected_style")
    }

    val themeStateFlow: Flow<ThemeState> =
        context.dataStore.data.map { preferences ->
            val modeIndex = preferences[PreferencesKeys.SELECTED_MODE] ?: 0
            val themeIndex = preferences[PreferencesKeys.SELECTED_THEME] ?: 0
            val isMonet =
                preferences[PreferencesKeys.IS_MONET_ENABLED] ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            val isCustom = preferences[PreferencesKeys.IS_CUSTOM] ?: false

            val styleName = preferences[PreferencesKeys.SELECTED_STYLE] ?: PaletteStyle.TonalSpot.name
            val style =
                try {
                    PaletteStyle.valueOf(styleName)
                } catch (_: IllegalArgumentException) {
                    PaletteStyle.TonalSpot
                }

            // ✅ [Fix 1] Read logic: Convert to Int first, then use Color(Int) constructor
            val customColorValue = preferences[PreferencesKeys.CUSTOM_COLOR] ?: 0xFF6750A4
            val decodedColor = Color(customColorValue.toInt())

            LogCatcher.d(
                "ThemeDebug_Repo",
                "Repo read: Monet=$isMonet, Custom=$isCustom, Style=$style, DecodedColorArg=${decodedColor.toArgb()}",
            )

            ThemeState(
                selectedModeIndex = modeIndex,
                selectedThemeIndex = themeIndex,
                isMonetEnabled = isMonet,
                isCustomTheme = isCustom,
                customColor = decodedColor,
                style = style,
                isLoaded = true,
            )
        }

    suspend fun saveThemeConfig(
        selectedModeIndex: Int,
        selectedThemeIndex: Int,
        customColor: Color,
        isMonetEnabled: Boolean,
        isCustom: Boolean,
        style: PaletteStyle,
    ) {
        // ✅ [Fix 2] Write logic: Use .toArgb() to get the standard Int value
        // This masks the difference of whether Color is Float or Int internally
        val colorInt = customColor.toArgb()

        LogCatcher.w(
            "ThemeDebug_Repo",
            "Repo write: Monet=$isMonetEnabled, Custom=$isCustom, Style=$style, ColorInt=$colorInt",
        )

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_MODE] = selectedModeIndex
            preferences[PreferencesKeys.SELECTED_THEME] = selectedThemeIndex
            preferences[PreferencesKeys.IS_MONET_ENABLED] = isMonetEnabled
            preferences[PreferencesKeys.IS_CUSTOM] = isCustom
            preferences[PreferencesKeys.CUSTOM_COLOR] = colorInt.toLong() // Convert Int to Long and save
            preferences[PreferencesKeys.SELECTED_STYLE] = style.name
        }
    }
}
