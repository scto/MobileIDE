package com.mobileide.editor.presentation

// Hypothetische Datenklassen für LSP-Informationen
data class Diagnostic(val range: Range, val message: String, val severity: Int)
data class CompletionItem(val label: String, val detail: String)
data class Range(val start: Position, val end: Position)
data class Position(val line: Int, val character: Int)

/**
 * Repräsentiert eine geöffnete Datei im Editor mit ihrem Zustand.
 */
data class EditorFile(
    val path: String,
    val content: String
)

/**
 * Der Zustand des Editors verwaltet jetzt eine Liste von Dateien, Tabs und die Suche.
 */
data class EditorState(
    val openFiles: List<EditorFile> = emptyList(),
    val activeFileIndex: Int = -1,
    val isLoading: Boolean = false,
    val diagnostics: Map<String, List<Diagnostic>> = emptyMap(), // Diagnostics pro Dateipfad
    val completionItems: List<CompletionItem> = emptyList(),
    val searchTerm: String = "",
    val showSearchBar: Boolean = false
) {
    /**
     * Gibt die aktuell aktive Datei zurück.
     */
    val activeFile: EditorFile?
        get() = openFiles.getOrNull(activeFileIndex)

    /**
     * Gibt die Diagnosen für die aktuell aktive Datei zurück.
     */
    val activeFileDiagnostics: List<Diagnostic>
        get() = activeFile?.path?.let { diagnostics[it] } ?: emptyList()
}

/**
 * Aktionen, die vom Editor-Bildschirm ausgelöst werden können.
 */
sealed interface EditorEvent {
    data class FileOpened(val filePath: String) : EditorEvent
    data class FileClosed(val fileIndex: Int) : EditorEvent
    data class TabSelected(val fileIndex: Int) : EditorEvent
    data class ContentChanged(val newContent: String) : EditorEvent
    data class SearchTermChanged(val term: String) : EditorEvent
    object ToggleSearchBar : EditorEvent
    object FileSaved : EditorEvent
}
