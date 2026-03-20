package com.mobileide.app.editor

import com.mobileide.app.ui.theme.amoledM3Theme
import com.mobileide.app.ui.theme.currentM3Theme

// ── Asset paths ───────────────────────────────────────────────────────────────
const val TEXTMATE_PREFIX        = "textmate/"
const val TEXTMATE_AMOLED_PREFIX = "textmate/black/"
const val DARCULA_THEME          = "darcula.json"
const val QUIETLIGHT_THEME       = "quietlight.json"
const val LANGUAGES_FILE         = "languages.json"
const val KEYWORDS_FILE          = "keywords.json"

// ── Base-theme paths ──────────────────────────────────────────────────────────
/** Dark-mode base theme (also used as AMOLED fallback for token colours). */
const val BASE_DARK_THEME        = "textmate/darcula.json"
/** Light-mode base theme. */
const val BASE_LIGHT_THEME       = "textmate/quietlight.json"
/** AMOLED pure-black base theme. */
const val BASE_AMOLED_THEME      = "textmate/black/darcula.json"

// ── Available editor fonts (assets/fonts/) ────────────────────────────────────
val AVAILABLE_FONTS = listOf(
    "JetBrains Mono"  to "fonts/JetBrainsMono-Regular.ttf",
    "Fira Code"       to "fonts/FiraCode-Regular.ttf",
    "Roboto"          to "fonts/Roboto-Regular.ttf",
    "Default"         to "fonts/Default.ttf",
)
val DEFAULT_FONT_PATH = "fonts/JetBrainsMono-Regular.ttf"

// ── Cache key ─────────────────────────────────────────────────────────────────
/**
 * Stable cache key that encodes every input that would change the resulting
 * [io.github.rosemoe.sora.langs.textmate.TextMateColorScheme].
 */
fun buildEditorCacheKey(isDark: Boolean, isAmoled: Boolean): String = buildString {
    append(if (isAmoled) "amoled" else if (isDark) "dark" else "light")
    append('_')
    append(currentM3Theme.value?.darkTokenColors?.hashCode())
    append('_')
    append(currentM3Theme.value?.lightTokenColors?.hashCode())
}
