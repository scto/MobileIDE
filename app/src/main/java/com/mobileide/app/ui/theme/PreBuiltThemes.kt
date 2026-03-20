package com.mobileide.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.google.gson.JsonArray
import java.util.Properties

// ─────────────────────────────────────────────────────────────────────────────
// Utility
// ─────────────────────────────────────────────────────────────────────────────

/** Convert a Compose [Color] to an #RRGGBB hex string (no alpha). */
fun Color.toHex(): String {
    val argb = this.value.toLong().toInt()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return String.format("#%02X%02X%02X", r, g, b)
}

// ─────────────────────────────────────────────────────────────────────────────
// Blueberry — default / fallback theme
// ─────────────────────────────────────────────────────────────────────────────

/**
 * **Blueberry** — the default MobileIDE Material3 theme.
 *
 * All user-installed themes that set [ThemeHolder.inheritBase] = true will fall
 * back to these colours for any slot they leave undefined.
 */
val blueberry = ThemeHolder(
    id          = "blueberry-default",
    name        = "Blueberry (Default)",
    inheritBase = true,
    lightScheme = lightColorScheme(
        primary              = Color(0xFF445E91),
        onPrimary            = Color(0xFFFFFFFF),
        primaryContainer     = Color(0xFFD8E2FF),
        onPrimaryContainer   = Color(0xFF001A41),
        secondary            = Color(0xFF575E71),
        onSecondary          = Color(0xFFFFFFFF),
        secondaryContainer   = Color(0xFFDBE2F9),
        onSecondaryContainer = Color(0xFF141B2C),
        tertiary             = Color(0xFF715573),
        onTertiary           = Color(0xFFFFFFFF),
        tertiaryContainer    = Color(0xFFFBD7FC),
        onTertiaryContainer  = Color(0xFF29132D),
        error                = Color(0xFFBA1A1A),
        errorContainer       = Color(0xFFFFDAD6),
        onError              = Color(0xFFFFFFFF),
        onErrorContainer     = Color(0xFF410002),
        background           = Color(0xFFF9F9FF),
        onBackground         = Color(0xFF1A1B20),
        surface              = Color(0xFFF9F9FF),
        onSurface            = Color(0xFF1A1B20),
        surfaceVariant       = Color(0xFFE1E2EC),
        onSurfaceVariant     = Color(0xFF44474F),
        outline              = Color(0xFF74777F),
        inverseOnSurface     = Color(0xFFF0F0F7),
        inverseSurface       = Color(0xFF2F3036),
        inversePrimary       = Color(0xFFADC6FF),
        surfaceTint          = Color(0xFF445E91),
        outlineVariant       = Color(0xFFC4C6D0),
        scrim                = Color(0xFF000000),
    ),
    darkScheme = darkColorScheme(
        primary              = Color(0xFFADC6FF),
        onPrimary            = Color(0xFF102F60),
        primaryContainer     = Color(0xFF2B4678),
        onPrimaryContainer   = Color(0xFFD8E2FF),
        secondary            = Color(0xFFBFC6DC),
        onSecondary          = Color(0xFF293041),
        secondaryContainer   = Color(0xFF3F4759),
        onSecondaryContainer = Color(0xFFDBE2F9),
        tertiary             = Color(0xFFDEBCDF),
        onTertiary           = Color(0xFF402843),
        tertiaryContainer    = Color(0xFF583E5B),
        onTertiaryContainer  = Color(0xFFFBD7FC),
        error                = Color(0xFFFFB4AB),
        errorContainer       = Color(0xFF93000A),
        onError              = Color(0xFF690005),
        onErrorContainer     = Color(0xFFFFDAD6),
        background           = Color(0xFF111318),
        onBackground         = Color(0xFFE2E2E9),
        surface              = Color(0xFF111318),
        onSurface            = Color(0xFFE2E2E9),
        surfaceVariant       = Color(0xFF44474F),
        onSurfaceVariant     = Color(0xFFC4C6D0),
        outline              = Color(0xFF8E9099),
        inverseOnSurface     = Color(0xFF2F3036),
        inverseSurface       = Color(0xFFE2E2E9),
        inversePrimary       = Color(0xFF445E91),
        surfaceTint          = Color(0xFFD8E2FF),
        outlineVariant       = Color(0xFF44474F),
        scrim                = Color(0xFF000000),
    ),
    lightTokenColors  = JsonArray(),
    darkTokenColors   = JsonArray(),
    lightEditorColors = emptyList(),
    darkEditorColors  = emptyList(),
    lightTerminalColors = Properties().apply {
        set("foreground", Color(0xFF1A1B20).toHex())
        set("background", Color(0xFFF9F9FF).toHex())
        set("cursor",  "#373b41")
        set("color0",  "#1d1f21"); set("color8",  "#969896")
        set("color1",  "#CC342B"); set("color9",  "#CC342B")
        set("color2",  "#198844"); set("color10", "#198844")
        set("color3",  "#FBA922"); set("color11", "#FBA922")
        set("color4",  "#3971ED"); set("color12", "#3971ED")
        set("color5",  "#A36AC7"); set("color13", "#A36AC7")
        set("color6",  "#3971ED"); set("color14", "#3971ED")
        set("color7",  "#c5c8c6"); set("color15", "#ffffff")
        set("color16", "#F96A38"); set("color17", "#3971ED")
        set("color18", "#282a2e"); set("color19", "#373b41")
        set("color20", "#b4b7b4"); set("color21", "#e0e0e0")
    },
    darkTerminalColors = Properties().apply {
        set("background", Color(0xFF111318).toHex())
        set("foreground", Color(0xFFE2E2E9).toHex())
        set("cursor",  "#6e6a86")
        set("color0",  "#393552"); set("color8",  "#6e6a86")
        set("color1",  "#eb6f92"); set("color9",  "#eb6f92")
        set("color2",  "#3e8fb0"); set("color10", "#3e8fb0")
        set("color3",  "#f6c177"); set("color11", "#f6c177")
        set("color4",  "#9ccfd8"); set("color12", "#9ccfd8")
        set("color5",  "#c4a7e7"); set("color13", "#c4a7e7")
        set("color6",  "#ea9a97"); set("color14", "#ea9a97")
        set("color7",  "#e0def4"); set("color15", "#e0def4")
    },
)

// ─────────────────────────────────────────────────────────────────────────────
// Lime — green-accented built-in theme
// ─────────────────────────────────────────────────────────────────────────────

val lime = ThemeHolder(
    id          = "lime",
    name        = "Lime",
    inheritBase = true,
    lightScheme = lightColorScheme(
        primary              = Color(0xFF4C662B),
        onPrimary            = Color(0xFFFFFFFF),
        primaryContainer     = Color(0xFFCDEDA3),
        onPrimaryContainer   = Color(0xFF354E16),
        secondary            = Color(0xFF586249),
        onSecondary          = Color(0xFFFFFFFF),
        secondaryContainer   = Color(0xFFDCE7C8),
        onSecondaryContainer = Color(0xFF404A33),
        tertiary             = Color(0xFF386663),
        onTertiary           = Color(0xFFFFFFFF),
        tertiaryContainer    = Color(0xFFBCECE7),
        onTertiaryContainer  = Color(0xFF1F4E4B),
        error                = Color(0xFFBA1A1A),
        onError              = Color(0xFFFFFFFF),
        errorContainer       = Color(0xFFFFDAD6),
        onErrorContainer     = Color(0xFF93000A),
        background           = Color(0xFFF9FAEF),
        onBackground         = Color(0xFF1A1C16),
        surface              = Color(0xFFF9FAEF),
        onSurface            = Color(0xFF1A1C16),
        surfaceVariant       = Color(0xFFE1E4D5),
        onSurfaceVariant     = Color(0xFF44483D),
        outline              = Color(0xFF75796C),
        outlineVariant       = Color(0xFFC5C8BA),
        scrim                = Color(0xFF000000),
        inverseSurface       = Color(0xFF2F312A),
        inverseOnSurface     = Color(0xFFF1F2E6),
        inversePrimary       = Color(0xFFB1D18A),
        surfaceTint          = Color(0xFF4C662B),
        surfaceDim           = Color(0xFFDADBD0),
        surfaceBright        = Color(0xFFF9FAEF),
        surfaceContainerLowest  = Color(0xFFFFFFFF),
        surfaceContainerLow     = Color(0xFFF3F4E9),
        surfaceContainer        = Color(0xFFEEEFE3),
        surfaceContainerHigh    = Color(0xFFE8E9DE),
        surfaceContainerHighest = Color(0xFFE2E3D8),
    ),
    darkScheme = darkColorScheme(
        primary              = Color(0xFFB1D18A),
        onPrimary            = Color(0xFF1F3701),
        primaryContainer     = Color(0xFF354E16),
        onPrimaryContainer   = Color(0xFFCDEDA3),
        secondary            = Color(0xFFBFCBAD),
        onSecondary          = Color(0xFF2A331E),
        secondaryContainer   = Color(0xFF404A33),
        onSecondaryContainer = Color(0xFFDCE7C8),
        tertiary             = Color(0xFFA0D0CB),
        onTertiary           = Color(0xFF003735),
        tertiaryContainer    = Color(0xFF1F4E4B),
        onTertiaryContainer  = Color(0xFFBCECE7),
        error                = Color(0xFFFFB4AB),
        onError              = Color(0xFF690005),
        errorContainer       = Color(0xFF93000A),
        onErrorContainer     = Color(0xFFFFDAD6),
        background           = Color(0xFF12140E),
        onBackground         = Color(0xFFE2E3D8),
        surface              = Color(0xFF12140E),
        onSurface            = Color(0xFFE2E3D8),
        surfaceVariant       = Color(0xFF44483D),
        onSurfaceVariant     = Color(0xFFC5C8BA),
        outline              = Color(0xFF8F9285),
        outlineVariant       = Color(0xFF44483D),
        scrim                = Color(0xFF000000),
        inverseSurface       = Color(0xFFE2E3D8),
        inverseOnSurface     = Color(0xFF2F312A),
        inversePrimary       = Color(0xFF4C662B),
        surfaceTint          = Color(0xFFB1D18A),
        surfaceDim           = Color(0xFF12140E),
        surfaceBright        = Color(0xFF383A32),
        surfaceContainerLowest  = Color(0xFF0C0F09),
        surfaceContainerLow     = Color(0xFF1A1C16),
        surfaceContainer        = Color(0xFF1E201A),
        surfaceContainerHigh    = Color(0xFF282B24),
        surfaceContainerHighest = Color(0xFF33362E),
    ),
    lightTokenColors  = JsonArray(),
    darkTokenColors   = JsonArray(),
    lightEditorColors = emptyList(),
    darkEditorColors  = emptyList(),
    lightTerminalColors = Properties().apply {
        set("foreground", Color(0xFF1A1C16).toHex())
        set("background", Color(0xFFF9FAEF).toHex())
        set("cursor",  "#373b41")
        set("color0",  "#1d1f21"); set("color8",  "#969896")
        set("color1",  "#CC342B"); set("color9",  "#CC342B")
        set("color2",  "#198844"); set("color10", "#198844")
        set("color3",  "#FBA922"); set("color11", "#FBA922")
        set("color4",  "#3971ED"); set("color12", "#3971ED")
        set("color5",  "#A36AC7"); set("color13", "#A36AC7")
        set("color6",  "#3971ED"); set("color14", "#3971ED")
        set("color7",  "#c5c8c6"); set("color15", "#ffffff")
        set("color16", "#F96A38"); set("color17", "#3971ED")
        set("color18", "#282a2e"); set("color19", "#373b41")
        set("color20", "#b4b7b4"); set("color21", "#e0e0e0")
    },
    darkTerminalColors = Properties().apply {
        set("background", Color(0xFF12140E).toHex())
        set("foreground", Color(0xFFE2E3D8).toHex())
        set("cursor",  "#6e6a86")
        set("color0",  "#393552"); set("color8",  "#6e6a86")
        set("color1",  "#eb6f92"); set("color9",  "#eb6f92")
        set("color2",  "#3e8fb0"); set("color10", "#3e8fb0")
        set("color3",  "#f6c177"); set("color11", "#f6c177")
        set("color4",  "#9ccfd8"); set("color12", "#9ccfd8")
        set("color5",  "#c4a7e7"); set("color13", "#c4a7e7")
        set("color6",  "#ea9a97"); set("color14", "#ea9a97")
        set("color7",  "#e0def4"); set("color15", "#e0def4")
    },
)

/** All Material3 themes that ship with MobileIDE. User-installed themes are appended at runtime. */
val inbuiltM3Themes: List<ThemeHolder> = listOf(blueberry, lime)
