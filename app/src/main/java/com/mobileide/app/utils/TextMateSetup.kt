package com.mobileide.app.utils

import android.content.Context
import android.util.Log
import com.mobileide.app.data.Language
import com.mobileide.app.editor.CodeHighlighter
import com.mobileide.app.editor.EditorThemeManager
import com.mobileide.app.editor.KeywordManager
import com.mobileide.app.editor.LanguageManager
import com.mobileide.app.editor.DEFAULT_FONT_PATH
import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LogTag
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "TextMateSetup"

// ── SCOPE_MAP: Language → TextMate scope ─────────────────────────────────────
val SCOPE_MAP: Map<Language, String> = mapOf(
    Language.KOTLIN      to "source.kotlin",
    Language.GRADLE      to "source.kotlin",
    Language.JAVA        to "source.java",
    Language.XML         to "text.xml",
    Language.JSON        to "source.json",
    Language.YAML        to "source.yaml",
    Language.TOML        to "source.toml",
    Language.INI         to "source.ini",
    Language.PROPERTIES  to "source.properties",
    Language.HTML        to "text.html.basic",
    Language.HTMX        to "text.html.htmx",
    Language.CSS         to "source.css",
    Language.SCSS        to "source.css.scss",
    Language.LESS        to "source.css.less",
    Language.JAVASCRIPT  to "source.js",
    Language.JSX         to "source.js.jsx",
    Language.TYPESCRIPT  to "source.ts",
    Language.TSX         to "source.tsx",
    Language.PHP         to "text.html.php",
    Language.C           to "source.c",
    Language.CPP         to "source.cpp",
    Language.CSHARP      to "source.cs",
    Language.RUST        to "source.rust",
    Language.GO          to "source.go",
    Language.SWIFT       to "source.swift",
    Language.PYTHON      to "source.python",
    Language.RUBY        to "source.ruby",
    Language.LUA         to "source.lua",
    Language.BASH        to "source.shell",
    Language.POWERSHELL  to "source.powershell",
    Language.BAT         to "source.batchfile",
    Language.MARKDOWN    to "text.html.markdown",
    Language.LATEX       to "text.tex.latex",
    Language.DIFF        to "source.diff",
    Language.LOG         to "text.log",
    Language.DART        to "source.dart",
    Language.SQL         to "source.sql",
    Language.GROOVY      to "source.groovy",
    Language.SMALI       to "source.smali",
    Language.ASM         to "source.asm",
    Language.ZIG         to "source.zig",
    Language.NIM         to "source.nim",
    Language.PASCAL      to "source.pascal",
    Language.LISP        to "source.lisp",
    Language.CMAKE       to "source.cmake",
    Language.COQ         to "source.coq",
    Language.IGNORE      to "source.ignore",
    Language.PLAIN       to "text.plain",
)

enum class EditorTheme(val displayName: String, val isDark: Boolean) {
    DARCULA    ("Darcula",     true),
    QUIETLIGHT ("Quiet Light", false);
    companion object {
        fun fromIsDark(dark: Boolean) = if (dark) DARCULA else QUIETLIGHT
        fun fromName(name: String)    = entries.firstOrNull {
            it.displayName.equals(name, true)
        } ?: DARCULA
    }
}

object TextMateSetup {

    suspend fun initialize(context: Context) {
        LanguageManager.initGrammarRegistry(context)
        KeywordManager.initKeywordRegistry(context)
        CodeHighlighter.registerMarkdownCodeHighlighter(context)
        Logger.success(LogTag.TEXTMATE, "TextMate registries ready")
    }

    suspend fun applyTheme(context: Context, editor: CodeEditor, isDark: Boolean) {
        val scheme = EditorThemeManager.createColorScheme(context, isDark)
        withContext(Dispatchers.Main) { editor.colorScheme = scheme }
        Logger.info(LogTag.TEXTMATE, "Applied theme [isDark=$isDark]")
    }

    suspend fun applyTheme(context: Context, editor: CodeEditor, themeName: String, isDark: Boolean) =
        applyTheme(context, editor, EditorTheme.fromName(themeName).isDark || isDark)

    suspend fun applyLanguage(editor: CodeEditor, language: Language) {
        val scope = SCOPE_MAP[language] ?: "text.plain"
        val lang  = runCatching {
            LanguageManager.createLanguage(editor.context, scope)
        }.getOrElse { e ->
            Logger.error(LogTag.TEXTMATE, "Language create failed: ${e.message}")
            null
        }
        withContext(Dispatchers.Main) { editor.setEditorLanguage(lang ?: EmptyLanguage()) }
    }

    fun configureEditor(editor: CodeEditor, settings: EditorSettings) {
        runCatching {
            editor.setTextSize(settings.fontSize)
            editor.tabWidth                             = settings.tabSize
            editor.props.deleteMultiSpaces              = if (settings.deleteMultiSpaces) settings.tabSize else -1
            editor.isLineNumberEnabled                  = settings.showLineNumbers
            editor.isHardwareAcceleratedDrawAllowed     = true
            editor.isWordwrap                           = settings.wordWrap
            editor.props.autoIndent                     = settings.autoIndent
            editor.props.stickyScroll                   = settings.stickyScroll
            editor.props.symbolPairAutoCompletion       = settings.bracketAutoClose
            editor.getComponent(EditorAutoCompletion::class.java)?.isEnabled = settings.autoComplete
            // Line spacing
            editor.lineSpacingMultiplier                = settings.lineSpacing
            // Cursor
            // Line number pinning
            editor.props.stickyScroll                   = settings.stickyScroll
            // Whitespace visibility
            editor.nonPrintablePaintingFlags = if (settings.showWhitespace) {
                CodeEditor.FLAG_DRAW_WHITESPACE_LEADING  or
                CodeEditor.FLAG_DRAW_WHITESPACE_INNER    or
                CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING or
                CodeEditor.FLAG_DRAW_TAB_SAME_AS_SPACE
            } else 0
            // Highlight current line
            // Complete on enter (soft-keyboard suggestion pick)
            // Use real tab character
        }.onFailure { Log.w(TAG, "configureEditor: ${it.message}") }
    }

    fun invalidateThemeCache() = EditorThemeManager.invalidate()
}

// ── EditorSettings — all user-configurable fields ────────────────────────────
data class EditorSettings(
    // Text
    val fontSize: Float               = 14f,
    val fontPath: String              = DEFAULT_FONT_PATH,
    val lineSpacing: Float            = 1.2f,
    // Indentation
    val tabSize: Int                  = 4,
    val deleteMultiSpaces: Boolean    = true,   // delete N spaces as one Tab
    // Behaviour
    val showLineNumbers: Boolean      = true,
    val wordWrap: Boolean             = false,
    val autoComplete: Boolean         = true,
    val bracketAutoClose: Boolean     = true,
    val autoIndent: Boolean           = true,
    val stickyScroll: Boolean         = false,
    val highlightCurrentLine: Boolean = true,
    val cursorAnimation: Boolean      = true,
    val showWhitespace: Boolean       = false,
    // Smart editing
    val autoCloseTag: Boolean         = true,
    val bulletContinuation: Boolean   = true,
    // Save behaviour
    val autoSave: Boolean             = true,
    val formatOnSave: Boolean         = false,
)
