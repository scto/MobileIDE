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

package com.scto.mobile.ide.ui.editor.viewmodel

// TreeSitter
// TextMate
// LSP
import android.app.Application
import android.content.Context
import android.view.ViewGroup
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scto.mobile.ide.R
import com.scto.mobile.ide.core.utils.BackupUtils
import com.scto.mobile.ide.core.utils.LogCatcher
import com.scto.mobile.ide.core.utils.PermissionManager
import com.scto.mobile.ide.lsp.ProotStreamConnectionProvider
import com.scto.mobile.ide.ui.editor.EditorColorSchemeManager
import com.scto.mobile.ide.ui.editor.TextMateInitializer
import com.scto.mobile.ide.ui.editor.components.MediaType
import com.scto.mobile.ide.ui.editor.git.GitManager
import com.tom.rv2ide.treesitter.TSLanguage
import com.tom.rv2ide.treesitter.java.TSLanguageJava
import com.tom.rv2ide.treesitter.json.TSLanguageJson
import com.tom.rv2ide.treesitter.kotlin.TSLanguageKotlin
import com.tom.rv2ide.treesitter.log.TSLanguageLog
import com.tom.rv2ide.treesitter.xml.TSLanguageXml
import io.github.rosemoe.sora.editor.ts.CssLanguage
import io.github.rosemoe.sora.editor.ts.HtmlLanguage
import io.github.rosemoe.sora.editor.ts.JavaScriptLanguage
import io.github.rosemoe.sora.editor.ts.TsLanguage
import io.github.rosemoe.sora.editor.ts.TsLanguageSpec
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.lsp.events.EventContext
import io.github.rosemoe.sora.lsp.events.EventListener
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.io.File
import java.io.InputStreamReader
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.Diagnostic

// ================== 核心数据结构 ==================

interface IEditorTab {
    val title: String
    val file: File
    val uniqueId: String
}

enum class DiffViewMode {
    SPLIT,
    SPLIT_VERTICAL,
    UNIFIED,
}

class DiffEditorState(override val file: File, val originalContent: String, initialCurrentContent: String) :
    IEditorTab {
    var currentContent by mutableStateOf(initialCurrentContent)
    override val title: String = "${file.name} (Diff)"
    override val uniqueId: String = "diff_${file.absolutePath}_${UUID.randomUUID()}"
    var viewMode by mutableStateOf(DiffViewMode.SPLIT)

    var activeDiffEditor: CodeEditor? = null
}

data class MediaEditorState(override val file: File, val mediaType: MediaType) : IEditorTab {
    override val uniqueId: String = file.absolutePath
    override val title: String = file.name
}

data class CodeEditorState(override val file: File) : IEditorTab {
    override val uniqueId: String = file.absolutePath
    override val title: String
        get() = if (isModified) "*${file.name}" else file.name

    var content by mutableStateOf("")
    var savedContent by mutableStateOf("")
    val isModified: Boolean
        get() = content != savedContent

    var lspEditor: LspEditor? = null
    var diagnostics: List<Diagnostic> by mutableStateOf(emptyList())

    fun onContentLoaded(loadedContent: String) {
        content = loadedContent
        savedContent = loadedContent
    }

    fun onContentSaved() {
        savedContent = content
    }
}

data class EditorConfig(
    val fontSize: Float = 14f,
    val tabWidth: Int = 4,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val showInvisibles: Boolean = false,
    val codeFolding: Boolean = true,
    val showToolbar: Boolean = true,
    val showHistory: Boolean = true,
    val fontPath: String = "",
    val customSymbols: String = "Tab,<,>,/,=,\",',!,?,;,:,{,},[,],(,),+,-,*,_,&,|",
    val pinLineNumber: Boolean = false,
    val cursorAnimationEnabled: Boolean = true,
    val smoothScrollEnabled: Boolean = true,
    val cursorBlinkPeriod: Int = 500,
    val highlightCurrentLine: Boolean = true,
    val highlightCurrentBlock: Boolean = true,
    val autoCloseBrackets: Boolean = true,
) {
    fun getSymbolList(): List<String> = customSymbols.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

// ================== ViewModel 实现 ==================

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private var lastColorScheme: ColorScheme? = null

    var hasShownInitialLoader by mutableStateOf(false)
        private set

    var openFiles by mutableStateOf<List<IEditorTab>>(emptyList())
        private set

    // History of closed files (Max 20)
    var closedFilesHistory by mutableStateOf<List<IEditorTab>>(emptyList())
        private set

    var activeFileIndex by mutableIntStateOf(-1)
        private set

    var currentProjectPath by mutableStateOf<String?>(null)
        private set

    var editorConfig by mutableStateOf(EditorConfig())
        private set

    var lastBuiltApk: File? by mutableStateOf(null)
        private set

    private val editorInstances = mutableMapOf<String, CodeEditor>()
    private var hasPermissions = false
    private lateinit var appContext: Context

    private var lspProject: LspProject? = null
    private val addedLspDefinitions = mutableSetOf<String>()
    private var lastSearchQuery = ""
    var isIgnoreCase = true
    private var isFormatting = false

    // ==========================================
    // Unified Content Synchronization Logic
    // ==========================================

    /**
     * Centralized method to handle content updates from ANY source (Editor, Diff, etc.) This ensures that changes are
     * reflected across:
     * 1. Physical File (optional, mostly for Diff)
     * 2. CodeEditorState (if open)
     * 3. DiffEditorState (if open)
     * 4. Active CodeEditor UI instances (for real-time visual sync)
     */
    fun onContentChanged(file: File, newContent: String, sourceInstance: Any? = null, saveToFile: Boolean = false) {
        val canonicalPath =
            try {
                file.canonicalPath
            } catch (_: Exception) {
                file.absolutePath
            }

        // 1. Sync to Physical File (if requested)
        if (saveToFile) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    file.writeText(newContent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 2. Sync to CodeEditorState
        openFiles
            .filterIsInstance<CodeEditorState>()
            .find {
                val p =
                    try {
                        it.file.canonicalPath
                    } catch (_: Exception) {
                        it.file.absolutePath
                    }
                p == canonicalPath
            }
            ?.let { state ->
                if (state.content != newContent) {
                    state.content = newContent
                    // If saved to file, update savedContent too
                    if (saveToFile) {
                        state.savedContent = newContent
                    }
                }
            }

        // 3. Sync to DiffEditorState
        openFiles
            .filterIsInstance<DiffEditorState>()
            .find {
                val p =
                    try {
                        it.file.canonicalPath
                    } catch (_: Exception) {
                        it.file.absolutePath
                    }
                p == canonicalPath
            }
            ?.let { state ->
                if (state.currentContent != newContent) {
                    state.currentContent = newContent
                }
            }

        // 4. Sync to Active Editor Instances (Visual Update)
        viewModelScope.launch(Dispatchers.Main) {
            editorInstances.entries.forEach { (path, editor) ->
                // Skip if this editor instance initiated the change
                if (editor === sourceInstance) return@forEach

                val entryFile = File(path)
                val entryPath =
                    try {
                        entryFile.canonicalPath
                    } catch (_: Exception) {
                        entryFile.absolutePath
                    }

                if (entryPath == canonicalPath) {
                    if (editor.text.toString() != newContent) {
                        val cursor = editor.cursor
                        val line = cursor.leftLine
                        val column = cursor.leftColumn

                        editor.setText(newContent)

                        // Try to preserve cursor
                        try {
                            if (line < editor.text.lineCount) {
                                editor.setSelection(line, column.coerceAtMost(editor.text.getColumnCount(line)))
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    @Synchronized
    fun getOrCreateEditor(context: Context, state: CodeEditorState): CodeEditor {
        val filePath = state.file.absolutePath

        editorInstances[filePath]?.let { existingEditor ->
            if (existingEditor.context != context) {
                try {
                    state.lspEditor?.dispose()
                    state.lspEditor = null
                    (existingEditor.parent as? ViewGroup)?.removeView(existingEditor)
                    existingEditor.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                editorInstances.remove(filePath)
            } else {
                (existingEditor.parent as? ViewGroup)?.removeView(existingEditor)
                return existingEditor
            }
        }

        val editor =
            CodeEditor(context).apply {
                layoutParams = ViewGroup.LayoutParams(-1, -1)
                isFocusable = true
                isEnabled = true
                setText(state.content)

                // Remove zoom limits
                setScaleTextSizes(2f, 300f)

                applyLanguageToEditor(this, state.file.name)

                setSelection(0, 0)
                text.addContentListener(
                    object : ContentListener {
                        override fun beforeReplace(content: Content) {}

                        override fun afterInsert(
                            content: Content,
                            startLine: Int,
                            startColumn: Int,
                            endLine: Int,
                            endColumn: Int,
                            inserted: CharSequence,
                        ) {
                            val newText = content.toString()
                            // Use centralized sync
                            onContentChanged(state.file, newText, sourceInstance = this@apply)
                        }

                        override fun afterDelete(
                            content: Content,
                            startLine: Int,
                            startColumn: Int,
                            endLine: Int,
                            endColumn: Int,
                            deleted: CharSequence,
                        ) {
                            val newText = content.toString()
                            // Use centralized sync
                            onContentChanged(state.file, newText, sourceInstance = this@apply)
                        }
                    }
                )
            }

        val currentLanguage = editor.editorLanguage
        val textMateLanguage = currentLanguage as? TextMateLanguage
        setupLspForEditor(context, state, editor, textMateLanguage)

        editorInstances[filePath] = editor
        return editor
    }

    fun applyLanguageToEditor(editor: CodeEditor, filenameOrExtension: String) {
        val context = getApplication<Application>()
        val ext =
            if (filenameOrExtension.contains('.')) {
                filenameOrExtension.substringAfterLast('.').lowercase()
            } else {
                filenameOrExtension.lowercase()
            }

        val prefs = context.getSharedPreferences("MobileIDE_Editor_Settings", Context.MODE_PRIVATE)
        val editorType = prefs.getString("editor_type", "textmate") ?: "textmate"

        if (LogCatcher.isLoggingEnabled) {
            LogCatcher.i(
                "EditorHighlight",
                "Applying language for file: $filenameOrExtension, ext: $ext, preferred engine: $editorType",
            )
        }

        var applied = false

        if (editorType == "treesitter") {
            val tsLanguage = loadTreeSitterLanguage(context, ext)
            if (tsLanguage != null) {
                if (LogCatcher.isLoggingEnabled) {
                    LogCatcher.i("EditorHighlight", "Applied TreeSitter language for $ext")
                }
                editor.setEditorLanguage(tsLanguage)
                lastColorScheme?.let { EditorColorSchemeManager.applyThemeColors(editor.colorScheme, it) }
                configureRainbowColors(editor.colorScheme)
                applied = true
            } else {
                if (LogCatcher.isLoggingEnabled) {
                    LogCatcher.i(
                        "EditorHighlight",
                        "TreeSitter language not supported for $ext, falling back to TextMate",
                    )
                }
            }
        }

        if (!applied) {
            val tmLanguage = loadTextMateLanguage(context, ext)
            if (tmLanguage != null) {
                if (LogCatcher.isLoggingEnabled) {
                    LogCatcher.i("EditorHighlight", "Applied TextMate language for $ext")
                }
                editor.setEditorLanguage(tmLanguage)
                try {
                    editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
                    if (LogCatcher.isLoggingEnabled) {
                        LogCatcher.i("EditorHighlight", "Applied TextMateColorScheme successfully")
                    }
                } catch (e: Exception) {
                    if (LogCatcher.isLoggingEnabled) {
                        LogCatcher.e("EditorHighlight", "Failed to apply TextMateColorScheme", e)
                    }
                }
                applied = true
            } else {
                if (LogCatcher.isLoggingEnabled) {
                    LogCatcher.i("EditorHighlight", "TextMate language not supported for $ext")
                }
            }
        }

        if (!applied && editorType != "treesitter") {
            // Fallback to TreeSitter if TextMate wasn't supported
            val tsLanguage = loadTreeSitterLanguage(context, ext)
            if (tsLanguage != null) {
                if (LogCatcher.isLoggingEnabled) {
                    LogCatcher.i("EditorHighlight", "Fallback: Applied TreeSitter language for $ext")
                }
                editor.setEditorLanguage(tsLanguage)
                lastColorScheme?.let { EditorColorSchemeManager.applyThemeColors(editor.colorScheme, it) }
                configureRainbowColors(editor.colorScheme)
                applied = true
            }
        }

        if (!applied) {
            if (LogCatcher.isLoggingEnabled) {
                LogCatcher.i("EditorHighlight", "No language found for $ext. Applied EmptyLanguage")
            }
            editor.setEditorLanguage(EmptyLanguage())
        }
    }

    // 🔥 修复报错: 补充 reloadAllEditors 方法
    fun reloadAllEditors(context: Context) {
        viewModelScope.launch(Dispatchers.Main) {
            val currentIndex = activeFileIndex
            openFiles.forEach { tab ->
                if (tab is CodeEditorState) {
                    val editor = editorInstances[tab.file.absolutePath] ?: return@forEach
                    val cursorLine = editor.cursor.leftLine
                    val cursorColumn = editor.cursor.leftColumn

                    try {
                        tab.lspEditor?.dispose()
                    } catch (_: Exception) {}
                    tab.lspEditor = null

                    applyLanguageToEditor(editor, tab.file.name)

                    val currentLang = editor.editorLanguage
                    if (currentLang is TextMateLanguage) {
                        setupLspForEditor(context, tab, editor, currentLang)
                    }

                    editor.setSelection(cursorLine, cursorColumn)
                }
            }
            activeFileIndex = currentIndex
        }
    }

    fun openDiff(projectPath: String, file: File) {
        viewModelScope.launch {
            val gitManager = GitManager(projectPath)
            val headContent = gitManager.getFileContentAtHead(file.absolutePath)
            val currentContent =
                withContext(Dispatchers.IO) {
                    try {
                        file.readText()
                    } catch (_: Exception) {
                        ""
                    }
                }

            val diffState = DiffEditorState(file, headContent, currentContent)
            val existingIndex =
                openFiles.indexOfFirst { it is DiffEditorState && it.file.absolutePath == file.absolutePath }

            if (existingIndex != -1) {
                activeFileIndex = existingIndex
            } else {
                openFiles = openFiles + diffState
                activeFileIndex = openFiles.lastIndex
            }
        }
    }

    fun updateDiffContent(state: DiffEditorState, newContent: String) {
        // Use centralized sync logic
        // sourceInstance is null because DiffViewer manages its own editor instance separately,
        // but we want to update the main editor instances if they exist.
        // saveToFile = true because Diff view changes are meant to be persisted immediately (per user request)
        onContentChanged(file = state.file, newContent = newContent, sourceInstance = null, saveToFile = true)
    }

    private fun loadTreeSitterLanguage(context: Context, extension: String): TsLanguage? {
        if (LogCatcher.isLoggingEnabled) {
            LogCatcher.i("EditorHighlight", "loadTreeSitterLanguage called for extension: $extension")
        }
        try {
            val language: TSLanguage =
                when (extension) {
                    "html",
                    "htm" -> HtmlLanguage()
                    "css" -> CssLanguage()
                    "js",
                    "mjs",
                    "cjs",
                    "javascript" -> JavaScriptLanguage()
                    "json",
                    "JSON" -> TSLanguageJson.getInstance()
                    "kt",
                    "kts" -> TSLanguageKotlin.getInstance()
                    "java",
                    "jav" -> TSLanguageJava.getInstance()
                    "xml",
                    "xaml",
                    "svg",
                    "plist" -> TSLanguageXml.getInstance()
                    "log" -> TSLanguageLog.getInstance()
                    else -> return null
                }
            val langFolderName =
                when (extension) {
                    "js",
                    "mjs",
                    "cjs",
                    "javascript" -> "javascript"
                    "htm" -> "html"
                    "kt",
                    "kts" -> "kotlin"
                    "jav" -> "java"
                    "xaml",
                    "svg",
                    "plist" -> "xml"
                    else -> extension
                }
            val highlightsScm =
                try {
                    context.assets.open("queries/$langFolderName/highlights.scm").use {
                        InputStreamReader(it).readText()
                    }
                } catch (_: Exception) {
                    ""
                }

            if (highlightsScm.isBlank()) {
                if (LogCatcher.isLoggingEnabled) {
                    LogCatcher.w("EditorHighlight", "No highlights.scm found for: $langFolderName")
                }
                return null
            }

            val spec =
                TsLanguageSpec(language, highlightsScm).apply {
                    rainbowBracketsEnabled = true
                    rainbowBracketsBaseColorId = 256
                    rainbowBracketsColorCount = 6
                }
            return TsLanguage(spec) { applyTreeSitterTheme(langFolderName) }
        } catch (e: Throwable) {
            if (LogCatcher.isLoggingEnabled) {
                LogCatcher.e("EditorHighlight", "Failed to create TreeSitter language for: $extension", e)
            }
            return null
        }
    }

    /**
     * Applies a rich Tree-sitter color theme based on the language. All languages share a common set of semantic token
     * mappings.
     */
    private fun io.github.rosemoe.sora.editor.ts.TsThemeBuilder.applyTreeSitterTheme(langFolder: String) {
        // === Common semantic tokens ===
        // Default text
        TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL) applyTo ""

        // Keywords
        TextStyle.makeStyle(EditorColorScheme.KEYWORD) applyTo
            arrayOf(
                "keyword",
                "keyword.control",
                "keyword.operator",
                "keyword.return",
                "keyword.import",
                "keyword.modifier",
                "keyword.type",
                "keyword.function",
            )

        // Comments
        TextStyle.makeStyle(EditorColorScheme.COMMENT) applyTo
            arrayOf("comment", "comment.line", "comment.block", "comment.documentation")

        // Strings
        val stringColorId =
            when (langFolder) {
                "html" -> EditorColorScheme.ATTRIBUTE_VALUE
                else -> EditorColorScheme.LITERAL
            }
        TextStyle.makeStyle(stringColorId) applyTo
            arrayOf(
                "string",
                "string.special",
                "string.special.key",
                "string.escape",
                "string.template",
                "string.regex",
            )

        // Numbers and constants
        TextStyle.makeStyle(EditorColorScheme.LITERAL) applyTo
            arrayOf(
                "number",
                "number.float",
                "constant",
                "constant.builtin",
                "constant.numeric",
                "constant.language",
                "escape",
            )

        // Functions
        TextStyle.makeStyle(EditorColorScheme.FUNCTION_NAME) applyTo
            arrayOf(
                "function",
                "function.call",
                "function.method",
                "function.method.call",
                "function.builtin",
                "function.constructor",
                "constructor",
            )

        // Variables
        TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_VAR) applyTo
            arrayOf("variable", "variable.builtin", "variable.parameter", "variable.other", "variable.other.readwrite")

        // Types
        TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_NAME) applyTo
            arrayOf("type", "type.builtin", "type.definition", "type.parameter", "class", "class.name")

        // Properties
        TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_NAME) applyTo "property"

        // Operators and punctuation
        TextStyle.makeStyle(EditorColorScheme.OPERATOR) applyTo arrayOf("operator", "punctuation.special")
        TextStyle.makeStyle(EditorColorScheme.OPERATOR) applyTo arrayOf("punctuation.bracket", "punctuation.delimiter")

        // === Language-specific tokens ===
        when (langFolder) {
            "html" -> {
                TextStyle.makeStyle(EditorColorScheme.HTML_TAG) applyTo arrayOf("tag", "tag.error")
                TextStyle.makeStyle(EditorColorScheme.ATTRIBUTE_NAME) applyTo "attribute"
                TextStyle.makeStyle(EditorColorScheme.ATTRIBUTE_VALUE) applyTo "string"
            }
            "xml" -> {
                TextStyle.makeStyle(EditorColorScheme.HTML_TAG) applyTo arrayOf("tag", "tag.error", "element.tag")
                TextStyle.makeStyle(EditorColorScheme.ATTRIBUTE_NAME) applyTo arrayOf("attribute", "attr.name")
                TextStyle.makeStyle(EditorColorScheme.ATTRIBUTE_VALUE) applyTo arrayOf("string", "attr.value")
                TextStyle.makeStyle(EditorColorScheme.KEYWORD) applyTo "xml_decl"
                TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_NAME) applyTo
                    arrayOf("ns_declarator", "xmlns.prefix", "attr.prefix")
                TextStyle.makeStyle(EditorColorScheme.LITERAL) applyTo
                    arrayOf("xml.ref", "cdata.start", "cdata.end", "cdata.data")
            }
            "css" -> {
                TextStyle.makeStyle(EditorColorScheme.HTML_TAG) applyTo "tag"
                TextStyle.makeStyle(EditorColorScheme.ATTRIBUTE_NAME) applyTo arrayOf("attribute", "property")
                TextStyle.makeStyle(EditorColorScheme.LITERAL) applyTo "string.special" // color values
            }
            "kotlin",
            "java" -> {
                TextStyle.makeStyle(EditorColorScheme.ATTRIBUTE_NAME) applyTo arrayOf("attribute", "annotation")
                TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_NAME) applyTo "type"
                TextStyle.makeStyle(EditorColorScheme.FUNCTION_NAME) applyTo
                    arrayOf("function", "function.method", "constructor")
                TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_VAR) applyTo arrayOf("variable", "variable.builtin")
            }
            "python" -> {
                TextStyle.makeStyle(EditorColorScheme.ATTRIBUTE_NAME) applyTo "decorator"
                TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_NAME) applyTo "type"
                TextStyle.makeStyle(EditorColorScheme.FUNCTION_NAME) applyTo
                    arrayOf("function", "function.method", "function.builtin")
            }
        }
    }

    private fun loadTextMateLanguage(context: Context, extension: String): TextMateLanguage? {
        if (LogCatcher.isLoggingEnabled) {
            LogCatcher.i("TextMateLanguage", "loadTextMateLanguage called for extension: $extension")
        }
        return try {
            if (!TextMateInitializer.isReady()) {
                if (LogCatcher.isLoggingEnabled) {
                    LogCatcher.i("TextMateLanguage", "TextMateInitializer is NOT ready. Starting initialization.")
                }
                TextMateInitializer.initialize(context)
                return null
            }
            val scopeName =
                when (extension) {
                    "html",
                    "htm" -> "text.html.basic"
                    "css" -> "source.css"
                    "js",
                    "javascript" -> "source.js"
                    "glsl",
                    "vert",
                    "frag" -> "source.c"
                    "c",
                    "h" -> "source.c"
                    "cpp",
                    "hpp",
                    "cc" -> "source.cpp"
                    "php" -> "source.php"
                    "ts",
                    "typescript" -> "source.ts"
                    "tsx" -> "source.tsx"
                    "bat",
                    "cmd" -> "source.batchfile"
                    "clj",
                    "cljs",
                    "cljc",
                    "edn" -> "source.clojure"
                    "coffee" -> "source.coffee"
                    "cs" -> "source.cs"
                    "dart" -> "source.dart"
                    "diff",
                    "patch" -> "source.diff"
                    "dockerfile",
                    "docker" -> "source.dockerfile"
                    "fs",
                    "fsi",
                    "fsx",
                    "fsscript" -> "source.fsharp"
                    "go" -> "source.go"
                    "groovy",
                    "gvy",
                    "gradle" -> "source.groovy"
                    "handlebars",
                    "hbs" -> "text.html.handlebars"
                    "hlsl" -> "source.hlsl"
                    "java",
                    "jav" -> "source.java"
                    "kt",
                    "kts" -> "source.kotlin"
                    "xml",
                    "xaml",
                    "dtd",
                    "plist",
                    "svg" -> "text.xml"
                    "properties",
                    "cfg",
                    "conf",
                    "config",
                    "editorconfig",
                    "gitconfig",
                    "gitmodules",
                    "gitattributes" -> "source.properties"
                    "toml" -> "source.toml"
                    "ini" -> "source.ini"
                    "cmake",
                    "cmakelists" -> "source.cmake"
                    "log" -> "text.log"
                    "aidl" -> "source.aidl"
                    "json" -> "source.json"
                    "jsonc" -> "source.json.comments"
                    "jsonl" -> "source.json.lines"
                    "cu",
                    "cuh" -> "source.cuda-cpp"
                    "jl" -> "source.julia"
                    "less" -> "source.css.less"
                    "lua" -> "source.lua"
                    "makefile",
                    "mk",
                    "mak" -> "source.makefile"
                    "md",
                    "markdown" -> "text.html.markdown"
                    "m" -> "source.objc"
                    "mm" -> "source.objcpp"
                    "ps1",
                    "psm1",
                    "psd1" -> "source.powershell"
                    "pug",
                    "jade" -> "text.pug"
                    "py",
                    "rpy",
                    "pyw",
                    "cp",
                    "python" -> "source.python"
                    "r",
                    "rhistory",
                    "rprofile" -> "source.r"
                    "cshtml" -> "text.html.cshtml"
                    "rst" -> "source.rst"
                    "rb",
                    "rbx",
                    "rjs",
                    "gemspec" -> "source.ruby"
                    "rs" -> "source.rust"
                    "scss" -> "source.css.scss"
                    "shader" -> "source.shaderlab"
                    "sh",
                    "bash",
                    "zsh" -> "source.shell"
                    "sql" -> "source.sql"
                    "swift" -> "source.swift"
                    "vb",
                    "vbs" -> "source.asp.vb.net"
                    "yaml",
                    "yml" -> "source.yaml"
                    else -> {
                        if (LogCatcher.isLoggingEnabled) {
                            LogCatcher.i("TextMateLanguage", "No scopeName mapped for extension: $extension")
                        }
                        return null
                    }
                }
            if (LogCatcher.isLoggingEnabled) {
                LogCatcher.i("TextMateLanguage", "Mapped extension $extension to scope: $scopeName")
            }
            val prefs = context.getSharedPreferences("MobileIDE_Editor_Settings", Context.MODE_PRIVATE)
            val lspEnabled = prefs.getBoolean("editor_lsp_enabled", false)
            val tmLang = TextMateLanguage.create(scopeName, !lspEnabled)
            if (LogCatcher.isLoggingEnabled) {
                LogCatcher.i("TextMateLanguage", "TextMateLanguage created successfully for scope: $scopeName")
            }
            tmLang
        } catch (e: Exception) {
            if (LogCatcher.isLoggingEnabled) {
                LogCatcher.e("TextMateLanguage", "Failed to create TextMateLanguage for extension: $extension", e)
            }
            null
        }
    }

    fun configureRainbowColors(scheme: EditorColorScheme) {
        scheme.setColor(256, 0xFFFF6B6B.toInt())
        scheme.setColor(257, 0xFFFFD93D.toInt())
        scheme.setColor(258, 0xFF6BCB77.toInt())
        scheme.setColor(259, 0xFF4D96FF.toInt())
        scheme.setColor(260, 0xFF9D4EDD.toInt())
        scheme.setColor(261, 0xFF00E5FF.toInt())
    }

    fun openFile(file: File) {
        if (file.isDirectory || !file.exists()) return
        viewModelScope.launch {
            // Check if file is already open
            val existingIndex =
                openFiles.indexOfFirst {
                    (it is CodeEditorState && it.file.absolutePath == file.absolutePath) ||
                        (it is MediaEditorState && it.file.absolutePath == file.absolutePath)
                }
            if (existingIndex != -1) {
                activeFileIndex = existingIndex
            } else {
                val extension = file.extension.lowercase()
                val mediaType =
                    when (extension) {
                        "png",
                        "jpg",
                        "jpeg",
                        "gif",
                        "webp",
                        "bmp",
                        "ico" -> MediaType.IMAGE
                        "svg" -> MediaType.SVG
                        "mp4",
                        "mkv",
                        "webm",
                        "avi",
                        "3gp" -> MediaType.VIDEO
                        else -> null
                    }

                if (mediaType != null) {
                    val newState = MediaEditorState(file = file, mediaType = mediaType)
                    openFiles = openFiles + newState
                    activeFileIndex = openFiles.lastIndex
                } else {
                    val content =
                        withContext(Dispatchers.IO) {
                            try {
                                file.readText(Charsets.UTF_8)
                            } catch (_: Exception) {
                                ""
                            }
                        }
                    val newState = CodeEditorState(file = file)
                    newState.onContentLoaded(content)
                    openFiles = openFiles + newState
                    activeFileIndex = openFiles.lastIndex
                }
            }
        }
    }

    suspend fun saveAllModifiedFiles(context: Context, snackbarHostState: SnackbarHostState): Boolean {
        return withContext(Dispatchers.IO) {
            val modifiedFiles = openFiles.filterIsInstance<CodeEditorState>().filter { it.isModified }
            if (modifiedFiles.isEmpty()) return@withContext true

            var successCount = 0
            var failCount = 0
            var lastError: String? = null

            modifiedFiles.forEach { state ->
                try {
                    state.file.writeText(state.content)
                    state.onContentSaved()
                    successCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                    failCount++
                    lastError = e.message
                }
            }

            val message =
                if (failCount == 0) {
                    context.getString(R.string.editor_saved_files, successCount)
                } else {
                    context.getString(R.string.editor_save_all_result, successCount, failCount, lastError)
                }

            withContext(Dispatchers.Main) { viewModelScope.launch { snackbarHostState.showSnackbar(message) } }

            failCount == 0
        }
    }

    fun closeFile(indexToClose: Int) {
        if (indexToClose !in openFiles.indices) return
        val tab = openFiles[indexToClose]

        // Add to history (Limit to 20)
        val newHistory = closedFilesHistory.toMutableList()
        // Remove if already exists to move it to the top
        newHistory.removeAll { it.file.absolutePath == tab.file.absolutePath }
        newHistory.add(0, tab)
        if (newHistory.size > 20) {
            newHistory.removeAt(newHistory.lastIndex)
        }
        closedFilesHistory = newHistory

        if (tab is CodeEditorState) {
            try {
                tab.lspEditor?.dispose()
            } catch (_: Exception) {}
            editorInstances.remove(tab.file.absolutePath)?.release()
        }

        openFiles = openFiles.toMutableList().also { it.removeAt(indexToClose) }
        if (openFiles.isEmpty()) {
            activeFileIndex = -1
        } else if (activeFileIndex >= indexToClose) {
            activeFileIndex = (activeFileIndex - 1).coerceAtLeast(0)
        }
    }

    fun restoreClosedFile(tab: IEditorTab) {
        // Remove from history
        closedFilesHistory = closedFilesHistory.filter { it.file.absolutePath != tab.file.absolutePath }
        // Open file
        openFile(tab.file)
    }

    fun clearClosedHistory() {
        closedFilesHistory = emptyList()
    }

    fun closeOtherFiles(indexToKeep: Int) {
        if (indexToKeep !in openFiles.indices) return
        val keepTab = openFiles[indexToKeep]

        openFiles.forEachIndexed { index, tab ->
            if (index != indexToKeep && tab is CodeEditorState) {
                try {
                    tab.lspEditor?.dispose()
                } catch (_: Exception) {}
                editorInstances.remove(tab.file.absolutePath)?.release()
            }
        }
        openFiles = listOf(keepTab)
        activeFileIndex = 0
    }

    fun closeAllFiles() {
        openFiles.filterIsInstance<CodeEditorState>().forEach {
            try {
                it.lspEditor?.dispose()
            } catch (_: Exception) {}
        }
        editorInstances.values.forEach {
            try {
                it.release()
            } catch (_: Exception) {}
        }
        editorInstances.clear()
        openFiles = emptyList()
        activeFileIndex = -1
    }

    fun changeActiveFileIndex(index: Int) {
        if (index in openFiles.indices) activeFileIndex = index
    }

    fun getActiveEditor(): CodeEditor? {
        val activeTab = openFiles.getOrNull(activeFileIndex) ?: return null
        return when (activeTab) {
            is CodeEditorState -> editorInstances[activeTab.file.absolutePath]
            is DiffEditorState -> activeTab.activeDiffEditor
            else -> null
        }
    }

    fun undo() {
        getActiveEditor()?.undo()
    }

    fun redo() {
        getActiveEditor()?.redo()
    }

    fun insertSymbol(symbol: String) {
        val p = if (symbol == "Tab") "\t" else symbol
        getActiveEditor()?.insertText(p, p.length)
    }

    fun insertText(text: String) {
        insertSymbol(text)
    }

    fun jumpToLine(lineStr: String) {
        val line = lineStr.toIntOrNull() ?: return
        getActiveEditor()?.let { editor ->
            val totalLines = editor.text.lineCount
            val targetLine = (line - 1).coerceIn(0, totalLines - 1)
            editor.setSelection(targetLine, 0)
            editor.ensureSelectionVisible()
        }
    }

    fun loadInitialFile(projectPath: String) {
        if (projectPath != currentProjectPath) {
            closeAllFiles()
            clearClosedHistory() // Clear history when switching projects
            currentProjectPath = projectPath
            val indexFile = File(projectPath, "index.html")
            if (indexFile.exists()) openFile(indexFile)
        }
    }

    suspend fun autoSaveProject(context: Context, projectPath: String) {
        withContext(Dispatchers.IO) {
            val modifiedFiles = openFiles.filterIsInstance<CodeEditorState>().filter { it.isModified }
            if (modifiedFiles.isNotEmpty()) {
                modifiedFiles.forEach { state ->
                    try {
                        state.file.writeText(state.content)
                        state.onContentSaved()
                    } catch (_: Exception) {}
                }
                BackupUtils.backupProject(context, projectPath)
            }
        }
    }

    fun reloadEditorConfig(context: Context) {
        val prefs = context.getSharedPreferences("MobileIDE_Editor_Settings", Context.MODE_PRIVATE)
        editorConfig =
            EditorConfig(
                fontSize = prefs.getFloat("editor_font_size", 14f),
                tabWidth = prefs.getInt("editor_tab_width", 4),
                wordWrap = prefs.getBoolean("editor_word_wrap", false),
                showInvisibles = prefs.getBoolean("editor_show_invisibles", false),
                codeFolding = prefs.getBoolean("editor_code_folding", true),
                showToolbar = prefs.getBoolean("editor_show_toolbar", true),
                showHistory = prefs.getBoolean("editor_show_history", true),
                fontPath = prefs.getString("editor_font_path", "") ?: "",
                customSymbols =
                    prefs.getString("editor_custom_symbols", "Tab,<,>,/,=,\",',!,?,;,:,{,},[,],(,),+,-,*,_,&,|") ?: "",
                pinLineNumber = prefs.getBoolean("editor_pin_line_number", false),
                cursorAnimationEnabled = prefs.getBoolean("editor_cursor_animation", true),
                smoothScrollEnabled = prefs.getBoolean("editor_smooth_scroll", true),
                cursorBlinkPeriod = prefs.getInt("editor_cursor_blink", 500),
                highlightCurrentLine = prefs.getBoolean("editor_highlight_current_line", true),
                highlightCurrentBlock = prefs.getBoolean("editor_highlight_current_block", true),
                autoCloseBrackets = prefs.getBoolean("editor_auto_close_brackets", true),
            )
    }

    fun initializePermissions(context: Context) {
        appContext = context.applicationContext
        hasPermissions = PermissionManager.hasRequiredPermissions(appContext)
    }

    fun onInitialLoaderShown() {
        hasShownInitialLoader = true
    }

    fun updateLastBuild(path: String?) {
        lastBuiltApk = if (path != null) File(path) else null
    }

    fun updateEditorTheme(colorScheme: ColorScheme) {
        lastColorScheme = colorScheme
        editorInstances.values.forEach { editor ->
            if (editor.editorLanguage is TextMateLanguage) {
                // For TextMate editors, only apply background/UI colors, not syntax colors.
                // The TextMateColorScheme manages syntax colors from the theme JSON files.
                // We only need to update structural colors (background, line numbers etc.)
                EditorColorSchemeManager.applyUiColors(editor.colorScheme, colorScheme)
            } else {
                EditorColorSchemeManager.applyThemeColors(editor.colorScheme, colorScheme)
                // Re-apply rainbow colors if needed (as applying theme colors might reset some custom colors)
                if (editor.editorLanguage is TsLanguage) {
                    configureRainbowColors(editor.colorScheme)
                }
            }
            editor.invalidate()
        }
    }

    fun createNewItem(parentPath: String, name: String, isFile: Boolean, onSuccess: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newItem = File(parentPath, name)
                if (newItem.exists()) return@launch
                val success = if (isFile) newItem.createNewFile() else newItem.mkdirs()
                if (success) withContext(Dispatchers.Main) { onSuccess(newItem) }
            } catch (e: Exception) {
                LogCatcher.e("FileOps", "Create failed", e)
            }
        }
    }

    fun updateRenamedFile(oldFile: File, newFile: File) {
        val index = openFiles.indexOfFirst { it.file.absolutePath == oldFile.absolutePath }
        if (index != -1) {
            val oldTab = openFiles[index]
            if (oldTab is CodeEditorState) {
                val newState = oldTab.copy(file = newFile)
                newState.content = oldTab.content
                newState.savedContent = oldTab.savedContent
                newState.lspEditor = null

                val mutableList = openFiles.toMutableList()
                mutableList[index] = newState
                openFiles = mutableList

                val oldEditor = editorInstances.remove(oldFile.absolutePath)
                if (oldEditor != null) {
                    editorInstances[newFile.absolutePath] = oldEditor
                }
            }
        }
    }

    var isUseRegex = false

    fun searchText(query: String, ignoreCase: Boolean = isIgnoreCase, useRegex: Boolean = isUseRegex) {
        lastSearchQuery = query
        isIgnoreCase = ignoreCase
        isUseRegex = useRegex
        val editor = getActiveEditor() ?: return
        if (query.isNotEmpty()) {
            try {
                // 先停止之前的搜索，避免状态混乱
                editor.searcher.stopSearch()
                val type =
                    if (useRegex) io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions.TYPE_REGULAR_EXPRESSION
                    else io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions.TYPE_NORMAL
                editor.searcher.search(
                    query,
                    io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions(type, ignoreCase),
                )
            } catch (e: Exception) {
                LogCatcher.e("Search", "Search failed", e)
            }
        } else {
            editor.searcher.stopSearch()
        }
    }

    fun searchNext() {
        try {
            val editor = getActiveEditor() ?: return
            val searcher = editor.searcher

            // 检查是否有活跃的搜索结果
            if (searcher.hasQuery()) {
                searcher.gotoNext()
            } else if (lastSearchQuery.isNotEmpty()) {
                // 如果没有活跃搜索但之前有搜索词，重新搜索
                searchText(lastSearchQuery, isIgnoreCase)
                searcher.gotoNext()
            }
        } catch (e: Exception) {
            LogCatcher.e("Search", "Search next failed", e)
        }
    }

    fun searchPrev() {
        try {
            val editor = getActiveEditor() ?: return
            val searcher = editor.searcher

            // 检查是否有活跃的搜索结果
            if (searcher.hasQuery()) {
                searcher.gotoPrevious()
            } else if (lastSearchQuery.isNotEmpty()) {
                // 如果没有活跃搜索但之前有搜索词，重新搜索
                searchText(lastSearchQuery, isIgnoreCase)
                searcher.gotoPrevious()
            }
        } catch (e: Exception) {
            LogCatcher.e("Search", "Search previous failed", e)
        }
    }

    fun stopSearch() {
        try {
            getActiveEditor()?.searcher?.stopSearch()
        } catch (e: Exception) {
            LogCatcher.e("Search", "Stop search failed", e)
        }
    }

    fun replaceCurrent(text: String) {
        getActiveEditor()?.searcher?.replaceCurrentMatch(text)
    }

    fun replaceAll(text: String) {
        getActiveEditor()?.searcher?.replaceAll(text)
    }

    fun updateCodeWithUndo(newContent: String) {
        val editor = getActiveEditor() ?: return
        viewModelScope.launch(Dispatchers.Main) {
            val text = editor.text
            // Replaces entire content while preserving undo history
            try {
                // Ensure we are deleting everything from (0,0) to the last character
                val lastLineIndex = text.lineCount - 1
                val lastColumnIndex = text.getColumnCount(lastLineIndex)

                // If the file is empty, just insert
                if (text.isEmpty()) {
                    text.insert(0, 0, newContent)
                } else {
                    text.replace(0, 0, lastLineIndex, lastColumnIndex, newContent)
                }

                // Update the state as well, but rely on listener for content sync usually.
                // However, since we modify programmatically, the listener will trigger.
                // We just need to ensure savedContent is updated if we consider this "saved"
                // but usually "undo" implies it's an edit in the buffer.
                // If the user "Saved" in the config screen, they expect it to be on disk too.
                // So we should also update savedContent to avoid "unsaved" indicator.
                (openFiles.getOrNull(activeFileIndex) as? CodeEditorState)?.savedContent = newContent
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun formatCode() {
        if (isFormatting) return
        isFormatting = true
        val editor = getActiveEditor() ?: return
        val ext = openFiles[activeFileIndex].file.extension
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val original = editor.text.toString()
                val formatted =
                    com.scto.mobile.ide.core.utils.CodeFormatter.format(original, ext, editorConfig.tabWidth)
                if (formatted != original) {
                    withContext(Dispatchers.Main) {
                        editor.setText(formatted)
                        (openFiles[activeFileIndex] as CodeEditorState).content = formatted
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFormatting = false
            }
        }
    }

    fun getCursorPosition(): Pair<Int, Int> {
        val editor = getActiveEditor() ?: return Pair(1, 1)
        val cursor = editor.cursor
        return Pair(cursor.leftLine + 1, cursor.leftColumn + 1)
    }

    fun jumpTo(line: Int, column: Int) {
        val editor = getActiveEditor() ?: return
        try {
            // SoraEditor uses 0-based indexing for line and column
            editor.setSelection(line, column)
            editor.ensureSelectionVisible()
        } catch (_: Exception) {}
    }

    private fun setupLspForEditor(context: Context, state: CodeEditorState, editor: CodeEditor, language: Language?) {
        val fileExtension = state.file.extension.lowercase()
        if (
            fileExtension !in
                listOf(
                    "html",
                    "htm",
                    "css",
                    "js",
                    "javascript",
                    "php",
                    "c",
                    "h",
                    "cpp",
                    "hpp",
                    "glsl",
                    "vert",
                    "frag",
                    "json",
                    "ts",
                    "typescript",
                    "tsx",
                )
        )
            return
        val prefs = context.getSharedPreferences("MobileIDE_Editor_Settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("editor_lsp_enabled", false) || language == null) return

        try {
            if (lspProject == null) {
                val projectPath = File(context.filesDir, "lsp_workspace").apply { mkdirs() }.absolutePath
                lspProject = LspProject(projectPath)
                lspProject!!.init()
            }
            val fileName = "editor_${System.identityHashCode(state)}_${System.currentTimeMillis()}.${fileExtension}"
            val project = lspProject!!
            val realFile = File(project.projectUri.path, fileName)
            if (!realFile.exists()) realFile.writeText(state.content)

            if (!addedLspDefinitions.contains(fileExtension)) {
                val matchingServer =
                    (com.rk.lsp.LspRegistry.extensionServers + com.rk.lsp.LspRegistry.externalServers).find {
                        it.isSupported(realFile)
                    }

                val def =
                    if (
                        matchingServer != null && kotlinx.coroutines.runBlocking { matchingServer.isInstalled(context) }
                    ) {
                        val lspSettingsPrefs =
                            context.getSharedPreferences("MobileIDE_Lsp_Settings", Context.MODE_PRIVATE)
                        val isEnabled = lspSettingsPrefs.getBoolean("lsp_enabled_${matchingServer.id}", true)
                        if (isEnabled) {
                            val config = matchingServer.getConnectionConfig()
                            if (config is com.rk.lsp.LspConnectionConfig.Process) {
                                CustomLanguageServerDefinition(
                                    ext = fileExtension,
                                    serverConnectProvider = {
                                        ProotStreamConnectionProvider(context, config.command.toList())
                                    },
                                )
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                        ?: when (fileExtension) {
                            "html",
                            "htm" ->
                                CustomLanguageServerDefinition(
                                    ext = "html",
                                    serverConnectProvider = {
                                        ProotStreamConnectionProvider(
                                            context,
                                            listOf("vscode-html-language-server", "--stdio"),
                                        )
                                    },
                                )
                            "css" ->
                                CustomLanguageServerDefinition(
                                    ext = "css",
                                    serverConnectProvider = {
                                        ProotStreamConnectionProvider(
                                            context,
                                            listOf("vscode-css-language-server", "--stdio"),
                                        )
                                    },
                                )
                            "js",
                            "javascript",
                            "ts",
                            "typescript",
                            "tsx" ->
                                CustomLanguageServerDefinition(
                                    ext = "js",
                                    serverConnectProvider = {
                                        ProotStreamConnectionProvider(
                                            context,
                                            listOf("typescript-language-server", "--stdio"),
                                        )
                                    },
                                )
                            "php" ->
                                CustomLanguageServerDefinition(
                                    ext = "php",
                                    serverConnectProvider = {
                                        ProotStreamConnectionProvider(context, listOf("intelephense", "--stdio"))
                                    },
                                )
                            "c",
                            "h",
                            "cpp",
                            "hpp" ->
                                CustomLanguageServerDefinition(
                                    ext = fileExtension,
                                    serverConnectProvider = { ProotStreamConnectionProvider(context, listOf("clangd")) },
                                )
                            "glsl",
                            "vert",
                            "frag" ->
                                CustomLanguageServerDefinition(
                                    ext = fileExtension,
                                    serverConnectProvider = {
                                        ProotStreamConnectionProvider(
                                            context,
                                            listOf("glsl-language-server", "--stdio"),
                                        )
                                    },
                                )
                            "json" ->
                                CustomLanguageServerDefinition(
                                    ext = "json",
                                    serverConnectProvider = {
                                        ProotStreamConnectionProvider(
                                            context,
                                            listOf("vscode-json-language-server", "--stdio"),
                                        )
                                    },
                                )
                            "java" ->
                                CustomLanguageServerDefinition(
                                    ext = "java",
                                    serverConnectProvider = { ProotStreamConnectionProvider(context, listOf("jdtls")) },
                                )
                            "kt",
                            "kotlin" ->
                                CustomLanguageServerDefinition(
                                    ext = "kt",
                                    serverConnectProvider = {
                                        ProotStreamConnectionProvider(context, listOf("kotlin-language-server"))
                                    },
                                )
                            else -> null
                        }
                if (def != null) {
                    project.addServerDefinition(def)
                    addedLspDefinitions.add(fileExtension)
                }
            }

            val lspEditor = project.getOrCreateEditor(realFile.absolutePath)
            lspEditor.wrapperLanguage = language
            lspEditor.editor = editor
            state.lspEditor = lspEditor

            lspEditor.eventManager.addEventListener(
                object : EventListener {
                    override val eventName = "editor/publishDiagnostics"

                    override fun handle(context: EventContext) {
                        val data = context.getOrNull<List<Diagnostic>>("data")
                        if (data != null) {
                            state.diagnostics = data
                        }
                    }
                }
            )

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    lspEditor.connect()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        closeAllFiles()
    }
}
