package com.mobileide.app.editor

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import com.mobileide.app.ui.theme.currentM3Theme
import com.mobileide.app.utils.EditorSettings
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import com.mobileide.app.ui.theme.EditorColor
import com.mobileide.app.ui.theme.applyTo
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MobileIDE's [CodeEditor] subclass.
 *
 * Applies Material3 colours + installed M3-theme token overrides to every
 * available [EditorColorScheme] slot in a single [setThemeColors] call.
 */
class Editor @JvmOverloads constructor(
    context: Context,
) : CodeEditor(context) {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        runCatching { scope.cancel() }
    }

    // ── Theme colours ─────────────────────────────────────────────────────────

    /**
     * Apply all available [EditorColorScheme] slots from Material3 colours,
     * then overlay any per-theme [EditorColor] overrides from the active M3 theme.
     *
     * Parameters map to M3 colour roles; alpha helpers are used for derived slots.
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
        warningColor:         Int   = Color.parseColor("#E5C07B"),
        scrollThumb:          Int   = alpha(onSurface, 0.3f),
        scrollThumbPressed:   Int   = alpha(onSurface, 0.5f),
        inlayHintFg:          Int   = alpha(onSurface, 0.5f),
        inlayHintBg:          Int   = alpha(surfaceContainer, 0.7f),
        nonPrintable:         Int   = alpha(onSurface, 0.25f),
        matchedBg:            Int   = alpha(colorPrimary, 0.15f),
        snippetEditing:       Int   = alpha(colorPrimaryContainer, 0.6f),
        snippetRelated:       Int   = alpha(colorPrimaryContainer, 0.3f),
        snippetInactive:      Int   = alpha(surfaceContainer, 0.5f),
        textHighlight:        Int   = alpha(colorPrimary, 0.25f),
        textHighlightStrong:  Int   = alpha(colorPrimary, 0.4f),
    ) {
        // Rebuild scheme in background, apply on Main
        scope.launch(Dispatchers.IO) {
            updateColors { scheme ->
                fun s(color: Int, vararg ids: Int) =
                    ids.forEach { scheme.setColor(it, color) }
                val E = EditorColorScheme

                    // ── Transparency fix ──────────────────────────────────────
                    scheme.setColor(E.HIGHLIGHTED_DELIMITERS_UNDERLINE, Color.TRANSPARENT)

                    // ── Background ────────────────────────────────────────────
                    s(editorBackground, E.WHOLE_BACKGROUND)

                    // ── Gutter / line numbers ─────────────────────────────────
                    s(gutterColor, E.LINE_NUMBER_BACKGROUND)
                    s(surfaceContainer, E.LINE_NUMBER_PANEL)
                    s(alpha(onSurface, 0.5f), E.LINE_NUMBER)
                    s(colorPrimary, E.LINE_NUMBER_CURRENT)
                    s(dividerColor, E.LINE_DIVIDER)

                    // ── Current line ──────────────────────────────────────────
                    s(currentLine, E.CURRENT_LINE)

                    // ── Selection ─────────────────────────────────────────────
                    s(handleColor, E.SELECTION_HANDLE, E.SELECTION_INSERT)
                    s(selectionBg, E.SELECTED_TEXT_BACKGROUND, E.MATCHED_TEXT_BACKGROUND)

                    // ── Scrollbars ────────────────────────────────────────────
                    s(alpha(onSurface, 0.0f), E.SCROLL_BAR_TRACK)
                    s(scrollThumb, E.SCROLL_BAR_THUMB)
                    s(scrollThumbPressed, E.SCROLL_BAR_THUMB_PRESSED)

                    // ── Block / indent lines ──────────────────────────────────
                    s(alpha(colorPrimary, 0.6f), E.BLOCK_LINE_CURRENT)
                    s(alpha(onSurface, 0.2f), E.BLOCK_LINE)
                    s(nonPrintable, E.NON_PRINTABLE_CHAR)

                    // ── Autocomplete popup ────────────────────────────────────
                    s(surface, E.COMPLETION_WND_BACKGROUND, E.COMPLETION_WND_CORNER)
                    s(highSurfaceContainer, E.COMPLETION_WND_ITEM_CURRENT)
                    s(onSurface, E.COMPLETION_WND_TEXT_PRIMARY)
                    s(alpha(onSurface, 0.6f), E.COMPLETION_WND_TEXT_SECONDARY)

                    // ── Diagnostic tooltip ────────────────────────────────────
                    s(surface, E.DIAGNOSTIC_TOOLTIP_BACKGROUND)
                    s(onSurface, E.DIAGNOSTIC_TOOLTIP_BRIEF_MSG)
                    s(alpha(onSurface, 0.7f), E.DIAGNOSTIC_TOOLTIP_DETAILED_MSG)
                    s(colorPrimary, E.DIAGNOSTIC_TOOLTIP_ACTION)

                    // ── Signature help popup ──────────────────────────────────
                    s(surface, E.SIGNATURE_BACKGROUND)
                    s(onSurface, E.SIGNATURE_TEXT_NORMAL)
                    s(colorPrimary, E.SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER)

                    // ── Bracket / delimiter highlighting ──────────────────────
                    s(colorPrimary, E.HIGHLIGHTED_DELIMITERS_FOREGROUND)

                    // ── Problems ──────────────────────────────────────────────
                    s(errorColor, E.PROBLEM_ERROR)
                    s(warningColor, E.PROBLEM_WARNING)
                    s(colorSecondary, E.PROBLEM_TYPO)

                    // ── Inlay hints ───────────────────────────────────────────
                    // Inlay hint constants only exist in Sora ≥ 0.23.x
                    runCatching {
                        val fgField = EditorColorScheme::class.java.getField("TEXT_INLAY_HINT_FOREGROUND")
                        val bgField = EditorColorScheme::class.java.getField("TEXT_INLAY_HINT_BACKGROUND")
                        setColor(fgField.getInt(null), inlayHintFg)
                        setColor(bgField.getInt(null), inlayHintBg)
                    }

                    // ── Snippet highlights ────────────────────────────────────
                    runCatching {
                        s(snippetEditing,  EditorColorScheme::class.java.getField("SNIPPET_BACKGROUND_EDITING").getInt(null))
                        s(snippetRelated,  EditorColorScheme::class.java.getField("SNIPPET_BACKGROUND_RELATED").getInt(null))
                        s(snippetInactive, EditorColorScheme::class.java.getField("SNIPPET_BACKGROUND_INACTIVE").getInt(null))
                    }

                    // ── Text highlight ────────────────────────────────────────
                    runCatching {
                        s(textHighlight,       EditorColorScheme::class.java.getField("TEXT_HIGHLIGHT_BACKGROUND").getInt(null))
                        s(alpha(colorPrimary, 0.6f), EditorColorScheme::class.java.getField("TEXT_HIGHLIGHT_BORDER").getInt(null))
                        s(textHighlightStrong, EditorColorScheme::class.java.getField("TEXT_HIGHLIGHT_STRONG_BACKGROUND").getInt(null))
                        s(colorPrimary,        EditorColorScheme::class.java.getField("TEXT_HIGHLIGHT_STRONG_BORDER").getInt(null))
                    }

                    // ── Static span ───────────────────────────────────────────
                    runCatching {
                        s(alpha(colorPrimaryContainer, 0.5f), EditorColorScheme::class.java.getField("STATIC_SPAN_BACKGROUND").getInt(null))
                        s(colorPrimary, EditorColorScheme::class.java.getField("STATIC_SPAN_FOREGROUND").getInt(null))
                    }

                    // ── Side block line ───────────────────────────────────────
                    runCatching {
                        s(dividerColor, EditorColorScheme::class.java.getField("SIDE_BLOCK_LINE").getInt(null))
                    }

                    // ── Sticky scroll divider ─────────────────────────────────
                    runCatching {
                        s(dividerColor, EditorColorScheme::class.java.getField("STICKY_SCROLL_DIVIDER").getInt(null))
                    }

                    // ── Underline / strikethrough ─────────────────────────────
                    runCatching {
                        s(colorPrimary, EditorColorScheme::class.java.getField("UNDERLINE").getInt(null))
                        s(errorColor,   EditorColorScheme::class.java.getField("STRIKETHROUGH").getInt(null))
                    }

                    // ── Hard-wrap marker ──────────────────────────────────────
                    runCatching {
                        s(dividerColor, EditorColorScheme::class.java.getField("HARD_WRAP_MARKER").getInt(null))
                    }

                    // ── Current row border ────────────────────────────────────
                    runCatching {
                        s(colorSecondary, EditorColorScheme::class.java.getField("CURRENT_ROW_BORDER").getInt(null))
                    }

                    // ── Apply per-colour overrides from the installed M3 theme ─
                    val editorColors = if (isDarkMode)
                        currentM3Theme.value?.darkEditorColors
                    else
                        currentM3Theme.value?.lightEditorColors
                    editorColors?.applyTo(scheme)
            }
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun applyEditorSettings(settings: EditorSettings) {
        // ── Text ─────────────────────────────────────────────────────────────
        setTextSize(settings.fontSize)
        lineSpacingMultiplier              = settings.lineSpacing

        // ── Indentation ───────────────────────────────────────────────────────
        tabWidth                           = settings.tabSize
        props.deleteMultiSpaces            = settings.tabSize
        props.deleteEmptyLineFast          = settings.deleteMultiSpaces

        // ── Display ───────────────────────────────────────────────────────────
        isLineNumberEnabled                = settings.showLineNumbers
        isWordwrap                         = settings.wordWrap

        // ── Behaviour ─────────────────────────────────────────────────────────
        props.autoIndent                   = settings.autoIndent
        props.stickyScroll                 = settings.stickyScroll
        props.symbolPairAutoCompletion     = settings.bracketAutoClose
        props.useICULibToSelectWords       = true
        getComponent(EditorAutoCompletion::class.java)?.isEnabled = settings.autoComplete

        // ── Cursor animation (top-level CodeEditor property) ──────────────────
        isCursorAnimationEnabled           = settings.cursorAnimation

        // ── Whitespace rendering ──────────────────────────────────────────────
        nonPrintablePaintingFlags = if (settings.showWhitespace) {
            FLAG_DRAW_WHITESPACE_LEADING  or
            FLAG_DRAW_WHITESPACE_INNER    or
            FLAG_DRAW_WHITESPACE_TRAILING or
            FLAG_DRAW_TAB_SAME_AS_SPACE
        } else 0

        // ── Hardware keyboard ─────────────────────────────────────────────────
        isDisableSoftKbdIfHardKbdAvailable = true

        lineNumberMarginLeft = 9f
    }

    fun applyFontFromSettings(context: Context, settings: EditorSettings) =
        applyFont(context, settings.fontPath, isAsset = true)

    // ── Language ──────────────────────────────────────────────────────────────

    suspend fun setLanguage(textmateScope: String) {
        val language = LanguageManager.createLanguage(context, textmateScope)
        KeywordManager.getKeywords(textmateScope)?.let { kw ->
            language.setCompleterKeywords(kw.toTypedArray())
        }
        withContext(Dispatchers.Main) { setEditorLanguage(language) }
    }

    // ── Font ──────────────────────────────────────────────────────────────────

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

    // ── Selection ─────────────────────────────────────────────────────────────

    fun getSelectedText(): String? =
        if (isTextSelected) text.substring(cursorRange.startIndex, cursorRange.endIndex) else null

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun updateColors(block: (EditorColorScheme) -> Unit) {
        val scheme = colorScheme
        block(scheme)
        colorScheme = scheme
    }

    companion object {
        private fun alpha(color: Int, alpha: Float): Int {
            val a = (alpha * 255).toInt().coerceIn(0, 255)
            return (color and 0x00FFFFFF) or (a shl 24)
        }
    }
}
