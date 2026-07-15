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

package com.scto.mobile.ide.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.LaunchedEffect
import com.materialkolor.rememberDynamicColorScheme
import com.scto.mobile.ide.core.utils.LogCatcher
import com.scto.mobile.ide.core.utils.ThemeState

// ============================================================================
// 1. 预设主题 (保留原样)
// ============================================================================
// ============================================================================
// 1. 预设主题 (已修复 Scrim 缺失与卡片对比度问题)
// ============================================================================

// 默认 Material 深色 (保留官方微调)
private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
        background = Color(0xFF141218), // 稍微压暗背景，突出卡片
        onBackground = Color(0xFFE6E1E5),
        surface = Color(0xFF141218),
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        scrim = Color(0xFF000000), // ✅ 已补全
        inverseSurface = Color(0xFFE6E1E5),
        inverseOnSurface = Color(0xFF313033),
        inversePrimary = Color(0xFF6750A4),
        surfaceDim = Color(0xFF141318),
        surfaceBright = Color(0xFF3B383E),
        surfaceContainerLowest = Color(0xFF0F0D13),
        surfaceContainerLow = Color(0xFF1D1B20), // 卡片颜色
        surfaceContainer = Color(0xFF211F26),
        surfaceContainerHigh = Color(0xFF2B2930),
        surfaceContainerHighest = Color(0xFF36343B),
    )

// 默认 Material 浅色
private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF625B71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        tertiary = Color(0xFF7D5260),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8E4),
        onTertiaryContainer = Color(0xFF31111D),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        background = Color(0xFFFDF8FD), // 极淡的灰紫色背景
        onBackground = Color(0xFF1C1B1F),
        surface = Color(0xFFFDF8FD),
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        scrim = Color(0xFF000000), // ✅ 已补全
        inverseSurface = Color(0xFF313033),
        inverseOnSurface = Color(0xFFF4EFF4),
        inversePrimary = Color(0xFFD0BCFF),
        surfaceDim = Color(0xFFDED8E1),
        surfaceBright = Color(0xFFFFFBFE),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F2FA), // 卡片颜色
        surfaceContainer = Color(0xFFF3EDF7),
        surfaceContainerHigh = Color(0xFFECE6F0),
        surfaceContainerHighest = Color(0xFFE6E0E9),
    )

// Catppuccin Mocha 深色 (修复背景与卡片撞色)
private val CatppuccinDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFD9B9FF),
        onPrimary = Color(0xFF411B6D),
        primaryContainer = Color(0xFF593485),
        onPrimaryContainer = Color(0xFFEEDBFF),
        secondary = Color(0xFFCFC1DA),
        onSecondary = Color(0xFF362D40),
        secondaryContainer = Color(0xFF4D4357),
        onSecondaryContainer = Color(0xFFECDDF7),
        tertiary = Color(0xFFF2B7C0),
        onTertiary = Color(0xFF4B252C),
        tertiaryContainer = Color(0xFF653B42),
        onTertiaryContainer = Color(0xFFFFD9DE),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFB4AB),
        background = Color(0xFF1D1B1E),
        onBackground = Color(0xFFE7E1E5),
        surface = Color(0xFF1D1B1E),
        onSurface = Color(0xFFE7E1E5),
        surfaceVariant = Color(0xFF4A454E),
        onSurfaceVariant = Color(0xFFCCC4CF),
        outline = Color(0xFF958E98),
        outlineVariant = Color(0xFF4A454E),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE7E1E5),
        inverseOnSurface = Color(0xFF322F33),
        inversePrimary = Color(0xFF714C9F),
        surfaceDim = Color(0xFF1D1B1E),
        surfaceBright = Color(0xFF37313D),
        surfaceContainerLowest = Color(0xFF1D1B1E),
        surfaceContainerLow = Color(0xFF262229),
        surfaceContainer = Color(0xFF2C2730),
        surfaceContainerHigh = Color(0xFF322C37),
        surfaceContainerHighest = Color(0xFF37313D),
    )

// Catppuccin Latte 浅色
private val CatppuccinLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF714C9F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEEDBFF),
        onPrimaryContainer = Color(0xFF2A0054),
        secondary = Color(0xFF655A6F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFECDDF7),
        onSecondaryContainer = Color(0xFF20182A),
        tertiary = Color(0xFF805159),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD9DE),
        onTertiaryContainer = Color(0xFF321018),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF1D1B1E),
        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF1D1B1E),
        surfaceVariant = Color(0xFFE8E0EB),
        onSurfaceVariant = Color(0xFF4A454E),
        outline = Color(0xFF7B757F),
        outlineVariant = Color(0xFFCCC4CF),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF322F33),
        inverseOnSurface = Color(0xFFF5EFF4),
        inversePrimary = Color(0xFFD9B9FF),
        surfaceDim = Color(0xFFECE3F2),
        surfaceBright = Color(0xFFFFFBFF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF8F3FA),
        surfaceContainer = Color(0xFFF4EDF7),
        surfaceContainerHigh = Color(0xFFEFE8F4),
        surfaceContainerHighest = Color(0xFFECE3F2),
    )

// Apple 深色 (修复：使用纯黑背景，实现 iOS Grouped 风格)
private val AppleDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF42E355),
        onPrimary = Color(0xFF00390A),
        primaryContainer = Color(0xFF005312),
        onPrimaryContainer = Color(0xFF70FF76),
        secondary = Color(0xFFBACCB3),
        onSecondary = Color(0xFF253423),
        secondaryContainer = Color(0xFF3B4B38),
        onSecondaryContainer = Color(0xFFD5E8CE),
        tertiary = Color(0xFFA0CFD4),
        onTertiary = Color(0xFF00363B),
        tertiaryContainer = Color(0xFF1F4D52),
        onTertiaryContainer = Color(0xFFBCEBF0),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFB4AB),
        background = Color(0xFF1A1C19),
        onBackground = Color(0xFFE2E3DD),
        surface = Color(0xFF1A1C19),
        onSurface = Color(0xFFE2E3DD),
        surfaceVariant = Color(0xFF424940),
        onSurfaceVariant = Color(0xFFC2C9BD),
        outline = Color(0xFF8C9388),
        outlineVariant = Color(0xFF424940),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE2E3DD),
        inverseOnSurface = Color(0xFF2F312D),
        inversePrimary = Color(0xFF006E1C),
        surfaceDim = Color(0xFF1A1C19),
        surfaceBright = Color(0xFF1F3721),
        surfaceContainerLowest = Color(0xFF1A1C19),
        surfaceContainerLow = Color(0xFF1C251C),
        surfaceContainer = Color(0xFF1D2C1E),
        surfaceContainerHigh = Color(0xFF1E3220),
        surfaceContainerHighest = Color(0xFF1F3721),
    )

// Apple 浅色 (修复：使用灰背景+白卡片，实现 iOS Grouped 风格)
private val AppleLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF006E1C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF70FF76),
        onPrimaryContainer = Color(0xFF002204),
        secondary = Color(0xFF52634F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD5E8CE),
        onSecondaryContainer = Color(0xFF111F0F),
        tertiary = Color(0xFF38656A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFBCEBF0),
        onTertiaryContainer = Color(0xFF002023),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFCFDF6),
        onBackground = Color(0xFF1A1C19),
        surface = Color(0xFFFCFDF6),
        onSurface = Color(0xFF1A1C19),
        surfaceVariant = Color(0xFFDEE5D8),
        onSurfaceVariant = Color(0xFF424940),
        outline = Color(0xFF72796F),
        outlineVariant = Color(0xFFC2C9BD),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2F312D),
        inverseOnSurface = Color(0xFFF0F1EB),
        inversePrimary = Color(0xFF42E355),
        surfaceDim = Color(0xFFD9E9D8),
        surfaceBright = Color(0xFFFCFDF6),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF0F6EC),
        surfaceContainer = Color(0xFFE8F2E5),
        surfaceContainerHigh = Color(0xFFE0EDDE),
        surfaceContainerHighest = Color(0xFFD9E9D8),
    )
// Lavender (熏衣草)
private val LavenderDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFC8BFFF),
        onPrimary = Color(0xFF2F2176),
        primaryContainer = Color(0xFF463A8D),
        onPrimaryContainer = Color(0xFFE5DEFF),
        secondary = Color(0xFFC9C3DC),
        onSecondary = Color(0xFF312E41),
        secondaryContainer = Color(0xFF474459),
        onSecondaryContainer = Color(0xFFE5DFF9),
        tertiary = Color(0xFFECB8CE),
        onTertiary = Color(0xFF482536),
        tertiaryContainer = Color(0xFF613B4D),
        onTertiaryContainer = Color(0xFFFFD8E7),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFB4AB),
        background = Color(0xFF1C1B1F),
        onBackground = Color(0xFFE5E1E6),
        surface = Color(0xFF1C1B1F),
        onSurface = Color(0xFFE5E1E6),
        surfaceVariant = Color(0xFF48454F),
        onSurfaceVariant = Color(0xFFC9C5D0),
        outline = Color(0xFF928F99),
        outlineVariant = Color(0xFF48454F),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE5E1E6),
        inverseOnSurface = Color(0xFF313033),
        inversePrimary = Color(0xFF5E52A7),
        surfaceDim = Color(0xFF1C1B1F),
        surfaceBright = Color(0xFF34323E),
        surfaceContainerLowest = Color(0xFF1C1B1F),
        surfaceContainerLow = Color(0xFF24232A),
        surfaceContainer = Color(0xFF292831),
        surfaceContainerHigh = Color(0xFF2F2D38),
        surfaceContainerHighest = Color(0xFF34323E),
    )
private val LavenderLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF5E52A7),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE5DEFF),
        onPrimaryContainer = Color(0xFF190261),
        secondary = Color(0xFF5F5C71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE5DFF9),
        onSecondaryContainer = Color(0xFF1C192B),
        tertiary = Color(0xFF7C5265),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8E7),
        onTertiaryContainer = Color(0xFF301121),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF1C1B1F),
        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFE5E0EC),
        onSurfaceVariant = Color(0xFF48454F),
        outline = Color(0xFF78767F),
        outlineVariant = Color(0xFFC9C5D0),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF313033),
        inverseOnSurface = Color(0xFFF4EFF4),
        inversePrimary = Color(0xFFC8BFFF),
        surfaceDim = Color(0xFFE9E4F3),
        surfaceBright = Color(0xFFFFFBFF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F3FB),
        surfaceContainer = Color(0xFFF2EEF8),
        surfaceContainerHigh = Color(0xFFEDE8F5),
        surfaceContainerHighest = Color(0xFFE9E4F3),
    )

// Midnight (午夜蓝) - GitHub Dark Style
private val MidnightDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFA2C9FF),
        onPrimary = Color(0xFF00315B),
        primaryContainer = Color(0xFF004881),
        onPrimaryContainer = Color(0xFFD3E4FF),
        secondary = Color(0xFFBBC7DB),
        onSecondary = Color(0xFF263141),
        secondaryContainer = Color(0xFF3C4758),
        onSecondaryContainer = Color(0xFFD7E3F8),
        tertiary = Color(0xFFD8BDE3),
        onTertiary = Color(0xFF3C2947),
        tertiaryContainer = Color(0xFF543F5E),
        onTertiaryContainer = Color(0xFFF5D9FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFB4AB),
        background = Color(0xFF1A1C1E),
        onBackground = Color(0xFFE3E2E6),
        surface = Color(0xFF1A1C1E),
        onSurface = Color(0xFFE3E2E6),
        surfaceVariant = Color(0xFF43474E),
        onSurfaceVariant = Color(0xFFC3C6CF),
        outline = Color(0xFF8D9199),
        outlineVariant = Color(0xFF43474E),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE3E2E6),
        inverseOnSurface = Color(0xFF2F3033),
        inversePrimary = Color(0xFF1460A5),
        surfaceDim = Color(0xFF1A1C1E),
        surfaceBright = Color(0xFF2D343D),
        surfaceContainerLowest = Color(0xFF1A1C1E),
        surfaceContainerLow = Color(0xFF202429),
        surfaceContainer = Color(0xFF252A30),
        surfaceContainerHigh = Color(0xFF292F37),
        surfaceContainerHighest = Color(0xFF2D343D),
    )
private val MidnightLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF0060AA),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD3E4FF),
        onPrimaryContainer = Color(0xFF001C38),
        secondary = Color(0xFF545F70),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD8E3F8),
        onSecondaryContainer = Color(0xFF111C2B),
        tertiary = Color(0xFF6C5677),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF5D9FF),
        onTertiaryContainer = Color(0xFF261430),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFDFCFF),
        onBackground = Color(0xFF1A1C1E),
        surface = Color(0xFFFDFCFF),
        onSurface = Color(0xFF1A1C1E),
        surfaceVariant = Color(0xFFDFE2EB),
        onSurfaceVariant = Color(0xFF43474E),
        outline = Color(0xFF73777F),
        outlineVariant = Color(0xFFC3C6CF),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2F3033),
        inverseOnSurface = Color(0xFFF1F0F4),
        inversePrimary = Color(0xFFA2C9FF),
        surfaceDim = Color(0xFFEDF1F7),
        surfaceBright = Color(0xFFFDFCFF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF1F5FB),
        surfaceContainer = Color(0xFFE9F0F8),
        surfaceContainerHigh = Color(0xFFE1EBF6),
        surfaceContainerHighest = Color(0xFFDAE7F3),
    )

// Nord (北极光)
private val NordDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF56D6F4),
        onPrimary = Color(0xFF003640),
        primaryContainer = Color(0xFF004E5C),
        onPrimaryContainer = Color(0xFFACECFF),
        secondary = Color(0xFFB2CBD2),
        onSecondary = Color(0xFF1D343A),
        secondaryContainer = Color(0xFF334A51),
        onSecondaryContainer = Color(0xFFCEE7EF),
        tertiary = Color(0xFFBFC4EB),
        onTertiary = Color(0xFF282F4D),
        tertiaryContainer = Color(0xFF3F4565),
        onTertiaryContainer = Color(0xFFDDE1FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFB4AB),
        background = Color(0xFF191C1D),
        onBackground = Color(0xFFE1E3E4),
        surface = Color(0xFF191C1D),
        onSurface = Color(0xFFE1E3E4),
        surfaceVariant = Color(0xFF3F484B),
        onSurfaceVariant = Color(0xFFBFC8CB),
        outline = Color(0xFF899295),
        outlineVariant = Color(0xFF3F484B),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE1E3E4),
        inverseOnSurface = Color(0xFF2E3132),
        inversePrimary = Color(0xFF00687A),
        surfaceDim = Color(0xFF191C1D),
        surfaceBright = Color(0xFF21363B),
        surfaceContainerLowest = Color(0xFF191C1D),
        surfaceContainerLow = Color(0xFF1C2527),
        surfaceContainer = Color(0xFF1E2B2E),
        surfaceContainerHigh = Color(0xFF203035),
        surfaceContainerHighest = Color(0xFF21363B),
    )
private val NordLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF00687A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFACECFF),
        onPrimaryContainer = Color(0xFF001F26),
        secondary = Color(0xFF4B6269),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCEE7EF),
        onSecondaryContainer = Color(0xFF061F24),
        tertiary = Color(0xFF575D7E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFDDE1FF),
        onTertiaryContainer = Color(0xFF131937),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFBFCFD),
        onBackground = Color(0xFF191C1D),
        surface = Color(0xFFFBFCFD),
        onSurface = Color(0xFF191C1D),
        surfaceVariant = Color(0xFFDBE4E7),
        onSurfaceVariant = Color(0xFF3F484B),
        outline = Color(0xFF70797C),
        outlineVariant = Color(0xFFBFC8CB),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2E3132),
        inverseOnSurface = Color(0xFFEFF1F2),
        inversePrimary = Color(0xFF56D6F4),
        surfaceDim = Color(0xFFE7F0F3),
        surfaceBright = Color(0xFFFBFCFD),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFEFF5F7),
        surfaceContainer = Color(0xFFE7F0F3),
        surfaceContainerHigh = Color(0xFFDFECEF),
        surfaceContainerHighest = Color(0xFFD9E8EB),
    )

// Strawberry (草莓)
private val StrawberryDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFFFB1C8),
        onPrimary = Color(0xFF600D32),
        primaryContainer = Color(0xFF7D2649),
        onPrimaryContainer = Color(0xFFFFD9E2),
        secondary = Color(0xFFE3BDC6),
        onSecondary = Color(0xFF422931),
        secondaryContainer = Color(0xFF5B3F47),
        onSecondaryContainer = Color(0xFFFFD9E2),
        tertiary = Color(0xFFEFBD94),
        onTertiary = Color(0xFF48290B),
        tertiaryContainer = Color(0xFF613F20),
        onTertiaryContainer = Color(0xFFFFDCC1),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFB4AB),
        background = Color(0xFF201A1B),
        onBackground = Color(0xFFEBE0E1),
        surface = Color(0xFF201A1B),
        onSurface = Color(0xFFEBE0E1),
        surfaceVariant = Color(0xFF514347),
        onSurfaceVariant = Color(0xFFD5C2C6),
        outline = Color(0xFF9E8C90),
        outlineVariant = Color(0xFF514347),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFEBE0E1),
        inverseOnSurface = Color(0xFF352F30),
        inversePrimary = Color(0xFF9B3E61),
        surfaceDim = Color(0xFF201A1B),
        surfaceBright = Color(0xFF3F2F33),
        surfaceContainerLowest = Color(0xFF201A1B),
        surfaceContainerLow = Color(0xFF2A2123),
        surfaceContainer = Color(0xFF312629),
        surfaceContainerHigh = Color(0xFF382B2E),
        surfaceContainerHighest = Color(0xFF3F2F33),
    )
private val StrawberryLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF9B3E61),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD9E2),
        onPrimaryContainer = Color(0xFF3E001D),
        secondary = Color(0xFF74565F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFD9E2),
        onSecondaryContainer = Color(0xFF2B151C),
        tertiary = Color(0xFF7C5635),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDCC1),
        onTertiaryContainer = Color(0xFF2E1500),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF201A1B),
        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF201A1B),
        surfaceVariant = Color(0xFFF2DDE1),
        onSurfaceVariant = Color(0xFF514347),
        outline = Color(0xFF837377),
        outlineVariant = Color(0xFFD5C2C6),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF352F30),
        inverseOnSurface = Color(0xFFFAEEEF),
        inversePrimary = Color(0xFFFFB1C8),
        surfaceDim = Color(0xFFF7ECF3),
        surfaceBright = Color(0xFFFFFBFF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFAF2F8),
        surfaceContainer = Color(0xFFF7ECF3),
        surfaceContainerHigh = Color(0xFFF4E6EE),
        surfaceContainerHighest = Color(0xFFF1E1E9),
    )

// Tako (章鱼紫)
private val TakoDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFD4BBFF),
        onPrimary = Color(0xFF3D1A73),
        primaryContainer = Color(0xFF54348B),
        onPrimaryContainer = Color(0xFFEBDCFF),
        secondary = Color(0xFFCDC2DB),
        onSecondary = Color(0xFF342D40),
        secondaryContainer = Color(0xFF4B4358),
        onSecondaryContainer = Color(0xFFEADef7),
        tertiary = Color(0xFFF1B7C4),
        onTertiary = Color(0xFF4A252F),
        tertiaryContainer = Color(0xFF643B45),
        onTertiaryContainer = Color(0xFFFFD9E0),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFB4AB),
        background = Color(0xFF1D1B1E),
        onBackground = Color(0xFFE6E1E6),
        surface = Color(0xFF1D1B1E),
        onSurface = Color(0xFFE6E1E6),
        surfaceVariant = Color(0xFF49454E),
        onSurfaceVariant = Color(0xFFCBC4CF),
        outline = Color(0xFF948F99),
        outlineVariant = Color(0xFF49454E),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE6E1E6),
        inverseOnSurface = Color(0xFF323033),
        inversePrimary = Color(0xFF6D4DA5),
        surfaceDim = Color(0xFF1D1B1E),
        surfaceBright = Color(0xFF36313D),
        surfaceContainerLowest = Color(0xFF1D1B1E),
        surfaceContainerLow = Color(0xFF262329),
        surfaceContainer = Color(0xFF2B2830),
        surfaceContainerHigh = Color(0xFF312D37),
        surfaceContainerHighest = Color(0xFF36313D),
    )
private val TakoLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF6D4DA5),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEBDCFF),
        onPrimaryContainer = Color(0xFF260058),
        secondary = Color(0xFF635B70),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFEADEF7),
        onSecondaryContainer = Color(0xFF1F182A),
        tertiary = Color(0xFF7F525D),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD9E0),
        onTertiaryContainer = Color(0xFF32101B),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF1D1B1E),
        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF1D1B1E),
        surfaceVariant = Color(0xFFE7E0EB),
        onSurfaceVariant = Color(0xFF49454E),
        outline = Color(0xFF7A757F),
        outlineVariant = Color(0xFFCBC4CF),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF323033),
        inverseOnSurface = Color(0xFFF5EFF4),
        inversePrimary = Color(0xFFD4BBFF),
        surfaceDim = Color(0xFFF4EDF8),
        surfaceBright = Color(0xFFFFFBFF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF8F3FB),
        surfaceContainer = Color(0xFFF4EDF8),
        surfaceContainerHigh = Color(0xFFEFE8F5),
        surfaceContainerHighest = Color(0xFFEBE3F3),
    )

// ============================================================================
// 2. 核心算法: 使用 Material Kolor 库替代手动实现
// ============================================================================

@Composable
fun AppTheme(themeState: ThemeState, content: @Composable () -> Unit) {
    val context = LocalContext.current

    val useDarkTheme =
        when (themeState.selectedModeIndex) {
            0 -> isSystemInDarkTheme()
            1 -> false
            2 -> true
            else -> isSystemInDarkTheme()
        }

    SideEffect {
        LogCatcher.d(
            "ThemeDebug_Apply",
            "应用主题中... Monet=${themeState.isMonetEnabled}, Custom=${themeState.isCustomTheme}, 模式=$useDarkTheme, 自定义色=${themeState.customColor.value}",
        )
    }

    // Determine target color scheme
    val targetColorScheme =
        if (themeState.isCustomTheme) {
            LogCatcher.i(
                "ThemeDebug_Branch",
                ">>> 命中分支: Custom (用户自定义 - Material Kolor), 颜色: ${themeState.customColor.value}",
            )
            rememberDynamicColorScheme(
                seedColor = themeState.customColor,
                isDark = useDarkTheme,
                style = themeState.style,
                isAmoled = false,
            )
        } else {
            when {
                themeState.isMonetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    LogCatcher.i("ThemeDebug_Branch", ">>> 命中分支: Monet (系统壁纸取色)")
                    if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                else -> {
                    LogCatcher.i(
                        "ThemeDebug_Branch",
                        ">>> 命中分支: Preset (预设主题), Index: ${themeState.selectedThemeIndex}",
                    )
                    when (themeState.selectedThemeIndex) {
                        0 -> if (useDarkTheme) CatppuccinDarkColorScheme else CatppuccinLightColorScheme
                        1 -> if (useDarkTheme) AppleDarkColorScheme else AppleLightColorScheme
                        2 -> if (useDarkTheme) LavenderDarkColorScheme else LavenderLightColorScheme
                        3 -> if (useDarkTheme) MidnightDarkColorScheme else MidnightLightColorScheme
                        4 -> if (useDarkTheme) NordDarkColorScheme else NordLightColorScheme
                        5 -> if (useDarkTheme) StrawberryDarkColorScheme else StrawberryLightColorScheme
                        6 -> if (useDarkTheme) TakoDarkColorScheme else TakoLightColorScheme
                        else -> if (useDarkTheme) DarkColorScheme else LightColorScheme
                    }
                }
            }
        }

    // Animate the color scheme
    val animatedColorScheme by
        animateColorSchemeAsState(targetColorScheme = targetColorScheme, animationSpec = tween(durationMillis = 300))

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    LaunchedEffect(animatedColorScheme, useDarkTheme) {
        com.scto.mide.term.ui.theme.SharedThemeState.currentColorScheme.value = animatedColorScheme
        com.scto.mide.term.ui.theme.SharedThemeState.isDark.value = useDarkTheme
    }

    MaterialTheme(colorScheme = animatedColorScheme, typography = Typography, content = content)
}

@Composable
fun animateColorSchemeAsState(
    targetColorScheme: ColorScheme,
    animationSpec: AnimationSpec<Color> = tween(durationMillis = 300),
): State<ColorScheme> {
    val primary = animateColorAsState(targetColorScheme.primary, animationSpec)
    val onPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec)
    val primaryContainer = animateColorAsState(targetColorScheme.primaryContainer, animationSpec)
    val onPrimaryContainer = animateColorAsState(targetColorScheme.onPrimaryContainer, animationSpec)
    val inversePrimary = animateColorAsState(targetColorScheme.inversePrimary, animationSpec)
    val secondary = animateColorAsState(targetColorScheme.secondary, animationSpec)
    val onSecondary = animateColorAsState(targetColorScheme.onSecondary, animationSpec)
    val secondaryContainer = animateColorAsState(targetColorScheme.secondaryContainer, animationSpec)
    val onSecondaryContainer = animateColorAsState(targetColorScheme.onSecondaryContainer, animationSpec)
    val tertiary = animateColorAsState(targetColorScheme.tertiary, animationSpec)
    val onTertiary = animateColorAsState(targetColorScheme.onTertiary, animationSpec)
    val tertiaryContainer = animateColorAsState(targetColorScheme.tertiaryContainer, animationSpec)
    val onTertiaryContainer = animateColorAsState(targetColorScheme.onTertiaryContainer, animationSpec)
    val background = animateColorAsState(targetColorScheme.background, animationSpec)
    val onBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec)
    val surface = animateColorAsState(targetColorScheme.surface, animationSpec)
    val onSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec)
    val surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec)
    val onSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec)
    val surfaceTint = animateColorAsState(targetColorScheme.surfaceTint, animationSpec)
    val inverseSurface = animateColorAsState(targetColorScheme.inverseSurface, animationSpec)
    val inverseOnSurface = animateColorAsState(targetColorScheme.inverseOnSurface, animationSpec)
    val error = animateColorAsState(targetColorScheme.error, animationSpec)
    val onError = animateColorAsState(targetColorScheme.onError, animationSpec)
    val errorContainer = animateColorAsState(targetColorScheme.errorContainer, animationSpec)
    val onErrorContainer = animateColorAsState(targetColorScheme.onErrorContainer, animationSpec)
    val outline = animateColorAsState(targetColorScheme.outline, animationSpec)
    val outlineVariant = animateColorAsState(targetColorScheme.outlineVariant, animationSpec)
    val scrim = animateColorAsState(targetColorScheme.scrim, animationSpec)
    val surfaceBright = animateColorAsState(targetColorScheme.surfaceBright, animationSpec)
    val surfaceDim = animateColorAsState(targetColorScheme.surfaceDim, animationSpec)
    val surfaceContainer = animateColorAsState(targetColorScheme.surfaceContainer, animationSpec)
    val surfaceContainerHigh = animateColorAsState(targetColorScheme.surfaceContainerHigh, animationSpec)
    val surfaceContainerHighest = animateColorAsState(targetColorScheme.surfaceContainerHighest, animationSpec)
    val surfaceContainerLow = animateColorAsState(targetColorScheme.surfaceContainerLow, animationSpec)
    val surfaceContainerLowest = animateColorAsState(targetColorScheme.surfaceContainerLowest, animationSpec)
    val primaryFixed = animateColorAsState(targetColorScheme.primaryFixed, animationSpec)
    val primaryFixedDim = animateColorAsState(targetColorScheme.primaryFixedDim, animationSpec)
    val onPrimaryFixed = animateColorAsState(targetColorScheme.onPrimaryFixed, animationSpec)
    val onPrimaryFixedVariant = animateColorAsState(targetColorScheme.onPrimaryFixedVariant, animationSpec)
    val secondaryFixed = animateColorAsState(targetColorScheme.secondaryFixed, animationSpec)
    val secondaryFixedDim = animateColorAsState(targetColorScheme.secondaryFixedDim, animationSpec)
    val onSecondaryFixed = animateColorAsState(targetColorScheme.onSecondaryFixed, animationSpec)
    val onSecondaryFixedVariant = animateColorAsState(targetColorScheme.onSecondaryFixedVariant, animationSpec)
    val tertiaryFixed = animateColorAsState(targetColorScheme.tertiaryFixed, animationSpec)
    val tertiaryFixedDim = animateColorAsState(targetColorScheme.tertiaryFixedDim, animationSpec)
    val onTertiaryFixed = animateColorAsState(targetColorScheme.onTertiaryFixed, animationSpec)
    val onTertiaryFixedVariant = animateColorAsState(targetColorScheme.onTertiaryFixedVariant, animationSpec)

    return remember(targetColorScheme) {
        derivedStateOf {
            ColorScheme(
                primary = primary.value,
                onPrimary = onPrimary.value,
                primaryContainer = primaryContainer.value,
                onPrimaryContainer = onPrimaryContainer.value,
                inversePrimary = inversePrimary.value,
                secondary = secondary.value,
                onSecondary = onSecondary.value,
                secondaryContainer = secondaryContainer.value,
                onSecondaryContainer = onSecondaryContainer.value,
                tertiary = tertiary.value,
                onTertiary = onTertiary.value,
                tertiaryContainer = tertiaryContainer.value,
                onTertiaryContainer = onTertiaryContainer.value,
                background = background.value,
                onBackground = onBackground.value,
                surface = surface.value,
                onSurface = onSurface.value,
                surfaceVariant = surfaceVariant.value,
                onSurfaceVariant = onSurfaceVariant.value,
                surfaceTint = surfaceTint.value,
                inverseSurface = inverseSurface.value,
                inverseOnSurface = inverseOnSurface.value,
                error = error.value,
                onError = onError.value,
                errorContainer = errorContainer.value,
                onErrorContainer = onErrorContainer.value,
                outline = outline.value,
                outlineVariant = outlineVariant.value,
                scrim = scrim.value,
                surfaceBright = surfaceBright.value,
                surfaceDim = surfaceDim.value,
                surfaceContainer = surfaceContainer.value,
                surfaceContainerHigh = surfaceContainerHigh.value,
                surfaceContainerHighest = surfaceContainerHighest.value,
                surfaceContainerLow = surfaceContainerLow.value,
                surfaceContainerLowest = surfaceContainerLowest.value,
                primaryFixed = primaryFixed.value,
                primaryFixedDim = primaryFixedDim.value,
                onPrimaryFixed = onPrimaryFixed.value,
                onPrimaryFixedVariant = onPrimaryFixedVariant.value,
                secondaryFixed = secondaryFixed.value,
                secondaryFixedDim = secondaryFixedDim.value,
                onSecondaryFixed = onSecondaryFixed.value,
                onSecondaryFixedVariant = onSecondaryFixedVariant.value,
                tertiaryFixed = tertiaryFixed.value,
                tertiaryFixedDim = tertiaryFixedDim.value,
                onTertiaryFixed = onTertiaryFixed.value,
                onTertiaryFixedVariant = onTertiaryFixedVariant.value,
            )
        }
    }
}
