package com.scto.mobile.ide.core.terminal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.mutableStateOf

object SharedThemeState {
    val currentColorScheme = mutableStateOf<ColorScheme?>(null)
    val isDark = mutableStateOf(false)
}
