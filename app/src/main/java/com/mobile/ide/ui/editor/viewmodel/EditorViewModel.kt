// Copyright 2025 Thomas Schmid
package com.mobile.ide.ui.editor.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.ide.R
import com.mobile.ide.core.utils.LogCatcher
import com.mobile.ide.core.utils.PermissionManager
import com.mobile.ide.textmate.TextMateLanguage
import com.mobile.ide.ui.editor.EditorColorSchemeManager
import com.mobile.ide.ui.editor.components.TextMateInitializer
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CodeEditorState(val file: File, val languageScopeName: String) {
    var content by mutableStateOf("")
    private var savedContent by mutableStateOf("")
    val isModified: Boolean
        get() = content != savedContent

    fun onContentLoaded(loadedContent: String) {
        content = loadedContent
        savedContent = loadedContent
    }

    fun onContentSaved() {
        savedContent = content
    }
}

class EditorViewModel : ViewModel() {
    var hasShownInitialLoader by mutableStateOf(false)
        private set

    var openFiles by mutableStateOf<List<CodeEditorState>>(emptyList())
        private set

    var activeFileIndex by mutableStateOf(-1)
        private set

    var currentProjectPath by mutableStateOf<String?>(null)
        private set

    private val editorInstances = mutableMapOf<String, CodeEditor>()
    private val supportedLanguageScopes = setOf("text.html.basic", "source.css", "source.js")

    private var hasPermissions = false
    private lateinit var appContext: Context

    fun initializePermissions(context: Context) {
        appContext = context.applicationContext
        hasPermissions = PermissionManager.hasRequiredPermissions(appContext)
        LogCatcher.permission(
            "EditorViewModel",
            "Initialization",
            if (hasPermissions) "Permissions granted" else "Need to request permissions",
        )
    }

    private fun checkPermissions(operation: String): Boolean {
        if (!hasPermissions) {
            LogCatcher.w("EditorViewModel", "Insufficient permissions - Operation: $operation")
            return false
        }
        return true
    }

    fun onInitialLoaderShown() {
        hasShownInitialLoader = true
    }

    fun updateEditorTheme(seedColor: Color, isDark: Boolean) {
        editorInstances.values.forEach { editor ->
            val currentScheme = editor.colorScheme
            EditorColorSchemeManager.applyThemeColors(currentScheme, seedColor, isDark)
        }
    }

    @Synchronized
    fun getOrCreateEditor(context: Context, state: CodeEditorState): CodeEditor {
        val filePath = state.file.absolutePath
        editorInstances[filePath]?.let { existingEditor ->
            (existingEditor.parent as? android.view.ViewGroup)?.removeView(existingEditor)
            return existingEditor
        }
        if (!TextMateInitializer.isReady()) {
            TextMateInitializer.initialize(context)
        }
        val editor =
            CodeEditor(context).apply {
                setText(state.content)
                colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
                if (state.languageScopeName in supportedLanguageScopes) {
                    try {
                        val language = TextMateLanguage.create(state.languageScopeName, true)
                        setEditorLanguage(language)
                    } catch (e: Exception) {
                        LogCatcher.e("EditorViewModel", "Failed to set language: ${state.languageScopeName}", e)
                    }
                }
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
                            if (state.content != newText) {
                                state.content = newText
                            }
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
                            if (state.content != newText) {
                                state.content = newText
                            }
                        }
                    }
                )
            }
        editorInstances[filePath] = editor
        return editor
    }

    override fun onCleared() {
        super.onCleared()
        editorInstances.values.forEach {
            try {
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        editorInstances.clear()
    }

    fun loadInitialFile(projectPath: String) {
        if (projectPath != currentProjectPath) {
            closeAllFiles()
            currentProjectPath = projectPath
            val indexFile = File(projectPath, "index.html")
            LogCatcher.d("EditorViewModel", "Attempting to load initial file: ${indexFile.absolutePath}")
            if (indexFile.exists() && indexFile.isFile && indexFile.canRead()) {
                if (checkPermissions("Load initial file")) {
                    LogCatcher.fileOperation("EditorViewModel", "Load initial file", indexFile.absolutePath, "Start")
                    openFile(indexFile)
                } else {
                    LogCatcher.fileOperation(
                        "EditorViewModel",
                        "Load initial file",
                        indexFile.absolutePath,
                        "Insufficient permissions",
                    )
                }
            }
        }
    }

    suspend fun saveAllModifiedFiles(context: Context) {
        withContext(Dispatchers.IO) {
            val modifiedFiles = openFiles.filter { it.isModified }
            if (modifiedFiles.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.msg_no_files_to_save), Toast.LENGTH_SHORT).show()
                }
                return@withContext
            }

            if (!checkPermissions("Save file")) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.msg_storage_perm_required), Toast.LENGTH_LONG)
                        .show()
                }
                return@withContext
            }

            var successCount = 0
            modifiedFiles.forEach { state ->
                try {
                    if (!state.file.canWrite()) {
                        LogCatcher.e("EditorViewModel", "File is not writable: ${state.file.absolutePath}")
                        return@forEach
                    }
                    state.file.outputStream().use { output ->
                        output.bufferedWriter(Charsets.UTF_8).use { writer -> writer.write(state.content) }
                    }
                    state.onContentSaved()
                    successCount++
                    LogCatcher.fileOperation("EditorViewModel", "Save file", state.file.absolutePath, "Success")
                } catch (e: Exception) {
                    LogCatcher.e("EditorViewModel", "Save failed", e)
                    LogCatcher.fileOperation(
                        "EditorViewModel",
                        "Save file",
                        state.file.absolutePath,
                        "Failed: ${e.message}",
                    )
                }
            }
            withContext(Dispatchers.Main) {
                if (successCount > 0) {
                    Toast.makeText(
                            context,
                            context.getString(R.string.msg_files_saved, successCount),
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                }
            }
        }
    }

    suspend fun buildHtmlContentFromProject(projectPath: String): String {
        return withContext(Dispatchers.IO) {
            val htmlFile = File(projectPath, "index.html")
            val cssFile = findFileByExtensions(projectPath, listOf("css"))
            val jsFile = findFileByExtensions(projectPath, listOf("js"))
            fun safeReadFile(file: File?): String {
                if (file == null || !file.exists() || !file.canRead()) return ""
                return try {
                    file.readText(Charsets.UTF_8)
                } catch (e: Exception) {
                    LogCatcher.e("EditorViewModel", "Failed to read file", e)
                    ""
                }
            }
            val htmlContent = if (htmlFile.exists()) safeReadFile(htmlFile) else "<h1>Error</h1>"
            val cssContent = safeReadFile(cssFile)
            val jsContent = safeReadFile(jsFile)
            """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width, initial-scale=1"><style>$cssContent</style></head><body>$htmlContent<script>$jsContent</script></body></html>"""
                .trimIndent()
        }
    }

    private fun findFileByExtensions(projectPath: String, extensions: List<String>): File? {
        val projectDir = File(projectPath)
        for (ext in extensions) {
            val commonNames =
                when (ext) {
                    "html",
                    "htm" -> listOf("index.$ext")
                    "css" -> listOf("style.$ext", "styles.$ext")
                    "js" -> listOf("script.$ext", "main.$ext", "index.$ext")
                    else -> emptyList()
                }
            for (name in commonNames) {
                val file = File(projectDir, name)
                if (file.exists()) return file
            }
        }
        return projectDir.listFiles { _, name -> extensions.any { name.endsWith(".$it") } }?.firstOrNull()
    }

    fun openFile(file: File) {
        if (file.isDirectory || !file.exists() || !file.canRead()) return
        viewModelScope.launch {
            val existingIndex = openFiles.indexOfFirst { it.file.absolutePath == file.absolutePath }
            if (existingIndex != -1) {
                activeFileIndex = existingIndex
                LogCatcher.fileOperation("EditorViewModel", "Switch file", file.absolutePath, "Already exists")
            } else {
                val content =
                    withContext(Dispatchers.IO) {
                        try {
                            if (file.length() > 1024 * 1024) {
                                LogCatcher.w("EditorViewModel", "File too large: ${file.absolutePath}")
                                "File too large"
                            } else {
                                file.readText(Charsets.UTF_8)
                            }
                        } catch (e: Exception) {
                            LogCatcher.e("EditorViewModel", "Failed to read file", e)
                            "Cannot read file: ${e.message}"
                        }
                    }
                val language = getLanguageScope(file.extension)
                val newState = CodeEditorState(file = file, languageScopeName = language)
                newState.onContentLoaded(content)
                openFiles = openFiles + newState
                activeFileIndex = openFiles.lastIndex
                LogCatcher.fileOperation("EditorViewModel", "Open file", file.absolutePath, "Success")
            }
        }
    }

    fun openFileWithPermissionCheck(file: File, context: Context) {
        if (!checkPermissions("Open file")) {
            LogCatcher.fileOperation("EditorViewModel", "Open file", file.absolutePath, "Insufficient permissions")
            return
        }

        LogCatcher.fileOperation("EditorViewModel", "Open file", file.absolutePath, "Start")
        openFile(file)
    }

    fun undo() {
        openFiles.getOrNull(activeFileIndex)?.let { state -> editorInstances[state.file.absolutePath]?.undo() }
    }

    fun redo() {
        openFiles.getOrNull(activeFileIndex)?.let { state -> editorInstances[state.file.absolutePath]?.redo() }
    }

    fun insertSymbol(symbol: String) {
        openFiles.getOrNull(activeFileIndex)?.let { state ->
            editorInstances[state.file.absolutePath]?.let { editor ->
                val startLine = editor.cursor.leftLine
                val startColumn = editor.cursor.leftColumn
                val processedSymbol = if (symbol == "Tab") "\t" else symbol

                editor.text.insert(startLine, startColumn, processedSymbol)

                val newLineCount = processedSymbol.count { it == '\n' }
                if (newLineCount > 0) {
                    val lastLineText = processedSymbol.substringAfterLast('\n')
                    editor.setSelection(startLine + newLineCount, lastLineText.length)
                } else {
                    editor.setSelection(startLine, startColumn + processedSymbol.length)
                }
            }
        }
    }

    fun changeActiveFileIndex(index: Int) {
        if (index in openFiles.indices) activeFileIndex = index
    }

    fun closeAllFiles() {
        openFiles.forEach { state -> editorInstances.remove(state.file.absolutePath)?.release() }
        openFiles = emptyList()
        activeFileIndex = -1
    }

    fun closeOtherFiles(indexToKeep: Int) {
        if (indexToKeep !in openFiles.indices) return
        openFiles.forEachIndexed { index, state ->
            if (index != indexToKeep) editorInstances.remove(state.file.absolutePath)?.release()
        }
        openFiles = listOf(openFiles[indexToKeep])
        activeFileIndex = 0
    }

    fun closeFile(indexToClose: Int) {
        if (indexToClose !in openFiles.indices) return
        openFiles.getOrNull(indexToClose)?.file?.absolutePath?.let { path -> editorInstances.remove(path)?.release() }
        openFiles = openFiles.toMutableList().also { it.removeAt(indexToClose) }
        if (openFiles.isEmpty()) {
            activeFileIndex = -1
        } else if (activeFileIndex >= indexToClose) {
            activeFileIndex = (activeFileIndex - 1).coerceAtLeast(0)
        }
    }

    private fun getLanguageScope(extension: String): String =
        when (extension.lowercase()) {
            "html",
            "htm" -> "text.html.basic"
            "css" -> "source.css"
            "js" -> "source.js"
            else -> "text.plain"
        }
}
