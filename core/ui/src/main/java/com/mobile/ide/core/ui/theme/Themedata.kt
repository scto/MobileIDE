// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.ui.theme

import androidx.compose.ui.graphics.Color

data class ThemeColor(
    val name: String,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val primaryColor: Color,
    val accentColor: Color,
)

val themeColors =
    listOf(
        ThemeColor("Catppuccin", Color(0xFF1E1E2E), Color(0xFF313244), Color(0xFFCBA6F7), Color(0xFFF5C2E7)),
        ThemeColor("Apple", Color(0xFF1C1C1E), Color(0xFF2C2C2E), Color(0xFF32D74B), Color(0xFF007AFF)),
        ThemeColor("Lavender", Color(0xFF1A1626), Color(0xFF49454F), Color(0xFFB8ADFF), Color(0xFFCBC0FF)),
        ThemeColor("Midnight", Color(0xFF0D1117), Color(0xFF161B22), Color(0xFF58A6FF), Color(0xFF79C0FF)),
        ThemeColor("Nord", Color(0xFF2E3440), Color(0xFF3B4252), Color(0xFF88C0D0), Color(0xFF81A1C1)),
        ThemeColor("Strawberry", Color(0xFF201418), Color(0xFF524347), Color(0xFFFF8FB4), Color(0xFFFFB1C8)),
        ThemeColor("Tako", Color(0xFF1A1625), Color(0xFF2D2640), Color(0xFF9D7CD8), Color(0xFFB79FE8)),
    )

enum class WelcomeStep {
    INTRO,
    PERMISSIONS,
    THEME_SETUP,
}
