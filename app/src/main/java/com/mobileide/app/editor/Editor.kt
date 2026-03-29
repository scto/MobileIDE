package com.mobileide.app.editor

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import com.mobileide.app.editor.intelligent.IntelligentFeatureRegistry
import com.mobileide.app.ui.theme.currentM3Theme
import com.mobileide.app.utils.EditorSettings
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MobileIDE's custom [CodeEditor] subclass.
 *
 * Responsibilities:
 * - Apply Material3 theme colours to the Sora editor chrome via [setThemeColors].
 * - Apply user [EditorSettings] (tab size, word-wrap, font, …) via [applyEditorSettings].
 * - Load / cache custom fonts via [FontCache].
 * - Register intelligent editing features (auto-close tag, bullet continuation).
 * - Expose helpers for text selection and custom text-action buttons.
 */
@Suppress("NOTHING_TO_INLINE")
class Editor : CodeEditor {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val scope = CoroutineScope(Dispatchers.Default)

    /** Current line-ending mode (default LF). */
    var lineEnding: LineEnding = LineEnding.LF

    /** Whether to insert a trailing newline on save. */
    var insertFinalNewline: Boolean = false

    /** Whether to strip trailing whitespace on save. */
    var trimTrailingWhitespace: Boolean = false

    init {
        applyFont(context)
        getComponent(EditorAutoCompletion::class.java).setEnabledAnimation(true)

        // Wire up intelligent-feature events
        IntelligentFeatureRegistry.allFeatures.forEach { feature ->
            // Key and content events are handled via subscribeEvent in feature impls
            // (See IntelligentFeature for the hook API)
        }
    }

    // ── Theme colours ─────────────────────────────────────────────────────────

    /**
     * Apply Material3-derived colours to the Sora editor chrome.
     *
     * This is called every time the active Material3 theme or dark-mode state
     * changes. It also picks up any [currentM3Theme] editor-colour overrides
     * that were defined in a user-installed theme JSON.
     *
     * @param isDarkMode            Whether dark mode is active.
     * @param editorBackground      Whole editor background (ARGB int).
     * @param surfaceContainer      Used for popup / completion-window backgrounds.
     * @param surface               Secondary surface (hover, diagnostic tooltips).
     * @param onSurface             Text / icon colours on surface.
     * @param highSurfaceContainer  Highlighted item in the completion window.
     * @param colorPrimary          Primary accent: delimiters, highlighted text.
     * @param colorPrimaryContainer (reserved for future use)
     * @param colorSecondary        (reserved for future use)
     * @param secondaryContainer    (reserved for future use)
     * @param selectionBg           Selection / match highlight background.
     * @param handleColor           Selection handles.
     * @param gutterColor           Line-number strip background.
     * @param currentLine           Current-line highlight.
     * @param dividerColor          Line divider and popup borders.
     * @param errorColor            Error underline / problem indicator.
     */
    fun setThemeColors(
        isDarkMode:           Boolean,
        editorBackground:     Int,
        surfaceContainer:     Int,
        surface:              Int,
        onSurface:            Int,
        highSurfaceContainer: Int,
        colorPrimary:         Int,
        colorPrimaryContainer:Int,
        colorSecondary:       Int,
        secondaryContainer:   Int,
        selectionBg:          Int,
        handleColor:          Int,
        gutterColor:          Int,
        currentLine:          Int,
        dividerColor:         Int,
        errorColor:           Int,
    ) {
        updateColors { scheme ->
            with(scheme) {
                fun EditorColorScheme.setColors(color: Int, vararg ids: Int) =
                    ids.forEach { setColor(it, color) }

                setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE, Color.TRANSPARENT)

                setColors(editorBackground,
                    EditorColorScheme.WHOLE_BACKGROUND)

                setColors(surfaceContainer,
                    EditorColorScheme.COMPLETION_WND_BACKGROUND,
                    EditorColorScheme.DIAGNOSTIC_TOOLTIP_BACKGROUND,
                    EditorColorScheme.SIGNATURE_BACKGROUND,
                    EditorColorScheme.LINE_NUMBER_PANEL)

                setColors(highSurfaceContainer,
                    EditorColorScheme.COMPLETION_WND_ITEM_CURRENT)

                setColors(dividerColor,
                    EditorColorScheme.COMPLETION_WND_CORNER,
                    EditorColorScheme.LINE_DIVIDER)

                setColors(onSurface,
                    EditorColorScheme.LINE_NUMBER,
                    EditorColorScheme.LINE_NUMBER_CURRENT,
                    EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY,
                    EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY,
                    EditorColorScheme.DIAGNOSTIC_TOOLTIP_BRIEF_MSG,
                    EditorColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG,
                    EditorColorScheme.SIGNATURE_TEXT_NORMAL)

                setColors(handleColor,
                    EditorColorScheme.SELECTION_HANDLE)

                setColors(selectionBg,
                    EditorColorScheme.SELECTION_INSERT,
                    EditorColorScheme.SELECTED_TEXT_BACKGROUND,
                    EditorColorScheme.MATCHED_TEXT_BACKGROUND)

                setColors(colorPrimary,
                    EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND,
                    EditorColorScheme.SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER,
                    EditorColorScheme.DIAGNOSTIC_TOOLTIP_ACTION)

                setColors(alpha(onSurface, 0.6f), EditorColorScheme.BLOCK_LINE_CURRENT)
                setColors(alpha(onSurface, 0.4f),
                    EditorColorScheme.NON_PRINTABLE_CHAR,
                    EditorColorScheme.BLOCK_LINE)
                setColors(alpha(onSurface, 0.3f), EditorColorScheme.SCROLL_BAR_THUMB)
                setColors(alpha(onSurface, 0.2f), EditorColorScheme.SCROLL_BAR_THUMB_PRESSED)

                setColors(currentLine, EditorColorScheme.CURRENT_LINE)
                setColors(gutterColor, EditorColorScheme.LINE_NUMBER_BACKGROUND)
                setColors(errorColor,  EditorColorScheme.PROBLEM_ERROR)

                // Apply any per-colour overrides from the user's installed M3 theme
                val editorColors = if (isDarkMode)
                    currentM3Theme.value?.darkEditorColors
                else
                    currentM3Theme.value?.lightEditorColors

                editorColors?.forEach { (key, color) -> setColor(key, color) }
            }
        }
    }

    // ── Editor settings ───────────────────────────────────────────────────────

    /**
     * Apply [EditorSettings] to this editor instance.
     * Safe to call on the main thread at any time.
     */
    fun applyEditorSettings(settings: EditorSettings) {
        tabWidth                  = settings.tabSize
        props.deleteMultiSpaces   = settings.tabSize
        isLineNumberEnabled       = settings.showLineNumbers
        setTextSize(settings.fontSize)
        isWordwrap                = settings.wordWrap
        props.autoIndent          = settings.autoIndent
        props.stickyScroll        = settings.stickyScroll
        props.symbolPairAutoCompletion = settings.bracketAutoClose
        getComponent(EditorAutoCompletion::class.java)?.isEnabled = settings.autoComplete
        lineSpacingMultiplier     = 1.2f   // comfortable default; expose if needed
        lineNumberMarginLeft      = 9f
    }

    // ── Language ──────────────────────────────────────────────────────────────

    /**
     * Set the TextMate language for [textmateScope].
     * Adds keyword completions if [KeywordManager] has them for this scope.
     */
    suspend fun setLanguage(textmateScope: String) {
        val language = LanguageManager.createLanguage(context, textmateScope)
        // Keyword completions
        KeywordManager.getKeywords(textmateScope)?.let { kw ->
            language.setCompleterKeywords(kw.toTypedArray())
        }
        withContext(Dispatchers.Main) { setEditorLanguage(language) }
    }

    // ── Font ──────────────────────────────────────────────────────────────────

    /**
     * Apply the editor font.  Falls back to [Typeface.MONOSPACE] if no custom
     * font is configured or the asset cannot be loaded.
     *
     * Extend this when a font-settings feature is added:
     * ```kotlin
     * applyFont(context, fontPath = prefs.fontPath, isAsset = prefs.isFontAsset)
     * ```
     */
    fun applyFont(
        context: Context,
        fontPath: String = DEFAULT_FONT_PATH,
        isAsset: Boolean = true,
    ) {
        runCatching {
            val resolved = fontPath.ifBlank { DEFAULT_FONT_PATH }
            val font = FontCache.getFont(context, resolved, isAsset) ?: Typeface.MONOSPACE
            typefaceText       = font
            typefaceLineNumber = font
        }.onFailure { it.printStackTrace() }
    }

    /** Apply font from [settings]. Called when editor settings change. */
    fun applyFontFromSettings(context: Context, settings: com.mobileide.app.utils.EditorSettings) {
        applyFont(context, settings.fontPath, isAsset = true)
    }

    // ── Text actions ──────────────────────────────────────────────────────────

    // TextActionItem API is not available in Sora 0.23.4
    // registerTextAction / unregisterTextAction removed

    // ── Selection ─────────────────────────────────────────────────────────────

    /** Return the currently selected text, or null if nothing is selected. */
    fun getSelectedText(): String? {
        if (!isTextSelected) return null
        return text.substring(cursorRange.startIndex, cursorRange.endIndex)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun release() {
        scope.cancel()
        super.release()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun alpha(color: Int, factor: Float): Int {
        val a = ((Color.alpha(color) * factor).toInt()).coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * Apply [block] to the current scheme immediately, then rebuild the full
     * TextMate scheme on IO and re-apply on Main.
     *
     * This two-phase approach avoids a visible flash: the colours are applied
     * synchronously for instant feedback, then the fully rebuilt TextMate scheme
     * (with correct token colours) replaces it asynchronously.
     */
    private fun updateColors(block: (EditorColorScheme) -> Unit) {
        // Phase 1: immediate synchronous patch
        block(colorScheme)

        // Phase 2: async rebuild with full TextMate token colours
        scope.launch(Dispatchers.IO) {
            val isDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            val newScheme = EditorThemeManager.createColorScheme(context, isDark)
            withContext(Dispatchers.Main) {
                colorScheme = newScheme
                block(newScheme)
            }
        }
    }

    // DEFAULT_FONT_PATH is defined in TextMateConstants.kt
}
