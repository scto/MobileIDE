package com.mobileide.app.ui.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.MaterialColors

// ─────────────────────────────────────────────────────────────────────────────
// Legacy IDE palette (syntax + chrome colours for old screens)
// Keep intact — still used by EditorSettingsScreen, SettingsScreen, etc.
// ─────────────────────────────────────────────────────────────────────────────

val IDEBackground       = Color(0xFF1E1E2E)
val IDESurface          = Color(0xFF2A2A3E)
val IDESurfaceVariant   = Color(0xFF313145)
val IDEPrimary          = Color(0xFF82AAFF)
val IDESecondary        = Color(0xFFC3E88D)
val IDETertiary         = Color(0xFFFF9CAC)
val IDEOnBackground     = Color(0xFFCDD6F4)
val IDEOnSurface        = Color(0xFFBAC2DE)
val IDEOutline          = Color(0xFF45475A)

val SyntaxKeyword       = Color(0xFFCBA6F7)
val SyntaxString        = Color(0xFFA6E3A1)
val SyntaxComment       = Color(0xFF6C7086)
val SyntaxNumber        = Color(0xFFFAB387)
val SyntaxFunction      = Color(0xFF89B4FA)
val SyntaxAnnotation    = Color(0xFFF38BA8)
val SyntaxType          = Color(0xFF89DCEB)
val SyntaxPlain         = Color(0xFFCDD6F4)

// ─────────────────────────────────────────────────────────────────────────────
// Material3 theme state
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Currently active Material3 [ThemeHolder].
 * Null on first launch — resolved in [MobileIdeTheme] from [ThemePreferences].
 *
 * Change from settings to hot-swap the theme without restart:
 * ```kotlin
 * currentM3Theme.value = newHolder
 * ThemePreferences.themeId = newHolder.id
 * ```
 */
val currentM3Theme = mutableStateOf<ThemeHolder?>(null)

/** Whether Material You dynamic colour is enabled (requires API 31+). */
val dynamicM3Theme = mutableStateOf(false)   // initialised from ThemePreferences in MainActivity

/** Whether AMOLED / pure-black background mode is enabled. */
val amoledM3Theme  = mutableStateOf(false)   // initialised from ThemePreferences in MainActivity

/** CompositionLocal providing the active [ThemeHolder] inside the theme wrapper. */
val LocalThemeHolder = staticCompositionLocalOf<ThemeHolder> {
    error("MobileIdeTheme must wrap this composable")
}

// ─────────────────────────────────────────────────────────────────────────────
// Root Compose theme — replaces the old MobileIDETheme
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Root Material3 theme for MobileIDE.
 *
 * Wrap the top-level composable with this instead of the old [MobileIDETheme].
 *
 * - Supports **Material You** wallpaper colours (API 31+) when [dynamicM3Theme] is true.
 * - Supports **AMOLED** pure-black mode when [amoledM3Theme] is true.
 * - Falls back to **Blueberry** for any missing state.
 * - Exposes [LocalThemeHolder] for downstream access to token/editor colours.
 */
@Composable
fun MobileIdeTheme(
    darkTheme: Boolean    = isSystemInDarkTheme(),
    highContrast: Boolean = amoledM3Theme.value,
    dynamicColor: Boolean = dynamicM3Theme.value,
    content: @Composable () -> Unit,
) {
    var holder = blueberry

    val colorScheme = if (dynamicColor && supportsDynamicTheming()) {
        // ── Material You ──────────────────────────────────────────────────────
        val context = LocalContext.current
        holder = blueberry   // editor / token colours still from Blueberry
        when {
            darkTheme && highContrast ->
                dynamicDarkColorScheme(context)
                    .copy(background = Color.Black, surface = Color.Black, surfaceDim = Color.Black)
            darkTheme  -> dynamicDarkColorScheme(context)
            else       -> dynamicLightColorScheme(context)
        }
    } else {
        // ── Static ThemeHolder ────────────────────────────────────────────────
        if (currentM3Theme.value == null) {
            holder = m3Themes.find { it.id == ThemePreferences.themeId } ?: blueberry
            currentM3Theme.value = holder
        } else {
            holder = currentM3Theme.value ?: blueberry
        }
        val scheme = if (darkTheme) holder.darkScheme else holder.lightScheme
        if (darkTheme && highContrast)
            scheme.copy(background = Color.Black, surface = Color.Black, surfaceDim = Color.Black)
        else
            scheme
    }

    CompositionLocalProvider(LocalThemeHolder provides holder) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = IDETypography,
        ) {
            Surface(color = MaterialTheme.colorScheme.background) {
                content()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Backwards-compatible alias
// Keep old call sites (MainActivity, tests) compiling without changes.
// ─────────────────────────────────────────────────────────────────────────────

@Deprecated(
    "Use MobileIdeTheme instead",
    ReplaceWith("MobileIdeTheme(content = content)", "com.mobileide.app.ui.theme.MobileIdeTheme")
)
@Composable
fun MobileIDETheme(content: @Composable () -> Unit) = MobileIdeTheme(content = content)

// ─────────────────────────────────────────────────────────────────────────────
// Utility
// ─────────────────────────────────────────────────────────────────────────────

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Harmonise [color] (as a 0xAARRGGBB Long) with the current Material3 primary colour
 * so that custom accent colours blend naturally into the active theme.
 */
@Composable
fun harmonize(color: Long): Int {
    val context = LocalContext.current
    return MaterialColors.harmonizeWithPrimary(context, color.toInt())
}

// ─────────────────────────────────────────────────────────────────────────────
// Semantic ColorScheme extensions
// ─────────────────────────────────────────────────────────────────────────────

// ── Warning ──────────────────────────────────────────────────────────────────
val ColorScheme.warningSurface: Color
    @Composable get() = if (isSystemInDarkTheme())
        Color(harmonize(0xFF633F00)) else Color(harmonize(0xFFFFDDB4))

val ColorScheme.onWarningSurface: Color
    @Composable get() = if (isSystemInDarkTheme())
        Color(harmonize(0xFFFFDDB4)) else Color(harmonize(0xFF633F00))

// ── File tree ─────────────────────────────────────────────────────────────────
val ColorScheme.folderSurface: Color
    @Composable get() = if (isSystemInDarkTheme())
        Color(harmonize(0xFFFFC857)) else Color(harmonize(0xFFFAB72D))

// ── Git status ───────────────────────────────────────────────────────────────
val ColorScheme.gitAdded: Color
    @Composable get() = if (isSystemInDarkTheme())
        Color(harmonize(0xFF81C784)) else Color(harmonize(0xFF2E7D32))

val ColorScheme.gitModified: Color
    @Composable get() = if (isSystemInDarkTheme())
        Color(harmonize(0xFF64B5F6)) else Color(harmonize(0xFF1565C0))

val ColorScheme.gitDeleted: Color
    get() = onSurface.copy(alpha = 0.6f)

val ColorScheme.gitConflicted: Color
    @Composable get() = if (isSystemInDarkTheme())
        Color(harmonize(0xFFE57373)) else Color(harmonize(0xFFC62828))
