package com.mobile.ide.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import com.mobile.ide.core.utils.LogCatcher
import com.mobile.ide.core.ui.theme.ThemeState

import kotlin.math.pow

// ============================================================================
// 1. 预设主题 (保留原样)
// ============================================================================
// ============================================================================
// 1. 预设主题 (已修复 Scrim 缺失与卡片对比度问题)
// ============================================================================

// 默认 Material 深色 (保留官方微调)
private val DarkColorScheme = darkColorScheme(
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
    surfaceContainerHighest = Color(0xFF36343B)
)

// 默认 Material 浅色
private val LightColorScheme = lightColorScheme(
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
    surfaceContainerHighest = Color(0xFFE6E0E9)
)

// Catppuccin Mocha 深色 (修复背景与卡片撞色)
private val CatppuccinDarkColorScheme = darkColorScheme(
    primary = Color(0xFFCBA6F7),
    onPrimary = Color(0xFF2B1B42),
    primaryContainer = Color(0xFF3D2A59),
    onPrimaryContainer = Color(0xFFEDDCFF),
    secondary = Color(0xFFF5C2E7),
    onSecondary = Color(0xFF421F3A),
    secondaryContainer = Color(0xFF5A3051),
    onSecondaryContainer = Color(0xFFFFD9F2),
    tertiary = Color(0xFFF38BA8),
    onTertiary = Color(0xFF4C1926),
    tertiaryContainer = Color(0xFF692D3B),
    onTertiaryContainer = Color(0xFFFFD9E0),
    error = Color(0xFFF38BA8),
    onError = Color(0xFF4C1926),
    errorContainer = Color(0xFF692D3B),
    onErrorContainer = Color(0xFFFFD9E0),
    background = Color(0xFF11111B), // 🔥 修改：使用 Crust 作为背景 (更深)
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF11111B),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF313244),
    onSurfaceVariant = Color(0xFFBAC2DE),
    outline = Color(0xFF6C7086),
    outlineVariant = Color(0xFF45475A),
    scrim = Color(0xFF000000), // ✅ 已补全
    inverseSurface = Color(0xFFCDD6F4),
    inverseOnSurface = Color(0xFF181825),
    inversePrimary = Color(0xFF7F56D9),
    surfaceDim = Color(0xFF11111B),
    surfaceBright = Color(0xFF313244),
    surfaceContainerLowest = Color(0xFF0F0F16),
    surfaceContainerLow = Color(0xFF181825), // Mantle (ElevatedCard)
    surfaceContainer = Color(0xFF1E1E2E),    // Base (NavDrawer/BottomNav)
    surfaceContainerHigh = Color(0xFF313244),
    surfaceContainerHighest = Color(0xFF45475A)
)

// Catppuccin Latte 浅色
private val CatppuccinLightColorScheme = lightColorScheme(
    primary = Color(0xFFD20F39),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFFE64553),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410002),
    tertiary = Color(0xFFEA76CB),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8F0),
    onTertiaryContainer = Color(0xFF3D0031),
    error = Color(0xFFD20F39),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFEFF1F5), // Base
    onBackground = Color(0xFF4C4F69),
    surface = Color(0xFFEFF1F5),
    onSurface = Color(0xFF4C4F69),
    surfaceVariant = Color(0xFFE6E9EF),
    onSurfaceVariant = Color(0xFF5C5F77),
    outline = Color(0xFF9CA0B0),
    outlineVariant = Color(0xFFCCD0DA),
    scrim = Color(0xFF000000), // ✅ 已补全
    inverseSurface = Color(0xFF4C4F69),
    inverseOnSurface = Color(0xFFEFF1F5),
    inversePrimary = Color(0xFFFFB4AB),
    surfaceDim = Color(0xFFDCE0E8),
    surfaceBright = Color(0xFFFFFBFE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F5F5), // 比背景略白
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE6E9EF),
    surfaceContainerHighest = Color(0xFFDCE0E8)
)

// Apple 深色 (修复：使用纯黑背景，实现 iOS Grouped 风格)
private val AppleDarkColorScheme = darkColorScheme(
    primary = Color(0xFF32D74B),
    onPrimary = Color(0xFF003910),
    primaryContainer = Color(0xFF005219),
    onPrimaryContainer = Color(0xFFB3F5BF),
    secondary = Color(0xFF64D2FF),
    onSecondary = Color(0xFF003547),
    secondaryContainer = Color(0xFF004D66),
    onSecondaryContainer = Color(0xFFBFE9FF),
    tertiary = Color(0xFFBF5AF2),
    onTertiary = Color(0xFF3D0066),
    tertiaryContainer = Color(0xFF550092),
    onTertiaryContainer = Color(0xFFECCCFF),
    error = Color(0xFFFF453A),
    onError = Color(0xFF680003),
    errorContainer = Color(0xFF930006),
    onErrorContainer = Color(0xFFFFDAD4),
    background = Color(0xFF000000), // 🔥 修改：iOS 深色背景通常是纯黑
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF1C1C1E), // 填充卡片色
    onSurfaceVariant = Color(0xFFE5E5EA),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF3A3A3C),
    scrim = Color(0xFF000000), // ✅ 已补全
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF00A32A),
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF1C1C1E),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF1C1C1E), // 🔥 修改：悬浮卡片为深灰色
    surfaceContainer = Color(0xFF2C2C2E),
    surfaceContainerHigh = Color(0xFF3A3A3C),
    surfaceContainerHighest = Color(0xFF48484A)
)

// Apple 浅色 (修复：使用灰背景+白卡片，实现 iOS Grouped 风格)
private val AppleLightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF), // 改为 iOS 标准蓝 (原绿色有点偏)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FF),
    onPrimaryContainer = Color(0xFF001C3A),
    secondary = Color(0xFF34C759), // 绿色作为 Secondary
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB3F5BF),
    onSecondaryContainer = Color(0xFF002107),
    tertiary = Color(0xFFBF5AF2),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECCCFF),
    onTertiaryContainer = Color(0xFF2B0049),
    error = Color(0xFFFF3B30),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410001),
    background = Color(0xFFF2F2F7), // 🔥 修改：iOS Grouped 背景色 (浅灰)
    onBackground = Color(0xFF000000),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF000000),
    outline = Color(0xFFC7C7CC),
    outlineVariant = Color(0xFFD1D1D6),
    scrim = Color(0xFF000000), // ✅ 已补全
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFF2F2F7),
    inversePrimary = Color(0xFF0A84FF),
    surfaceDim = Color(0xFFE5E5EA),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF), // 🔥 修改：卡片为纯白
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE5E5EA),
    surfaceContainerHighest = Color(0xFFD1D1D6)
)

// Lavender (熏衣草)
private val LavenderDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB8ADFF), onPrimary = Color(0xFF2B1B5C), primaryContainer = Color(0xFF422E7A), onPrimaryContainer = Color(0xFFE1D9FF),
    secondary = Color(0xFFCBC0FF), onSecondary = Color(0xFF332761), secondaryContainer = Color(0xFF4A3D78), onSecondaryContainer = Color(0xFFE8DDFF),
    tertiary = Color(0xFFFFABED), onTertiary = Color(0xFF5C0050), tertiaryContainer = Color(0xFF7F0070), onTertiaryContainer = Color(0xFFFFD7F5),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141218), onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF141218), onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF49454F), onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99), outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000), // ✅
    inverseSurface = Color(0xFFE6E1E9), inverseOnSurface = Color(0xFF313033), inversePrimary = Color(0xFF6750A4),
    surfaceDim = Color(0xFF141020), surfaceBright = Color(0xFF3B383E),
    surfaceContainerLowest = Color(0xFF0F0D13), surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26), surfaceContainerHigh = Color(0xFF2B2930), surfaceContainerHighest = Color(0xFF36343B)
)
private val LavenderLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFE1D9FF), onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFF5E5A71), onSecondary = Color(0xFFFFFFFF), secondaryContainer = Color(0xFFE8DDFF), onSecondaryContainer = Color(0xFF1A182B),
    tertiary = Color(0xFFA20095), onTertiary = Color(0xFFFFFFFF), tertiaryContainer = Color(0xFFFFD7F5), onTertiaryContainer = Color(0xFF3A0036),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFE), onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE), onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC), onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E), outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000), // ✅
    inverseSurface = Color(0xFF313033), inverseOnSurface = Color(0xFFF4EFF4), inversePrimary = Color(0xFFB8ADFF),
    surfaceDim = Color(0xFFDED8E1), surfaceBright = Color(0xFFFFFBFE),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7), surfaceContainerHigh = Color(0xFFECE6F0), surfaceContainerHighest = Color(0xFFE6E0E9)
)

// Midnight (午夜蓝) - GitHub Dark Style
private val MidnightDarkColorScheme = darkColorScheme(
    primary = Color(0xFF58A6FF), onPrimary = Color(0xFF003258), primaryContainer = Color(0xFF004A7C), onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF79C0FF), onSecondary = Color(0xFF003D5F), secondaryContainer = Color(0xFF005885), onSecondaryContainer = Color(0xFFCDE5FF),
    tertiary = Color(0xFFD2A8FF), onTertiary = Color(0xFF3B0072), tertiaryContainer = Color(0xFF5300A0), onTertiaryContainer = Color(0xFFEBDCFF),
    error = Color(0xFFFF7B72), onError = Color(0xFF8E1B13), errorContainer = Color(0xFFDA3633), onErrorContainer = Color(0xFFFFE0E0),
    background = Color(0xFF010409), // 🔥 极黑深蓝背景
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF010409), onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF161B22), onSurfaceVariant = Color(0xFFC9D1D9),
    outline = Color(0xFF6E7681), outlineVariant = Color(0xFF30363D),
    scrim = Color(0xFF000000), // ✅
    inverseSurface = Color(0xFFE6EDF3), inverseOnSurface = Color(0xFF0D1117), inversePrimary = Color(0xFF0969DA),
    surfaceDim = Color(0xFF010409), surfaceBright = Color(0xFF161B22),
    surfaceContainerLowest = Color(0xFF010409), surfaceContainerLow = Color(0xFF0D1117), // 略微提亮
    surfaceContainer = Color(0xFF161B22), surfaceContainerHigh = Color(0xFF21262D), surfaceContainerHighest = Color(0xFF30363D)
)
private val MidnightLightColorScheme = lightColorScheme(
    primary = Color(0xFF0969DA), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFD1E4FF), onPrimaryContainer = Color(0xFF001D35),
    secondary = Color(0xFF0550AE), onSecondary = Color(0xFFFFFFFF), secondaryContainer = Color(0xFFCDE5FF), onSecondaryContainer = Color(0xFF001A30),
    tertiary = Color(0xFF8250DF), onTertiary = Color(0xFFFFFFFF), tertiaryContainer = Color(0xFFEBDCFF), onTertiaryContainer = Color(0xFF2B0058),
    error = Color(0xFFCF222E), onError = Color(0xFFFFFFFF), errorContainer = Color(0xFFFFE0E0), onErrorContainer = Color(0xFF5D0F0F),
    background = Color(0xFFFFFFFF), onBackground = Color(0xFF0D1117),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF0D1117),
    surfaceVariant = Color(0xFFF6F8FA), onSurfaceVariant = Color(0xFF24292F),
    outline = Color(0xFFD0D7DE), outlineVariant = Color(0xFFE6EDF3),
    scrim = Color(0xFF000000), // ✅
    inverseSurface = Color(0xFF0D1117), inverseOnSurface = Color(0xFFE6EDF3), inversePrimary = Color(0xFF58A6FF),
    surfaceDim = Color(0xFFE6EDF3), surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF6F8FA),
    surfaceContainer = Color(0xFFE6EDF3), surfaceContainerHigh = Color(0xFFD0D7DE), surfaceContainerHighest = Color(0xFFBEC5CC)
)

// Nord (北极光)
private val NordDarkColorScheme = darkColorScheme(
    primary = Color(0xFF88C0D0), onPrimary = Color(0xFF003544), primaryContainer = Color(0xFF004D62), onPrimaryContainer = Color(0xFFB8E7F5),
    secondary = Color(0xFF81A1C1), onSecondary = Color(0xFF003351), secondaryContainer = Color(0xFF004A74), onSecondaryContainer = Color(0xFFCFE4FF),
    tertiary = Color(0xFFB48EAD), onTertiary = Color(0xFF3E2845), tertiaryContainer = Color(0xFF563E5C), onTertiaryContainer = Color(0xFFFFD6F7),
    error = Color(0xFFBF616A), onError = Color(0xFF4A1419), errorContainer = Color(0xFF6E2428), onErrorContainer = Color(0xFFFFD9DC),
    background = Color(0xFF242933), // 🔥 压暗背景 (比默认 Nord 背景更深，制造层级)
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF242933), onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF3B4252), onSurfaceVariant = Color(0xFFD8DEE9),
    outline = Color(0xFF4C566A), outlineVariant = Color(0xFF434C5E),
    scrim = Color(0xFF000000), // ✅
    inverseSurface = Color(0xFFECEFF4), inverseOnSurface = Color(0xFF2E3440), inversePrimary = Color(0xFF5E81AC),
    surfaceDim = Color(0xFF2B323E), surfaceBright = Color(0xFF3B4252),
    surfaceContainerLowest = Color(0xFF242933), surfaceContainerLow = Color(0xFF2E3440), // 标准 Nord 背景作为卡片
    surfaceContainer = Color(0xFF3B4252), surfaceContainerHigh = Color(0xFF434C5E), surfaceContainerHighest = Color(0xFF4C566A)
)
private val NordLightColorScheme = lightColorScheme(
    primary = Color(0xFF5E81AC), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFB8E7F5), onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF81A1C1), onSecondary = Color(0xFFFFFFFF), secondaryContainer = Color(0xFFCFE4FF), onSecondaryContainer = Color(0xFF001D33),
    tertiary = Color(0xFFB48EAD), onTertiary = Color(0xFFFFFFFF), tertiaryContainer = Color(0xFFFFD6F7), onTertiaryContainer = Color(0xFF2F1A33),
    error = Color(0xFFBF616A), onError = Color(0xFFFFFFFF), errorContainer = Color(0xFFFFD9DC), onErrorContainer = Color(0xFF410004),
    background = Color(0xFFECEFF4), onBackground = Color(0xFF2E3440),
    surface = Color(0xFFECEFF4), onSurface = Color(0xFF2E3440),
    surfaceVariant = Color(0xFFE5E9F0), onSurfaceVariant = Color(0xFF3B4252),
    outline = Color(0xFFD8DEE9), outlineVariant = Color(0xFFE5E9F0),
    scrim = Color(0xFF000000), // ✅
    inverseSurface = Color(0xFF2E3440), inverseOnSurface = Color(0xFFECEFF4), inversePrimary = Color(0xFF88C0D0),
    surfaceDim = Color(0xFFD8DEE9), surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFE5E9F0),
    surfaceContainer = Color(0xFFECEFF4), surfaceContainerHigh = Color(0xFFD8DEE9), surfaceContainerHighest = Color(0xFFC3C9D4)
)

// Strawberry (草莓)
private val StrawberryDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF8FB4), onPrimary = Color(0xFF5F0032), primaryContainer = Color(0xFF840048), onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFFFB1C8), onSecondary = Color(0xFF5E1137), secondaryContainer = Color(0xFF7A2A4E), onSecondaryContainer = Color(0xFFFFD9E4),
    tertiary = Color(0xFFFFABB8), onTertiary = Color(0xFF5F0919), tertiaryContainer = Color(0xFF7D1D2C), onTertiaryContainer = Color(0xFFFFD9DC),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF19100F), // 🔥 略微深红黑
    onBackground = Color(0xFFEFDEE3),
    surface = Color(0xFF19100F), onSurface = Color(0xFFEFDEE3),
    surfaceVariant = Color(0xFF524347), onSurfaceVariant = Color(0xFFD7C2C7),
    outline = Color(0xFF9F8C91), outlineVariant = Color(0xFF524347),
    scrim = Color(0xFF000000), // ✅
    inverseSurface = Color(0xFFEFDEE3), inverseOnSurface = Color(0xFF362B2E), inversePrimary = Color(0xFFA8005C),
    surfaceDim = Color(0xFF19100F), surfaceBright = Color(0xFF3F3437),
    surfaceContainerLowest = Color(0xFF130C0E), surfaceContainerLow = Color(0xFF201418),
    surfaceContainer = Color(0xFF281B1E), surfaceContainerHigh = Color(0xFF332528), surfaceContainerHighest = Color(0xFF3E3033)
)
private val StrawberryLightColorScheme = lightColorScheme(
    primary = Color(0xFFFF6B9D), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFFFD9E2), onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFFD0477E), onSecondary = Color(0xFFFFFFFF), secondaryContainer = Color(0xFFFFD9E4), onSecondaryContainer = Color(0xFF3E0021),
    tertiary = Color(0xFFE83A59), onTertiary = Color(0xFFFFFFFF), tertiaryContainer = Color(0xFFFFD9DC), onTertiaryContainer = Color(0xFF410004),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF), onBackground = Color(0xFF201418),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF201418),
    surfaceVariant = Color(0xFFF4DDE1), onSurfaceVariant = Color(0xFF524347),
    outline = Color(0xFF847377), outlineVariant = Color(0xFFD7C2C7),
    scrim = Color(0xFF000000), // ✅
    inverseSurface = Color(0xFF362B2E), inverseOnSurface = Color(0xFFFAEDEF), inversePrimary = Color(0xFFFF8FB4),
    surfaceDim = Color(0xFFE4D7D9), surfaceBright = Color(0xFFFFFBFF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFEF0F2),
    surfaceContainer = Color(0xFFF8EAEC), surfaceContainerHigh = Color(0xFFF2E4E7), surfaceContainerHighest = Color(0xFFECDEE1)
)

// Tako (章鱼紫)
private val TakoDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9D7CD8), onPrimary = Color(0xFF371F5A), primaryContainer = Color(0xFF4E3571), onPrimaryContainer = Color(0xFFE3D4FF),
    secondary = Color(0xFFB79FE8), onSecondary = Color(0xFF3E2661), secondaryContainer = Color(0xFF553C79), onSecondaryContainer = Color(0xFFEBDCFF),
    tertiary = Color(0xFFC790FF), onTertiary = Color(0xFF401466), tertiaryContainer = Color(0xFF592A7E), onTertiaryContainer = Color(0xFFF0D9FF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF14101F), // 🔥 深紫黑
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF14101F), onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF2D2640), onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99), outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000), // ✅
    inverseSurface = Color(0xFFE6E1E9), inverseOnSurface = Color(0xFF313033), inversePrimary = Color(0xFF6750A4),
    surfaceDim = Color(0xFF141020), surfaceBright = Color(0xFF3B383E),
    surfaceContainerLowest = Color(0xFF0F0B1B), surfaceContainerLow = Color(0xFF1A1625),
    surfaceContainer = Color(0xFF211F26), surfaceContainerHigh = Color(0xFF2B2930), surfaceContainerHighest = Color(0xFF36343B)
)
private val TakoLightColorScheme = lightColorScheme(
    primary = Color(0xFF825ED0), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFE3D4FF), onPrimaryContainer = Color(0xFF2A0D52),
    secondary = Color(0xFF6B4FA3), onSecondary = Color(0xFFFFFFFF), secondaryContainer = Color(0xFFEBDCFF), onSecondaryContainer = Color(0xFF24004F),
    tertiary = Color(0xFFA167D9), onTertiary = Color(0xFFFFFFFF), tertiaryContainer = Color(0xFFF0D9FF), onTertiaryContainer = Color(0xFF330055),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8F5FA), // 淡紫白
    onBackground = Color(0xFF1A1625),
    surface = Color(0xFFF8F5FA), onSurface = Color(0xFF1A1625),
    surfaceVariant = Color(0xFFE9E4ED), onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E), outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000), // ✅
    inverseSurface = Color(0xFF313033), inverseOnSurface = Color(0xFFF4EFF4), inversePrimary = Color(0xFF9D7CD8),
    surfaceDim = Color(0xFFDBD9E0), surfaceBright = Color(0xFFFFFBFE),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF5F3F7),
    surfaceContainer = Color(0xFFEFEDF1), surfaceContainerHigh = Color(0xFFE9E7EB), surfaceContainerHighest = Color(0xFFE3E1E5)
)
// ============================================================================
// 2. 核心算法: HCT Color Space Functions (Google Material Utilities)
// ============================================================================
// ============================================================================
// 2. 核心算法: HCT Color Space Functions (最终修复版)
// ============================================================================

// 1. 扩展函数：将 Color 转为 HCT (Hue, Chroma, Tone)
private fun Color.toHct(): Triple<Float, Float, Float> {
    // 1. RGB (0-1) 转 线性 RGB
    val r = red.toLinear()
    val g = green.toLinear()
    val b = blue.toLinear()

    // 2. 线性 RGB 转 XYZ
    // 🔥 修复点 1：XYZ 标准空间通常基于 0-100 的范围，而 RGB 是 0-1。
    // 这里的转换矩阵算出来的是 0-1 范围的 XYZ，所以必须 * 100，否则算出来的 L (亮度) 永远接近 0。
    val x = (r * 0.4124564f + g * 0.3575761f + b * 0.1804375f) * 100f
    val y = (r * 0.2126729f + g * 0.7151522f + b * 0.0721750f) * 100f
    val z = (r * 0.0193339f + g * 0.1191920f + b * 0.9503041f) * 100f

    // 3. XYZ 转 Lab
    // 这里的 reference white (95.047, 100, 108.883) 对应 D65 光源
    val l = 116f * labF(y / 100f) - 16f
    val a = 500f * (labF(x / 95.047f) - labF(y / 100f))
    val bLab = 200f * (labF(y / 100f) - labF(z / 108.883f))

    // 4. Lab 转 HCT (Hue, Chroma)
    val hue = Math.toDegrees(kotlin.math.atan2(bLab.toDouble(), a.toDouble())).toFloat()
    val hueNormalized = if (hue < 0) hue + 360f else hue
    val chroma = kotlin.math.sqrt(a * a + bLab * bLab)

    // Tone 直接就是 Lab 的 L
    return Triple(hueNormalized, chroma, l)
}

// 2. 内部函数：HCT 转 原始 RGB (可能包含越界值)
private fun hctToRgbRaw(h: Float, c: Float, t: Float): FloatArray {
    // 1. HCT 转 Lab
    val hRad = Math.toRadians(h.toDouble())
    val a = (c * kotlin.math.cos(hRad)).toFloat()
    val b = (c * kotlin.math.sin(hRad)).toFloat()
    val l = t

    // 2. Lab 转 XYZ
    val fy = (l + 16f) / 116f
    val fx = a / 500f + fy
    val fz = fy - b / 200f

    // 这里算出来的是 0-100 范围的 XYZ
    val x = 95.047f * labFInv(fx)
    val y = 100f * labFInv(fy)
    val z = 108.883f * labFInv(fz)

    // 3. XYZ 转 线性 RGB (注意这里除以 100 归一化到 0-1)
    val rLinear = (x * 3.2404542f - y * 1.5371385f - z * 0.4985314f) / 100f
    val gLinear = (-x * 0.9692660f + y * 1.8760108f + z * 0.0415560f) / 100f
    val bLinear = (x * 0.0556434f - y * 0.2040259f + z * 1.0572252f) / 100f

    return floatArrayOf(rLinear, gLinear, bLinear)
}

// 3. 检查 RGB 是否在 sRGB 色域内 (允许极小误差)
private fun isRgbInGamut(rgb: FloatArray): Boolean {
    val epsilon = 0.0001f
    // 只需要检查线性值是否在 0-1 之间即可，不需要先转 Gamma
    return (rgb[0] >= -epsilon && rgb[0] <= 1.0f + epsilon) &&
            (rgb[1] >= -epsilon && rgb[1] <= 1.0f + epsilon) &&
            (rgb[2] >= -epsilon && rgb[2] <= 1.0f + epsilon)
}

// 4. 主函数：HCT 转 Color (带色域映射 Gamut Mapping)
// 解决 0665DC 这种高饱和蓝色的关键
private fun hctToColor(h: Float, c: Float, t: Float): Color {
    // 步骤 A: 尝试直接转换
    val rawRgb = hctToRgbRaw(h, c, t)

    if (isRgbInGamut(rawRgb)) {
        return Color(
            red = rawRgb[0].fromLinear().coerceIn(0f, 1f),
            green = rawRgb[1].fromLinear().coerceIn(0f, 1f),
            blue = rawRgb[2].fromLinear().coerceIn(0f, 1f)
        )
    }

    // 步骤 B: 如果溢出，二分查找最佳 Chroma
    // 保持 Hue 和 Tone 不变，降低 Chroma 直到颜色能显示
    var low = 0f
    var high = c
    var bestChroma = 0f

    // 6次迭代足以达到肉眼无法区分的精度
    for (i in 0..6) {
        val mid = (low + high) / 2
        if (isRgbInGamut(hctToRgbRaw(h, mid, t))) {
            bestChroma = mid
            low = mid
        } else {
            high = mid
        }
    }

    val finalRgb = hctToRgbRaw(h, bestChroma, t)
    return Color(
        red = finalRgb[0].fromLinear().coerceIn(0f, 1f),
        green = finalRgb[1].fromLinear().coerceIn(0f, 1f),
        blue = finalRgb[2].fromLinear().coerceIn(0f, 1f)
    )
}

// 5. 数学辅助函数 (Gamma 校正与 Lab 函数)
private fun Float.toLinear(): Float =
    if (this <= 0.04045f) this / 12.92f else ((this + 0.055f) / 1.055f).pow(2.4f)

private fun Float.fromLinear(): Float =
    if (this <= 0.0031308f) this * 12.92f else 1.055f * this.pow(1f / 2.4f) - 0.055f

private fun labF(t: Float): Float {
    val delta = 6f / 29f
    return if (t > delta * delta * delta) t.pow(1f / 3f) else t / (3f * delta * delta) + 4f / 29f
}

private fun labFInv(t: Float): Float {
    val delta = 6f / 29f
    return if (t > delta) t * t * t else 3f * delta * delta * (t - 4f / 29f)
}

// ============================================================================
// 修复后的 scheme 生成逻辑
// ============================================================================
// ============================================================================
// 3. 最终方案：智能调整 Tone 值的生成逻辑 (拒绝惨白，保留色彩)
// ============================================================================
// ============================================================================
// 4. 终极方案：高保真色彩模式 (拒绝粉色/发白，还原纯正色彩)
// ============================================================================
// ============================================================================
// 5. 最终完美版：自适应亮度方案 (修复红色变橙、黄色变暗的问题)
// ============================================================================
// ============================================================================
// 6. 最终核弹版：原生直出方案 (What You See Is What You Get)
// ============================================================================
private fun generateDynamicColorScheme(seedColor: Color, isDark: Boolean): ColorScheme {
    val (hue, chroma, tone) = seedColor.toHct()

    // --- 1. 智能主色处理 (防止色彩偏移) ---
    val primaryColor = if (isDark) {
        if (tone < 40f) {
            val safeTone = 55f
            hctToColor(hue, chroma.coerceAtLeast(48f), safeTone)
        } else {
            seedColor
        }
    } else {
        if (tone < 50f) seedColor else hctToColor(hue, chroma.coerceAtLeast(48f), 40f)
    }

    val (_, _, primaryToneActual) = primaryColor.toHct()
    val onPrimaryColor = if (primaryToneActual > 60f) Color.Black else Color.White

    // --- 2. 背景与容器色系 (关键：解决 Card 颜色问题) ---
    // 给背景一点点主色的倾向 (Tint)，让界面不那么死板，但要很低饱和度
    val bgChroma = if (chroma < 5f) 0f else chroma * 0.06f // 稍微增加一点色相倾向

    if (isDark) {
        // [深色模式] Surface 逻辑：Tone 值越低越黑
        // 传统的 Surface 是 6，Elevated Card 需要比背景亮
        return darkColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            primaryContainer = hctToColor(hue, chroma, 30f),
            onPrimaryContainer = hctToColor(hue, chroma, 90f),

            secondary = hctToColor(hue, chroma, 50f), // 稍微降低饱和度或改变色相可以做复色，这里保持同色系
            onSecondary = Color.White,
            secondaryContainer = hctToColor(hue, chroma, 30f),
            onSecondaryContainer = hctToColor(hue, chroma, 90f),

            tertiary = hctToColor((hue + 60f) % 360f, chroma * 0.7f, 60f),
            onTertiary = Color.White,
            tertiaryContainer = hctToColor((hue + 60f) % 360f, chroma * 0.7f, 30f),
            onTertiaryContainer = hctToColor((hue + 60f) % 360f, chroma * 0.7f, 90f),

            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),

            // --- 核心修复：完整的 Surface 定义 ---
            background = hctToColor(hue, bgChroma, 6f),      // 极黑背景
            onBackground = hctToColor(hue, bgChroma, 90f),

            surface = hctToColor(hue, bgChroma, 6f),         // 与背景一致
            onSurface = hctToColor(hue, bgChroma, 90f),

            // FilledCard 默认用这个 (Tone 30 -> 明显的卡片感)
            surfaceVariant = hctToColor(hue, bgChroma, 30f),
            onSurfaceVariant = hctToColor(hue, bgChroma, 80f),

            // 边框
            outline = hctToColor(hue, bgChroma, 60f),
            outlineVariant = hctToColor(hue, bgChroma, 30f),

            // --- 容器系列 (ElevatedCard, BottomSheet 用这些) ---
            // 越 Low 越接近背景，越 High 越亮
            surfaceContainerLowest = hctToColor(hue, bgChroma, 4f),
            surfaceContainerLow = hctToColor(hue, bgChroma, 10f),  // ElevatedCard 默认
            surfaceContainer = hctToColor(hue, bgChroma, 12f),
            surfaceContainerHigh = hctToColor(hue, bgChroma, 17f),
            surfaceContainerHighest = hctToColor(hue, bgChroma, 22f),

            inverseSurface = hctToColor(hue, bgChroma, 90f),
            inverseOnSurface = hctToColor(hue, bgChroma, 20f),
            inversePrimary = hctToColor(hue, chroma, 80f),
            scrim = Color.Black
        )
    } else {
        // [浅色模式] Surface 逻辑：Tone 值越高越白
        return lightColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            primaryContainer = hctToColor(hue, chroma, 90f),
            onPrimaryContainer = hctToColor(hue, chroma, 10f),

            secondary = hctToColor(hue, chroma, 40f),
            onSecondary = Color.White,
            secondaryContainer = hctToColor(hue, chroma, 90f),
            onSecondaryContainer = hctToColor(hue, chroma, 10f),

            tertiary = hctToColor((hue + 60f) % 360f, chroma * 0.7f, 40f),
            onTertiary = Color.White,
            tertiaryContainer = hctToColor((hue + 60f) % 360f, chroma * 0.7f, 90f),
            onTertiaryContainer = hctToColor((hue + 60f) % 360f, chroma * 0.7f, 10f),

            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),

            // --- 核心修复：完整的 Surface 定义 ---
            background = hctToColor(hue, bgChroma, 98f),     // 极亮背景
            onBackground = hctToColor(hue, bgChroma, 10f),

            surface = hctToColor(hue, bgChroma, 98f),
            onSurface = hctToColor(hue, bgChroma, 10f),

            // FilledCard 默认用这个 (Tone 90 -> 灰调卡片)
            surfaceVariant = hctToColor(hue, bgChroma, 90f),
            onSurfaceVariant = hctToColor(hue, bgChroma, 30f),

            outline = hctToColor(hue, bgChroma, 50f),
            outlineVariant = hctToColor(hue, bgChroma, 80f),

            // --- 容器系列 ---
            // 浅色模式下，Lowest 是纯白，High 稍微变灰
            surfaceContainerLowest = hctToColor(hue, bgChroma, 100f),
            surfaceContainerLow = hctToColor(hue, bgChroma, 96f), // ElevatedCard 默认
            surfaceContainer = hctToColor(hue, bgChroma, 94f),
            surfaceContainerHigh = hctToColor(hue, bgChroma, 92f),
            surfaceContainerHighest = hctToColor(hue, bgChroma, 90f),

            inverseSurface = hctToColor(hue, bgChroma, 20f),
            inverseOnSurface = hctToColor(hue, bgChroma, 95f),
            inversePrimary = hctToColor(hue, chroma, 80f),
            scrim = Color.Black
        )
    }
}

@Composable
fun MyComposeApplicationTheme(
    themeState: ThemeState,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val useDarkTheme = when (themeState.selectedModeIndex) {
        0 -> isSystemInDarkTheme()
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    
    // [Debug Log] Print current theme state on every recomposition
    // Note: You can remove this SideEffect if the logs are flooding the output
    SideEffect {
        LogCatcher.d("ThemeDebug_Apply", "Applying theme... Monet=${themeState.isMonetEnabled}, Custom=${themeState.isCustomTheme}, Mode=$useDarkTheme, CustomColor=${themeState.customColor.value}")
    }

    val colorScheme = when {
        // 1. Monet (System wallpaper color extraction)
        themeState.isMonetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            LogCatcher.i("ThemeDebug_Branch", ">>> Hit branch: Monet (System wallpaper color extraction)")
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        // 2. Custom color
        themeState.isCustomTheme -> {
            LogCatcher.i("ThemeDebug_Branch", ">>> Hit branch: Custom (User-defined), Color: ${themeState.customColor.value}")
            generateDynamicColorScheme(themeState.customColor, useDarkTheme)
        }

        // 3. Preset theme list
        else -> {
            LogCatcher.i("ThemeDebug_Branch", ">>> Hit branch: Preset (Preset theme), Index: ${themeState.selectedThemeIndex}")
            when (themeState.selectedThemeIndex) {
                // 请确保这些变量在文件顶部有定义
                0 -> if (useDarkTheme) CatppuccinDarkColorScheme else CatppuccinLightColorScheme
                1 -> if (useDarkTheme) AppleDarkColorScheme else AppleLightColorScheme
                2 -> if (useDarkTheme) LavenderDarkColorScheme else LavenderLightColorScheme
                3 -> if (useDarkTheme) MidnightDarkColorScheme else MidnightLightColorScheme
                4 -> if (useDarkTheme) NordDarkColorScheme else NordLightColorScheme
                5 -> if (useDarkTheme) StrawberryDarkColorScheme else StrawberryLightColorScheme
                6 -> if (useDarkTheme) TakoDarkColorScheme else TakoLightColorScheme
                // 默认兜底 (保留原有的 DarkColorScheme / LightColorScheme)
                else -> if (useDarkTheme) DarkColorScheme else LightColorScheme
            }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // 确保你有定义 Typography
        content = content
    )
}