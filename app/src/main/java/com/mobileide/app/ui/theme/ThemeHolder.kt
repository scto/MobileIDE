package com.mobileide.app.ui.theme

import androidx.compose.material3.ColorScheme
import com.google.gson.JsonArray
import java.util.Properties

/**
 * Holds the fully resolved data for one Material3 theme variant.
 *
 * - [lightScheme] / [darkScheme] drive the Compose [MaterialTheme].
 * - [lightEditorColors] / [darkEditorColors] override Sora editor chrome colours.
 * - [lightTokenColors] / [darkTokenColors] override TextMate token colours.
 * - Terminal colour maps are kept for future terminal-emulator support.
 */
data class ThemeHolder(
    /** Stable unique identifier, e.g. "blueberry-default". */
    val id: String,
    /** Human-readable display name shown in the theme picker. */
    val name: String,
    /**
     * When true, unspecified colour slots in a user-installed theme inherit from
     * Blueberry (the default) instead of the Material3 system defaults.
     */
    val inheritBase: Boolean,
    /** Material3 ColorScheme for light mode. */
    val lightScheme: ColorScheme,
    /** Material3 ColorScheme for dark mode. */
    val darkScheme: ColorScheme,
    /** ANSI terminal colour map for light mode (foreground/background/color0–color21). */
    val lightTerminalColors: Properties,
    /** ANSI terminal colour map for dark mode. */
    val darkTerminalColors: Properties,
    /** Sora EditorColorScheme overrides for light mode (applied after TextMate build). */
    val lightEditorColors: List<EditorColor>,
    /** Sora EditorColorScheme overrides for dark mode. */
    val darkEditorColors: List<EditorColor>,
    /** TextMate token-colour overrides for light mode. */
    val lightTokenColors: JsonArray,
    /** TextMate token-colour overrides for dark mode. */
    val darkTokenColors: JsonArray,
)
