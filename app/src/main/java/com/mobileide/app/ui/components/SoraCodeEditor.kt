package com.mobileide.app.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mobileide.app.data.Language
import com.mobileide.app.editor.Editor
import com.mobileide.app.editor.intelligent.IntelligentFeatureRegistry
import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LogTag
import com.mobileide.app.utils.EditorSettings
import com.mobileide.app.utils.TextMateSetup
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorKeyEvent
import io.github.rosemoe.sora.event.KeyBindingEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CursorPos(val line: Int, val column: Int)

/**
 * Compose wrapper around MobileIDE's [Editor] subclass.
 *
 * Initialisation order:
 * 1. Grammar / keyword / Markdown-highlighter registries (IO)
 * 2. TextMate colour scheme (IO → Main)
 * 3. Material3 chrome + theme.json overrides (Main)
 * 4. Editor settings + font (Main)
 * 5. Language (IO → Main)
 * 6. Content — **last**, so the renderer is fully ready (Main)
 *
 * All settings changes are re-applied reactively via [LaunchedEffect].
 */
@Composable
fun SoraCodeEditor(
    content: String,
    language: Language,
    settings: EditorSettings,
    themeName: String,
    onContentChange: (String) -> Unit,
    onCursorChange: (CursorPos) -> Unit = {},
    onEditorReady: ((Editor) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark  = isSystemInDarkTheme()
    val cs      = MaterialTheme.colorScheme   // live — recomposed on theme change

    // ── Derive all ARGB ints for Editor.setThemeColors ────────────────────────
    val bgArgb               = cs.background.toArgb()
    val surfContArgb         = cs.surfaceContainer.toArgb()
    val surfArgb             = cs.surface.toArgb()
    val onSurfArgb           = cs.onSurface.toArgb()
    val hiContArgb           = cs.surfaceContainerHigh.toArgb()
    val primaryArgb          = cs.primary.toArgb()
    val primContArgb         = cs.primaryContainer.toArgb()
    val secArgb              = cs.secondary.toArgb()
    val secContArgb          = cs.secondaryContainer.toArgb()
    val selArgb              = cs.primary.copy(alpha = 0.25f).toArgb()
    val handleArgb           = cs.primary.toArgb()
    val gutterArgb           = cs.surfaceContainerLow.toArgb()
    val curLineArgb          = cs.surfaceContainerHigh.toArgb()
    val dividerArgb          = cs.outlineVariant.toArgb()
    val errorArgb            = cs.error.toArgb()
    val warningArgb          = cs.tertiary.toArgb()
    val scrollThumbArgb      = cs.onSurface.copy(alpha = 0.30f).toArgb()
    val scrollPressedArgb    = cs.onSurface.copy(alpha = 0.50f).toArgb()
    val inlayHintFgArgb      = cs.onSurface.copy(alpha = 0.45f).toArgb()
    val inlayHintBgArgb      = cs.surfaceContainer.copy(alpha = 0.70f).toArgb()
    val nonPrintableArgb     = cs.onSurface.copy(alpha = 0.20f).toArgb()
    val matchedBgArgb        = cs.primary.copy(alpha = 0.12f).toArgb()
    val snippetEditingArgb   = cs.primaryContainer.copy(alpha = 0.55f).toArgb()
    val snippetRelatedArgb   = cs.primaryContainer.copy(alpha = 0.30f).toArgb()
    val snippetInactiveArgb  = cs.surfaceContainer.copy(alpha = 0.50f).toArgb()
    val textHighlightArgb    = cs.primary.copy(alpha = 0.20f).toArgb()
    val textHighlightSArgb   = cs.primary.copy(alpha = 0.35f).toArgb()

    val editor = remember {
        Editor(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    var initialized by remember { mutableStateOf(false) }

    // ── Helper: apply full theme colours ──────────────────────────────────────
    fun applyThemeColors() {
        editor.setThemeColors(
            isDarkMode            = isDark,
            editorBackground      = bgArgb,
            surfaceContainer      = surfContArgb,
            surface               = surfArgb,
            onSurface             = onSurfArgb,
            highSurfaceContainer  = hiContArgb,
            colorPrimary          = primaryArgb,
            colorPrimaryContainer = primContArgb,
            colorSecondary        = secArgb,
            secondaryContainer    = secContArgb,
            selectionBg           = selArgb,
            handleColor           = handleArgb,
            gutterColor           = gutterArgb,
            currentLine           = curLineArgb,
            dividerColor          = dividerArgb,
            errorColor            = errorArgb,
            warningColor          = warningArgb,
            scrollThumb           = scrollThumbArgb,
            scrollThumbPressed    = scrollPressedArgb,
            inlayHintFg           = inlayHintFgArgb,
            inlayHintBg           = inlayHintBgArgb,
            nonPrintable          = nonPrintableArgb,
            matchedBg             = matchedBgArgb,
            snippetEditing        = snippetEditingArgb,
            snippetRelated        = snippetRelatedArgb,
            snippetInactive       = snippetInactiveArgb,
            textHighlight         = textHighlightArgb,
            textHighlightStrong   = textHighlightSArgb,
        )
    }

    // ── Full initialisation ───────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        Logger.info(LogTag.SORA_EDITOR, "init: ${language.name} dark=$isDark")

        // 1. Registries
        TextMateSetup.initialize(context)

        // 2. TextMate colour scheme
        TextMateSetup.applyTheme(context, editor, isDark)

        // 3. Material3 chrome
        withContext(Dispatchers.Main) { applyThemeColors() }

        // 4. Settings + font
        withContext(Dispatchers.Main) {
            TextMateSetup.configureEditor(editor, settings)
            editor.applyFont(context, settings.fontPath, isAsset = true)
        }

        // 5. Language
        TextMateSetup.applyLanguage(editor, language)

        // 6. Content — MUST be last
        withContext(Dispatchers.Main) {
            editor.setText(content)
            initialized = true
            onEditorReady?.invoke(editor)
        }
        Logger.success(LogTag.SORA_EDITOR, "init done: ${content.length} chars")
    }

    // ── Re-apply on dark/light toggle ────────────────────────────────────────
    LaunchedEffect(isDark) {
        if (!initialized) return@LaunchedEffect
        TextMateSetup.applyTheme(context, editor, isDark)
        withContext(Dispatchers.Main) { applyThemeColors() }
        Logger.info(LogTag.SORA_EDITOR, "theme re-applied: isDark=$isDark")
    }

    // ── Re-apply on M3 theme name change ─────────────────────────────────────
    LaunchedEffect(themeName) {
        if (!initialized) return@LaunchedEffect
        TextMateSetup.applyTheme(context, editor, isDark)
        withContext(Dispatchers.Main) { applyThemeColors() }
    }

    // ── Re-apply on language change ───────────────────────────────────────────
    LaunchedEffect(language) {
        if (!initialized) return@LaunchedEffect
        TextMateSetup.applyLanguage(editor, language)
    }

    // ── Re-apply on settings change ───────────────────────────────────────────
    LaunchedEffect(settings) {
        if (!initialized) return@LaunchedEffect
        withContext(Dispatchers.Main) {
            TextMateSetup.configureEditor(editor, settings)
            editor.applyFont(context, settings.fontPath, isAsset = true)
        }
    }

    // ── Events ────────────────────────────────────────────────────────────────
    val fileExt = language.ext
    DisposableEffect(editor) {
        val unsubContent = editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
            onContentChange(editor.text.toString())
        }
        val unsubCursor = editor.subscribeEvent(SelectionChangeEvent::class.java) { evt, _ ->
            onCursorChange(CursorPos(evt.left.line + 1, evt.left.column + 1))
        }
        val unsubKey = editor.subscribeEvent(EditorKeyEvent::class.java) { evt, _ ->
            IntelligentFeatureRegistry.dispatchKeyEvent(evt, fileExt, editor)
        }
        val unsubBind = editor.subscribeEvent(KeyBindingEvent::class.java) { evt, _ ->
            IntelligentFeatureRegistry.dispatchKeyBindingEvent(evt, fileExt, editor)
        }
        onDispose {
            runCatching { unsubContent.unsubscribe() }
            runCatching { unsubCursor.unsubscribe() }
            runCatching { unsubKey.unsubscribe() }
            runCatching { unsubBind.unsubscribe() }
            runCatching { editor.release() }
        }
    }

    // ── Content sync on file switch ───────────────────────────────────────────
    AndroidView(
        factory = { editor },
        update  = { ed ->
            if (initialized && content != ed.text.toString()) {
                val line = runCatching { ed.cursor.leftLine }.getOrDefault(0)
                val col  = runCatching { ed.cursor.leftColumn }.getOrDefault(0)
                ed.setText(content)
                runCatching {
                    val maxLine = (ed.text.lineCount - 1).coerceAtLeast(0)
                    val l = line.coerceIn(0, maxLine)
                    val c = col.coerceIn(0, ed.text.getColumnCount(l))
                    ed.setSelection(l, c)
                }
            }
        },
        modifier = modifier,
    )
}

/** Read-only viewer — autocomplete and bracket-close disabled. */
@Composable
fun SoraCodeViewer(
    content: String,
    language: Language,
    themeName: String,
    settings: EditorSettings,
    modifier: Modifier = Modifier,
) = SoraCodeEditor(
    content         = content,
    language        = language,
    settings        = settings.copy(autoComplete = false, bracketAutoClose = false),
    themeName       = themeName,
    onContentChange = {},
    modifier        = modifier,
)
