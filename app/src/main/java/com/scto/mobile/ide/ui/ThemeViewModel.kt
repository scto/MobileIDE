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

package com.scto.mobile.ide.ui

import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.materialkolor.PaletteStyle
import com.scto.mobile.ide.core.common.utils.LogCatcher // Import log utility
import com.scto.mobile.ide.utils.ThemeDataStoreRepository
import com.scto.mobile.ide.utils.ThemeState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(private val repository: ThemeDataStoreRepository) : ViewModel() {

    val themeState: StateFlow<ThemeState> =
        repository.themeStateFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue =
                ThemeState(
                    0,
                    0,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    false,
                    Color(0xFF6750A4),
                    PaletteStyle.TonalSpot,
                    false,
                ),
        )

    fun saveThemeConfig(
        selectedModeIndex: Int,
        selectedThemeIndex: Int,
        customColor: Color,
        isMonetEnabled: Boolean,
        isCustom: Boolean,
        style: PaletteStyle = PaletteStyle.TonalSpot,
    ) {
        // [Debug Log] ViewModel reception layer
        LogCatcher.d(
            "ThemeDebug_VM",
            "ViewModel preparing to save: Monet=$isMonetEnabled, Custom=$isCustom, Style=$style, Color=${customColor.value}",
        )

        viewModelScope.launch {
            repository.saveThemeConfig(
                selectedModeIndex,
                selectedThemeIndex,
                customColor,
                isMonetEnabled,
                isCustom,
                style,
            )
        }
    }
}

class ThemeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(ThemeDataStoreRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
