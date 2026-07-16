package com.scto.mobile.ide.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.scto.mobile.ide.core.terminal.settings.Settings

val isDarkTheme: Boolean
    @Composable
    get() = Settings.default_night_mode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES || (Settings.default_night_mode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM && isSystemInDarkTheme())

val amoled: Boolean
    get() = Settings.amoled

@Composable
fun XedTheme(
    darkTheme: Boolean = isDarkTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
