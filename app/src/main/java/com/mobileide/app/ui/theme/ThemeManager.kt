package com.mobileide.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// IDE colour palette (syntax + chrome)
// Used by legacy screens (EditorSettingsScreen, SettingsScreen, etc.)
// ─────────────────────────────────────────────────────────────────────────────

data class IDEColorPalette(
    val name: String,
    val background: Color, val surface: Color, val surfaceVariant: Color,
    val outline: Color, val onBackground: Color, val onSurface: Color,
    val primary: Color, val secondary: Color, val tertiary: Color,
    val syntaxKeyword: Color, val syntaxString: Color, val syntaxComment: Color,
    val syntaxNumber: Color, val syntaxFunction: Color, val syntaxAnnotation: Color,
    val syntaxType: Color, val syntaxPlain: Color,
)

object Themes {
    val CATPPUCCIN = IDEColorPalette("Catppuccin Mocha",
        Color(0xFF1E1E2E), Color(0xFF2A2A3E), Color(0xFF313145), Color(0xFF45475A),
        Color(0xFFCDD6F4), Color(0xFFBAC2DE),
        Color(0xFF82AAFF), Color(0xFFC3E88D), Color(0xFFFF9CAC),
        Color(0xFFCBA6F7), Color(0xFFA6E3A1), Color(0xFF6C7086),
        Color(0xFFFAB387), Color(0xFF89B4FA), Color(0xFFF38BA8),
        Color(0xFF89DCEB), Color(0xFFCDD6F4))

    val DRACULA = IDEColorPalette("Dracula",
        Color(0xFF282A36), Color(0xFF323443), Color(0xFF3A3C4E), Color(0xFF44475A),
        Color(0xFFF8F8F2), Color(0xFFBDBDBD),
        Color(0xFF6272A4), Color(0xFF50FA7B), Color(0xFFFF5555),
        Color(0xFFFF79C6), Color(0xFFF1FA8C), Color(0xFF6272A4),
        Color(0xFFBD93F9), Color(0xFF50FA7B), Color(0xFFFF5555),
        Color(0xFF8BE9FD), Color(0xFFF8F8F2))

    val ONE_DARK = IDEColorPalette("One Dark",
        Color(0xFF21252B), Color(0xFF282C34), Color(0xFF2F343D), Color(0xFF3E4452),
        Color(0xFFABB2BF), Color(0xFF7F848E),
        Color(0xFF528BFF), Color(0xFF98C379), Color(0xFFE06C75),
        Color(0xFFC678DD), Color(0xFF98C379), Color(0xFF5C6370),
        Color(0xFFD19A66), Color(0xFF61AFEF), Color(0xFFE06C75),
        Color(0xFF56B6C2), Color(0xFFABB2BF))

    val MONOKAI = IDEColorPalette("Monokai",
        Color(0xFF272822), Color(0xFF2E2E2A), Color(0xFF343430), Color(0xFF49483E),
        Color(0xFFF8F8F2), Color(0xFF888980),
        Color(0xFF66D9E8), Color(0xFFA6E22E), Color(0xFFF92672),
        Color(0xFFF92672), Color(0xFFE6DB74), Color(0xFF75715E),
        Color(0xFFAE81FF), Color(0xFFA6E22E), Color(0xFFF92672),
        Color(0xFF66D9E8), Color(0xFFF8F8F2))

    val NORD = IDEColorPalette("Nord",
        Color(0xFF2E3440), Color(0xFF3B4252), Color(0xFF434C5E), Color(0xFF4C566A),
        Color(0xFFECEFF4), Color(0xFFD8DEE9),
        Color(0xFF88C0D0), Color(0xFFA3BE8C), Color(0xFFBF616A),
        Color(0xFF81A1C1), Color(0xFFA3BE8C), Color(0xFF616E88),
        Color(0xFFB48EAD), Color(0xFF88C0D0), Color(0xFFBF616A),
        Color(0xFF8FBCBB), Color(0xFFECEFF4))

    val SOLARIZED = IDEColorPalette("Solarized Dark",
        Color(0xFF002B36), Color(0xFF073642), Color(0xFF0D3F4F), Color(0xFF586E75),
        Color(0xFF839496), Color(0xFF657B83),
        Color(0xFF268BD2), Color(0xFF859900), Color(0xFFDC322F),
        Color(0xFF859900), Color(0xFF2AA198), Color(0xFF586E75),
        Color(0xFFD33682), Color(0xFF268BD2), Color(0xFFCB4B16),
        Color(0xFF6C71C4), Color(0xFF839496))

    val GITHUB_DARK = IDEColorPalette("GitHub Dark",
        Color(0xFF0D1117), Color(0xFF161B22), Color(0xFF21262D), Color(0xFF30363D),
        Color(0xFFE6EDF3), Color(0xFF8B949E),
        Color(0xFF58A6FF), Color(0xFF3FB950), Color(0xFFF85149),
        Color(0xFFFF7B72), Color(0xFFA5D6FF), Color(0xFF8B949E),
        Color(0xFFF0883E), Color(0xFFD2A8FF), Color(0xFFF85149),
        Color(0xFF79C0FF), Color(0xFFE6EDF3))

    val GITHUB_LIGHT = IDEColorPalette("GitHub Light",
        Color(0xFFFFFFFF), Color(0xFFF6F8FA), Color(0xFFEAEEF2), Color(0xFFD0D7DE),
        Color(0xFF24292F), Color(0xFF57606A),
        Color(0xFF0969DA), Color(0xFF116329), Color(0xFFCF222E),
        Color(0xFFCF222E), Color(0xFF0A3069), Color(0xFF6E7781),
        Color(0xFF0550AE), Color(0xFF8250DF), Color(0xFFCF222E),
        Color(0xFF0550AE), Color(0xFF24292F))

    val QUIET_LIGHT = IDEColorPalette("Quiet Light",
        Color(0xFFF5F5F5), Color(0xFFF0F0F0), Color(0xFFE8E8E8), Color(0xFFCCCCCC),
        Color(0xFF333333), Color(0xFF777777),
        Color(0xFF4B83CD), Color(0xFF448C27), Color(0xFFAA3731),
        Color(0xFF4B83CD), Color(0xFF448C27), Color(0xFFAAAAAA),
        Color(0xFFAB6526), Color(0xFFAA3731), Color(0xFF7A3E9D),
        Color(0xFF7A3E9D), Color(0xFF333333))

    val ALL = listOf(CATPPUCCIN, DRACULA, ONE_DARK, MONOKAI, NORD, SOLARIZED,
                     GITHUB_DARK, GITHUB_LIGHT, QUIET_LIGHT)
    fun byName(name: String) = ALL.firstOrNull { it.name == name } ?: CATPPUCCIN
}

// Non-Composable accessor (for utils that don't have Compose context)
object ActiveTheme {
    private var _p = Themes.CATPPUCCIN
    fun set(p: IDEColorPalette) { _p = p }
    fun get() = _p
    val syntaxKeyword    get() = _p.syntaxKeyword
    val syntaxString     get() = _p.syntaxString
    val syntaxComment    get() = _p.syntaxComment
    val syntaxNumber     get() = _p.syntaxNumber
    val syntaxFunction   get() = _p.syntaxFunction
    val syntaxAnnotation get() = _p.syntaxAnnotation
    val syntaxType       get() = _p.syntaxType
    val syntaxPlain      get() = _p.syntaxPlain
}
