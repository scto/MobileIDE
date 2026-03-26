package com.mobileide.feature.editor

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.UUID

class FeatureEditorViewModel(application: Application) : AndroidViewModel(application) {

    // ── Tabs ──────────────────────────────────────────────────────────────────
    private val _tabs = MutableStateFlow<List<FeatureEditorTab>>(emptyList())
    val tabs: StateFlow<List<FeatureEditorTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    // ── Settings ──────────────────────────────────────────────────────────────
    private val _settings = MutableStateFlow(FeatureEditorSettings())
    val settings: StateFlow<FeatureEditorSettings> = _settings.asStateFlow()

    // ── Search ────────────────────────────────────────────────────────────────
    private val _searchOptions  = MutableStateFlow(FeatureSearchOptions())
    val searchOptions: StateFlow<FeatureSearchOptions> = _searchOptions.asStateFlow()

    private val _searchResults  = MutableStateFlow<List<FeatureSearchResult>>(emptyList())
    val searchResults: StateFlow<List<FeatureSearchResult>> = _searchResults.asStateFlow()

    private val _showSearchPanel  = MutableStateFlow(false)
    val showSearchPanel: StateFlow<Boolean> = _showSearchPanel.asStateFlow()

    private val _showReplacePanel = MutableStateFlow(false)
    val showReplacePanel: StateFlow<Boolean> = _showReplacePanel.asStateFlow()

    // ── UI state ──────────────────────────────────────────────────────────────
    private val _statusMessage  = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isLoading      = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showLanguageDialog = MutableStateFlow(false)
    val showLanguageDialog: StateFlow<Boolean> = _showLanguageDialog.asStateFlow()

    private val _showGotoLineDialog = MutableStateFlow(false)
    val showGotoLineDialog: StateFlow<Boolean> = _showGotoLineDialog.asStateFlow()

    // ── Bookmarks ─────────────────────────────────────────────────────────────
    private val _bookmarks = MutableStateFlow<List<FeatureBookmark>>(emptyList())
    val bookmarks: StateFlow<List<FeatureBookmark>> = _bookmarks.asStateFlow()

    // ── Undo / Redo ───────────────────────────────────────────────────────────
    private val undoStack = mutableMapOf<String, ArrayDeque<String>>()
    private val redoStack = mutableMapOf<String, ArrayDeque<String>>()

    init { createNewTab() }

    // ── Tab management ────────────────────────────────────────────────────────

    fun createNewTab(
        fileName: String = "untitled.txt",
        content: String = "",
        filePath: String? = null,
        language: FeatureEditorLanguage? = null,
    ): String {
        val id  = UUID.randomUUID().toString()
        val tab = FeatureEditorTab(
            id       = id,
            fileName = fileName,
            filePath = filePath,
            content  = content,
            language = language
                ?: filePath?.let { FeatureEditorLanguage.fromFileName(it.substringAfterLast('/')) }
                ?: FeatureEditorLanguage.fromFileName(fileName),
        )
        _tabs.update { it + tab }
        _activeTabId.value = id
        undoStack[id] = ArrayDeque()
        redoStack[id] = ArrayDeque()
        return id
    }

    fun closeTab(tabId: String) {
        val idx = _tabs.value.indexOfFirst { it.id == tabId }.takeIf { it >= 0 } ?: return
        _tabs.update { it.filter { t -> t.id != tabId } }
        undoStack.remove(tabId); redoStack.remove(tabId)
        if (_activeTabId.value == tabId) {
            val tabs = _tabs.value
            _activeTabId.value = when {
                tabs.isEmpty()  -> { createNewTab(); _tabs.value.firstOrNull()?.id }
                idx > 0         -> tabs[idx - 1].id
                else            -> tabs.firstOrNull()?.id
            }
        }
    }

    fun setActiveTab(id: String) { _activeTabId.value = id }
    fun getActiveTab() = _tabs.value.firstOrNull { it.id == _activeTabId.value }

    fun updateTabContent(tabId: String, content: String) {
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        undoStack.getOrPut(tabId) { ArrayDeque() }.apply {
            addLast(tab.content)
            while (size > 100) removeFirst()
        }
        redoStack[tabId]?.clear()
        _tabs.update { tabs -> tabs.map { if (it.id == tabId) it.copy(content = content, isModified = true) else it } }
    }

    fun updateCursorPosition(tabId: String, line: Int, col: Int) {
        _tabs.update { tabs -> tabs.map { if (it.id == tabId) it.copy(cursorLine = line, cursorColumn = col) else it } }
    }

    // ── File I/O ──────────────────────────────────────────────────────────────

    fun openFileFromUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val ctx = getApplication<Application>()
                val (name, text) = withContext(Dispatchers.IO) {
                    val n = getFileName(ctx, uri) ?: "unknown.txt"
                    val t = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                    n to t
                }
                createNewTab(fileName = name, content = text, filePath = uri.toString())
                showStatus("Geöffnet: $name")
            } catch (e: Exception) {
                showStatus("Fehler: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveActiveTab(uri: Uri? = null) {
        viewModelScope.launch {
            val tab = getActiveTab() ?: return@launch
            _isLoading.value = true
            try {
                val ctx = getApplication<Application>()
                withContext(Dispatchers.IO) {
                    val target = uri ?: tab.filePath?.let { Uri.parse(it) }
                    if (target != null) {
                        ctx.contentResolver.openOutputStream(target, "wt")?.use {
                            it.write(tab.content.toByteArray(Charsets.UTF_8))
                        }
                    } else {
                        java.io.File(ctx.filesDir, tab.fileName).also {
                            FileOutputStream(it).use { s -> s.write(tab.content.toByteArray()) }
                        }
                    }
                }
                _tabs.update { tabs -> tabs.map { if (it.id == tab.id) it.copy(isModified = false) else it } }
                showStatus("Gespeichert: ${tab.fileName}")
            } catch (e: Exception) {
                showStatus("Speicherfehler: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Undo / Redo ───────────────────────────────────────────────────────────

    fun undo(tabId: String) {
        val prev = undoStack[tabId]?.removeLastOrNull() ?: return
        val cur  = _tabs.value.firstOrNull { it.id == tabId }?.content ?: return
        redoStack.getOrPut(tabId) { ArrayDeque() }.addLast(cur)
        _tabs.update { tabs -> tabs.map { if (it.id == tabId) it.copy(content = prev) else it } }
    }

    fun redo(tabId: String) {
        val next = redoStack[tabId]?.removeLastOrNull() ?: return
        val cur  = _tabs.value.firstOrNull { it.id == tabId }?.content ?: return
        undoStack.getOrPut(tabId) { ArrayDeque() }.addLast(cur)
        _tabs.update { tabs -> tabs.map { if (it.id == tabId) it.copy(content = next) else it } }
    }

    fun canUndo(tabId: String) = (undoStack[tabId]?.size ?: 0) > 0
    fun canRedo(tabId: String) = (redoStack[tabId]?.size ?: 0) > 0

    // ── Search ────────────────────────────────────────────────────────────────

    fun updateSearchOptions(opts: FeatureSearchOptions) {
        _searchOptions.value = opts
        performSearch(opts)
    }

    private fun performSearch(opts: FeatureSearchOptions) {
        if (opts.query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch(Dispatchers.Default) {
            val content = getActiveTab()?.content ?: return@launch
            val results = mutableListOf<FeatureSearchResult>()
            content.lines().forEachIndexed { li, line ->
                val src = if (opts.caseSensitive) line else line.lowercase()
                val qry = if (opts.caseSensitive) opts.query else opts.query.lowercase()
                var start = 0
                while (start < src.length) {
                    val idx = if (opts.useRegex) runCatching { Regex(qry).find(src, start)?.range?.first ?: -1 }.getOrDefault(-1)
                              else src.indexOf(qry, start)
                    if (idx < 0) break
                    val len = if (opts.useRegex) runCatching { Regex(qry).find(src, idx)?.value?.length ?: opts.query.length }.getOrDefault(opts.query.length)
                              else opts.query.length
                    results += FeatureSearchResult(li, idx, len, line.trim().take(60))
                    start = idx + len
                }
            }
            _searchResults.value = results
        }
    }

    fun replaceNext() {
        val tab = getActiveTab() ?: return
        val opts = _searchOptions.value
        if (opts.query.isEmpty()) return
        val new = if (opts.useRegex) runCatching {
            val rx = if (opts.caseSensitive) Regex(opts.query) else Regex(opts.query, RegexOption.IGNORE_CASE)
            rx.replaceFirst(tab.content, opts.replacement)
        }.getOrDefault(tab.content) else {
            val idx = if (opts.caseSensitive) tab.content.indexOf(opts.query)
                      else tab.content.lowercase().indexOf(opts.query.lowercase())
            if (idx >= 0) tab.content.substring(0, idx) + opts.replacement + tab.content.substring(idx + opts.query.length)
            else tab.content
        }
        updateTabContent(tab.id, new)
        performSearch(opts)
    }

    fun replaceAll() {
        val tab = getActiveTab() ?: return
        val opts = _searchOptions.value
        if (opts.query.isEmpty()) return
        val count = _searchResults.value.size
        val new = if (opts.useRegex) runCatching {
            val rx = if (opts.caseSensitive) Regex(opts.query) else Regex(opts.query, RegexOption.IGNORE_CASE)
            rx.replace(tab.content, opts.replacement)
        }.getOrDefault(tab.content)
        else tab.content.replace(opts.query, opts.replacement, ignoreCase = !opts.caseSensitive)
        updateTabContent(tab.id, new)
        _searchResults.value = emptyList()
        showStatus("$count Vorkommen ersetzt")
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun updateSettings(s: FeatureEditorSettings) { _settings.value = s }
    fun setLanguage(tabId: String, lang: FeatureEditorLanguage) {
        _tabs.update { tabs -> tabs.map { if (it.id == tabId) it.copy(language = lang) else it } }
    }
    fun toggleWordWrap()    = _settings.update { it.copy(wordWrap = !it.wordWrap) }
    fun toggleLineNumbers() = _settings.update { it.copy(showLineNumbers = !it.showLineNumbers) }

    // ── Bookmarks ─────────────────────────────────────────────────────────────

    fun addBookmark(line: Int, label: String = "") {
        val tabId = _activeTabId.value ?: return
        _bookmarks.update { it + FeatureBookmark(tabId, line, label) }
        showStatus("Lesezeichen: Zeile ${line + 1}")
    }
    fun removeBookmark(b: FeatureBookmark) = _bookmarks.update { it - b }
    fun getActiveBookmarks() = _bookmarks.value.filter { it.tabId == _activeTabId.value }

    // ── Text helpers ──────────────────────────────────────────────────────────

    fun formatCode() {
        val tab = getActiveTab() ?: return
        updateTabContent(tab.id, tab.content.lines().joinToString("\n") { it.trimEnd() })
        showStatus("Formatiert")
    }

    fun getWordCount(): Triple<Int, Int, Int> {
        val c = getActiveTab()?.content ?: return Triple(0, 0, 0)
        return Triple(c.lines().size, if (c.isBlank()) 0 else c.trim().split(Regex("\\s+")).size, c.length)
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    fun showSearch()   { _showSearchPanel.value = true }
    fun hideSearch()   { _showSearchPanel.value = false; _showReplacePanel.value = false; _searchResults.value = emptyList() }
    fun toggleReplace() = _showReplacePanel.update { !it }
    fun showLanguageSelector() { _showLanguageDialog.value = true }
    fun hideLanguageSelector() { _showLanguageDialog.value = false }
    fun showGotoLine() { _showGotoLineDialog.value = true }
    fun hideGotoLine() { _showGotoLineDialog.value = false }

    fun showStatus(msg: String) {
        _statusMessage.value = msg
        viewModelScope.launch { kotlinx.coroutines.delay(3000); _statusMessage.value = null }
    }

    private fun getFileName(ctx: Context, uri: Uri): String? =
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            c.moveToFirst(); if (i >= 0) c.getString(i) else null
        }
}
