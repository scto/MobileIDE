package com.mobileide.feature.editor

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Compose wrapper around Sora [CodeEditor] for the `:feature:editor` module.
 *
 * Integrates:
 * - MobileIDE's Material3 colour-scheme patching (all EditorColorScheme slots)
 * - TextMate grammar + theme setup (reuses the same assets as the main editor)
 * - IntelligentFeature dispatch for autoclose-tag and bullet continuation
 */
@Composable
fun FeatureSoraEditor(
    tab: FeatureEditorTab,
    settings: FeatureEditorSettings,
    onContentChanged: (String) -> Unit,
    onCursorChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark  = isSystemInDarkTheme()
    val cs      = MaterialTheme.colorScheme

    // Derive all colour ints
    val bgArgb          = cs.background.toArgb()
    val surfContArgb    = cs.surfaceContainer.toArgb()
    val surfArgb        = cs.surface.toArgb()
    val onSurfArgb      = cs.onSurface.toArgb()
    val hiContArgb      = cs.surfaceContainerHigh.toArgb()
    val primaryArgb     = cs.primary.toArgb()
    val primContArgb    = cs.primaryContainer.toArgb()
    val secArgb         = cs.secondary.toArgb()
    val secContArgb     = cs.secondaryContainer.toArgb()
    val selArgb         = cs.primary.copy(alpha = 0.25f).toArgb()
    val handleArgb      = cs.primary.toArgb()
    val gutterArgb      = cs.surfaceContainerLow.toArgb()
    val curLineArgb     = cs.surfaceContainerHigh.toArgb()
    val dividerArgb     = cs.outlineVariant.toArgb()
    val errorArgb       = cs.error.toArgb()
    val warningArgb     = cs.tertiary.toArgb()

    val scope = remember { CoroutineScope(Dispatchers.Main) }
    var initialized by remember { mutableStateOf(false) }

    val editor = remember {
        CodeEditor(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    fun applyColors() {
        patchColorScheme(
            editor          = editor,
            isDark          = isDark,
            bgArgb          = bgArgb,
            surfContArgb    = surfContArgb,
            surfArgb        = surfArgb,
            onSurfArgb      = onSurfArgb,
            hiContArgb      = hiContArgb,
            primaryArgb     = primaryArgb,
            primContArgb    = primContArgb,
            secArgb         = secArgb,
            gutterArgb      = gutterArgb,
            curLineArgb     = curLineArgb,
            dividerArgb     = dividerArgb,
            errorArgb       = errorArgb,
            warningArgb     = warningArgb,
            selArgb         = selArgb,
            handleArgb      = handleArgb,
        )
    }

    // ── Full init ─────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        // 1. Grammar registries (same assets as main editor)
        withContext(Dispatchers.IO) {
            runCatching {
                FileProviderRegistry.getInstance()
                    .addFileProvider(AssetsFileResolver(context.assets))
                GrammarRegistry.getInstance()
                    .loadGrammars("textmate/languages.json")
            }
        }
        // 2. Colour scheme
        withContext(Dispatchers.Main) {
            applyTextMateTheme(context, editor, isDark)
            applyColors()
        }
        // 3. Settings + font
        withContext(Dispatchers.Main) {
            applySettings(editor, settings)
            applyFont(context, editor, settings.fontPath)
        }
        // 4. Language
        withContext(Dispatchers.IO) {
            applyLanguage(editor, tab.language)
        }
        // 5. Content — last
        withContext(Dispatchers.Main) {
            editor.setText(tab.content)
            initialized = true
        }
    }

    // Re-apply on dark/light switch
    LaunchedEffect(isDark) {
        if (!initialized) return@LaunchedEffect
        withContext(Dispatchers.IO) { applyTextMateTheme(context, editor, isDark) }
        withContext(Dispatchers.Main) { applyColors() }
    }

    // Re-apply on language change
    LaunchedEffect(tab.language) {
        if (!initialized) return@LaunchedEffect
        withContext(Dispatchers.IO) { applyLanguage(editor, tab.language) }
    }

    // Re-apply on settings change
    LaunchedEffect(settings) {
        if (!initialized) return@LaunchedEffect
        withContext(Dispatchers.Main) {
            applySettings(editor, settings)
            applyFont(context, editor, settings.fontPath)
        }
    }

    // ── Events ────────────────────────────────────────────────────────────────
    val fileExt = tab.fileName.substringAfterLast('.', "")
    DisposableEffect(editor) {
        val unsubContent = editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
            onContentChanged(editor.text.toString())
        }
        val unsubCursor = editor.subscribeEvent(SelectionChangeEvent::class.java) { evt, _ ->
            onCursorChanged(evt.left.line + 1, evt.left.column + 1)
        }
        onDispose {
            runCatching { unsubContent.unsubscribe() }
            runCatching { unsubCursor.unsubscribe() }
            runCatching { editor.release() }
        }
    }

    // ── Content sync ──────────────────────────────────────────────────────────
    AndroidView(
        factory = { editor },
        update  = { ed ->
            if (initialized && tab.content != ed.text.toString()) {
                val l = runCatching { ed.cursor.leftLine }.getOrDefault(0)
                val c = runCatching { ed.cursor.leftColumn }.getOrDefault(0)
                ed.setText(tab.content)
                runCatching {
                    val ml = (ed.text.lineCount - 1).coerceAtLeast(0)
                    ed.setSelection(l.coerceIn(0, ml), c.coerceIn(0, ed.text.getColumnCount(l.coerceIn(0, ml))))
                }
            }
        },
        modifier = modifier,
    )
}

// ── Colour-scheme patcher ─────────────────────────────────────────────────────
private fun patchColorScheme(
    editor: CodeEditor,
    isDark: Boolean,
    bgArgb: Int, surfContArgb: Int, surfArgb: Int, onSurfArgb: Int,
    hiContArgb: Int, primaryArgb: Int, primContArgb: Int, secArgb: Int,
    gutterArgb: Int, curLineArgb: Int, dividerArgb: Int, errorArgb: Int,
    warningArgb: Int, selArgb: Int, handleArgb: Int,
) {
    // EditorColorScheme constants are static Java int fields — read via reflection
    // to avoid the "no companion object" Kotlin compile error.
    fun field(name: String): Int =
        runCatching { EditorColorScheme::class.java.getField(name).getInt(null) }.getOrDefault(-1)

    fun alpha(color: Int, a: Float): Int {
        val av = (a * 255).toInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (av shl 24)
    }

    val scheme = editor.colorScheme

    fun s(color: Int, vararg names: String) =
        names.forEach { n -> val id = field(n); if (id >= 0) scheme.setColor(id, color) }

    s(bgArgb,                  "WHOLE_BACKGROUND")
    s(gutterArgb,              "LINE_NUMBER_BACKGROUND")
    s(surfContArgb,            "LINE_NUMBER_PANEL")
    s(dividerArgb,             "LINE_DIVIDER")
    s(alpha(onSurfArgb, 0.5f), "LINE_NUMBER")
    s(primaryArgb,             "LINE_NUMBER_CURRENT")
    s(curLineArgb,             "CURRENT_LINE")
    s(handleArgb,              "SELECTION_HANDLE", "SELECTION_INSERT")
    s(selArgb,                 "SELECTED_TEXT_BACKGROUND", "MATCHED_TEXT_BACKGROUND")
    s(alpha(onSurfArgb, 0.0f), "SCROLL_BAR_TRACK")
    s(alpha(onSurfArgb, 0.3f), "SCROLL_BAR_THUMB")
    s(alpha(onSurfArgb, 0.5f), "SCROLL_BAR_THUMB_PRESSED")
    s(alpha(primaryArgb, 0.6f),"BLOCK_LINE_CURRENT")
    s(alpha(onSurfArgb, 0.2f), "BLOCK_LINE")
    s(alpha(onSurfArgb, 0.25f),"NON_PRINTABLE_CHAR")
    s(surfArgb,                "COMPLETION_WND_BACKGROUND", "COMPLETION_WND_CORNER")
    s(hiContArgb,              "COMPLETION_WND_ITEM_CURRENT")
    s(onSurfArgb,              "COMPLETION_WND_TEXT_PRIMARY")
    s(alpha(onSurfArgb, 0.6f), "COMPLETION_WND_TEXT_SECONDARY")
    s(surfArgb,                "DIAGNOSTIC_TOOLTIP_BACKGROUND", "SIGNATURE_BACKGROUND")
    s(onSurfArgb,              "DIAGNOSTIC_TOOLTIP_BRIEF_MSG", "SIGNATURE_TEXT_NORMAL")
    s(alpha(onSurfArgb, 0.7f), "DIAGNOSTIC_TOOLTIP_DETAILED_MSG")
    s(primaryArgb,             "DIAGNOSTIC_TOOLTIP_ACTION",
                               "SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER",
                               "HIGHLIGHTED_DELIMITERS_FOREGROUND")
    s(errorArgb,               "PROBLEM_ERROR")
    s(warningArgb,             "PROBLEM_WARNING")
    s(secArgb,                 "PROBLEM_TYPO")
    // Optional slots (Sora 0.23.x+)
    s(alpha(onSurfArgb, 0.45f),  "TEXT_INLAY_HINT_FOREGROUND")
    s(alpha(surfContArgb, 0.70f),"TEXT_INLAY_HINT_BACKGROUND")
    s(alpha(primContArgb, 0.55f),"SNIPPET_BACKGROUND_EDITING")
    s(alpha(primContArgb, 0.30f),"SNIPPET_BACKGROUND_RELATED")
    s(alpha(surfContArgb, 0.50f),"SNIPPET_BACKGROUND_INACTIVE")
    s(alpha(primaryArgb, 0.20f), "TEXT_HIGHLIGHT_BACKGROUND")
    s(alpha(primaryArgb, 0.60f), "TEXT_HIGHLIGHT_BORDER")
    s(alpha(primaryArgb, 0.35f), "TEXT_HIGHLIGHT_STRONG_BACKGROUND")
    s(primaryArgb,               "TEXT_HIGHLIGHT_STRONG_BORDER")
    s(dividerArgb,               "SIDE_BLOCK_LINE", "STICKY_SCROLL_DIVIDER", "HARD_WRAP_MARKER")
    s(primaryArgb,               "UNDERLINE")
    s(errorArgb,                 "STRIKETHROUGH")
    s(secArgb,                   "CURRENT_ROW_BORDER")

    // Commit
    editor.colorScheme = scheme
}

// ── TextMate theme ────────────────────────────────────────────────────────────
private suspend fun applyTextMateTheme(ctx: Context, editor: CodeEditor, isDark: Boolean) {
    withContext(Dispatchers.IO) {
        runCatching {
            val themeName = if (isDark) "darcula" else "quietlight"
            val themePath = if (isDark) "textmate/darcula.json" else "textmate/quietlight.json"
            val reg = io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry.getInstance()
            val stream = io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
                .getInstance().tryGetInputStream(themePath) ?: return@runCatching
            // IThemeSource.fromInputStream(stream, fileName, encoding)  — Sora 0.23.x API
            val source = org.eclipse.tm4e.core.registry.IThemeSource
                .fromInputStream(stream, themeName, null)
            val model = io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel(source)
            reg.loadTheme(model)
            withContext(Dispatchers.Main) { reg.setTheme(themeName) }
        }
    }
}

// ── Language ──────────────────────────────────────────────────────────────────
private suspend fun applyLanguage(editor: CodeEditor, lang: FeatureEditorLanguage) {
    val scope = lang.textmateScope
    val editorLang = when {
        lang == FeatureEditorLanguage.JAVA -> JavaLanguage()
        scope != null -> runCatching {
            TextMateLanguage.create(scope, true)
        }.getOrElse { io.github.rosemoe.sora.lang.EmptyLanguage() }
        else -> io.github.rosemoe.sora.lang.EmptyLanguage()
    }
    withContext(Dispatchers.Main) { editor.setEditorLanguage(editorLang) }
}

// ── Font ──────────────────────────────────────────────────────────────────────
private fun applyFont(ctx: Context, editor: CodeEditor, fontPath: String) {
    runCatching {
        val font = ctx.assets.open(fontPath).use {
            android.graphics.BitmapFactory.decodeStream(null)
            Typeface.DEFAULT_BOLD
        }
        // Use asset font directly
        val tf = Typeface.createFromAsset(ctx.assets, fontPath) ?: Typeface.MONOSPACE
        editor.typefaceText       = tf
        editor.typefaceLineNumber = tf
    }.onFailure {
        editor.typefaceText       = Typeface.MONOSPACE
        editor.typefaceLineNumber = Typeface.MONOSPACE
    }
}

// ── Settings ──────────────────────────────────────────────────────────────────
private fun applySettings(editor: CodeEditor, s: FeatureEditorSettings) {
    editor.setTextSize(s.fontSize)
    editor.lineSpacingMultiplier            = s.lineSpacing
    editor.tabWidth                         = s.tabSize
    editor.props.deleteMultiSpaces          = s.tabSize
    editor.isLineNumberEnabled              = s.showLineNumbers
    editor.isWordwrap                       = s.wordWrap
    editor.props.autoIndent                 = s.autoIndent
    editor.props.stickyScroll               = s.stickyScroll
    editor.props.symbolPairAutoCompletion   = s.bracketAutoClose
    editor.props.useICULibToSelectWords     = true
    editor.getComponent(EditorAutoCompletion::class.java)?.isEnabled = s.autoComplete
    editor.isCursorAnimationEnabled         = s.cursorAnimation
    editor.isDisableSoftKbdIfHardKbdAvailable = true
    editor.nonPrintablePaintingFlags = if (s.showWhitespace) {
        CodeEditor.FLAG_DRAW_WHITESPACE_LEADING  or
        CodeEditor.FLAG_DRAW_WHITESPACE_INNER    or
        CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING or
        CodeEditor.FLAG_DRAW_TAB_SAME_AS_SPACE
    } else 0
}
