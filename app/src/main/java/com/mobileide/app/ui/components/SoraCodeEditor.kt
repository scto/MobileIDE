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
 * Compose wrapper around [Editor] (our [io.github.rosemoe.sora.widget.CodeEditor] subclass).
 *
 * Initialisation order (all on IO unless stated otherwise):
 * 1. Grammar + keyword + Markdown-highlighter registries
 * 2. TextMate colour scheme → assigned on Main
 * 3. Theme colours → [Editor.setThemeColors] from Material3 ColorScheme
 * 4. Editor settings (Main)
 * 5. Language (IO create, Main assign)
 * 6. Text content (Main, **last** so renderer is fully ready)
 *
 * Intelligent features (auto-close tag, bullet continuation) receive
 * key events via [IntelligentFeatureRegistry].
 */
@Composable
fun SoraCodeEditor(
    content: String,
    language: Language,
    settings: EditorSettings,
    themeName: String,
    onContentChange: (String) -> Unit,
    onCursorChange: (CursorPos) -> Unit = {},
    onEditorReady: ((com.mobileide.app.editor.Editor) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context    = LocalContext.current
    val isDark     = isSystemInDarkTheme()
    val colors     = MaterialTheme.colorScheme   // live Material3 colours

    // Derive ARGB ints for theme patching from the current Material3 ColorScheme
    val bgArgb          = colors.background.toArgb()
    val surfContArgb    = colors.surfaceContainer.toArgb()
    val surfArgb        = colors.surface.toArgb()
    val onSurfArgb      = colors.onSurface.toArgb()
    val hiContArgb      = colors.surfaceContainerHigh.toArgb()
    val primaryArgb     = colors.primary.toArgb()
    val primContArgb    = colors.primaryContainer.toArgb()
    val secArgb         = colors.secondary.toArgb()
    val secContArgb     = colors.secondaryContainer.toArgb()
    val selArgb         = colors.primary.copy(alpha = 0.27f).toArgb()
    val handleArgb      = colors.primary.toArgb()
    val gutterArgb      = colors.surfaceContainerLow.toArgb()
    val curLineArgb     = colors.surfaceContainer.toArgb()
    val dividerArgb     = colors.outlineVariant.toArgb()
    val errorArgb       = colors.error.toArgb()

    val editor = remember {
        Editor(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    var initialized by remember { mutableStateOf(false) }

    // ── Full initialisation ───────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        Logger.info(LogTag.SORA_EDITOR, "init: ${language.name} / $themeName / dark=$isDark")

        // 1. Registries (IO, idempotent)
        TextMateSetup.initialize(context)

        // 2. TextMate colour scheme (IO build → Main assign)
        TextMateSetup.applyTheme(context, editor, isDark)

        // 3. Material3 chrome patch (Main)
        withContext(Dispatchers.Main) {
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
            )
        }

        // 4. Editor settings + font (Main)
        withContext(Dispatchers.Main) {
            TextMateSetup.configureEditor(editor, settings)
            editor.applyFont(context, settings.fontPath, isAsset = true)
        }

        // 5. Language (IO create → Main assign)
        TextMateSetup.applyLanguage(editor, language)

        // 6. Content — MUST come last
        withContext(Dispatchers.Main) {
            editor.setText(content)
            initialized = true
            onEditorReady?.invoke(editor)
        }
        Logger.success(LogTag.SORA_EDITOR, "init done: ${content.length} chars")
    }

    // ── Re-apply on system dark/light toggle ──────────────────────────────────
    LaunchedEffect(isDark) {
        if (!initialized) return@LaunchedEffect
        TextMateSetup.applyTheme(context, editor, isDark)
        withContext(Dispatchers.Main) {
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
            )
        }
        Logger.info(LogTag.SORA_EDITOR, "theme re-applied: isDark=$isDark")
    }

    // ── Re-apply when editor theme name changes ───────────────────────────────
    LaunchedEffect(themeName) {
        if (!initialized) return@LaunchedEffect
        TextMateSetup.applyTheme(context, editor, isDark)
    }

    // ── Re-apply when language changes ────────────────────────────────────────
    LaunchedEffect(language) {
        if (!initialized) return@LaunchedEffect
        TextMateSetup.applyLanguage(editor, language)
    }

    // ── Re-apply when editor settings change ──────────────────────────────────
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

/** Read-only variant of [SoraCodeEditor] (autocomplete and bracket-close disabled). */
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
