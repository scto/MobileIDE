package com.mobileide.app.ui.theme

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serial
import java.io.Serializable

/**
 * All 36 Material3 colour slots a theme JSON file may override.
 * Any null slot falls back to the Blueberry base theme when [ThemeConfig.inheritBase] is true.
 */
data class BaseColors(
    val primary: String? = null,
    val onPrimary: String? = null,
    val primaryContainer: String? = null,
    val onPrimaryContainer: String? = null,
    val secondary: String? = null,
    val onSecondary: String? = null,
    val secondaryContainer: String? = null,
    val onSecondaryContainer: String? = null,
    val tertiary: String? = null,
    val onTertiary: String? = null,
    val tertiaryContainer: String? = null,
    val onTertiaryContainer: String? = null,
    val error: String? = null,
    val onError: String? = null,
    val errorContainer: String? = null,
    val onErrorContainer: String? = null,
    val background: String? = null,
    val onBackground: String? = null,
    val surface: String? = null,
    val onSurface: String? = null,
    val surfaceVariant: String? = null,
    val onSurfaceVariant: String? = null,
    val outline: String? = null,
    val outlineVariant: String? = null,
    val scrim: String? = null,
    val inverseSurface: String? = null,
    val inverseOnSurface: String? = null,
    val inversePrimary: String? = null,
    val surfaceTint: String? = null,
    val surfaceDim: String? = null,
    val surfaceBright: String? = null,
    val surfaceContainerLowest: String? = null,
    val surfaceContainerLow: String? = null,
    val surfaceContainer: String? = null,
    val surfaceContainerHigh: String? = null,
    val surfaceContainerHighest: String? = null,
) : Serializable

/**
 * One light or dark palette block inside a [ThemeConfig].
 *
 * [tokenColors] supports two JSON formats:
 *
 * **Option 1 – simple map**
 * ```json
 * { "tokenColors": { "comment": "#6C7086", "keyword": "#CBA6F7" } }
 * ```
 *
 * **Option 2 – TextMate array**
 * ```json
 * { "tokenColors": [{ "scope": "comment", "settings": { "foreground": "#6C7086" } }] }
 * ```
 */
data class ThemePalette(
    val baseColors: BaseColors?,
    val terminalColors: Map<String, String>? = null,
    val editorColors: Map<String, String>? = null,
    @Transient var tokenColors: JsonElement? = null,
) : Serializable {

    @Serial
    private fun writeObject(out: ObjectOutputStream) {
        out.defaultWriteObject()
        out.writeObject(tokenColors?.toString())
    }

    @Serial
    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
        val str = input.readObject() as? String
        tokenColors = str?.let { JsonParser.parseString(it) }
    }
}

/**
 * Root model deserialised from a MobileIDE `.json` theme file.
 * Install via [com.mobileide.app.ui.theme.ThemeLoader.installTheme].
 *
 * Minimal example:
 * ```json
 * {
 *   "id": "my-theme",
 *   "name": "My Theme",
 *   "targetVersion": 1,
 *   "inheritBase": true,
 *   "dark": {
 *     "baseColors": { "primary": "#89B4FA", "background": "#1E1E2E" }
 *   }
 * }
 * ```
 */
data class ThemeConfig(
    val id: String?,
    val name: String?,
    /** App versionCode this theme targets — shown as a warning when mismatched. */
    val targetVersion: Int?,
    val inheritBase: Boolean?,
    val light: ThemePalette?,
    val dark: ThemePalette?,
) : Serializable
