package com.mobileide.editor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileide.data.FileRepository
import com.mobileide.languages.LanguageServerManager
import com.mobileide.lsp.LspClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val languageServerManager: LanguageServerManager,
    private val fileRepository: FileRepository // NEU: FileRepository für Speichern/Laden
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    private var lspClient: LspClient? = null
    private var lspNotificationJob: Job? = null

    fun onEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.FileOpened -> openFile(event.filePath)
            is EditorEvent.TabSelected -> selectTab(event.fileIndex)
            is EditorEvent.FileClosed -> closeTab(event.fileIndex)
            is EditorEvent.ContentChanged -> updateContent(event.newContent)
            is EditorEvent.ToggleSearchBar -> _state.update { it.copy(showSearchBar = !it.showSearchBar) }
            is EditorEvent.SearchTermChanged -> _state.update { it.copy(searchTerm = event.term) }
            is EditorEvent.FileSaved -> saveFile()
        }
    }

    private fun openFile(filePath: String) {
        val existingIndex = _state.value.openFiles.indexOfFirst { it.path == filePath }
        if (existingIndex != -1) {
            selectTab(existingIndex)
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            fileRepository.readFile(filePath)
                .onSuccess { content ->
                    val newFile = EditorFile(path = filePath, content = content)
                     _state.update {
                        val newFiles = it.openFiles + newFile
                        it.copy(
                            openFiles = newFiles,
                            activeFileIndex = newFiles.lastIndex,
                            isLoading = false
                        )
                    }
                    initializeLspSession(filePath)
                }
                .onFailure { error ->
                    // TODO: Fehler dem Nutzer anzeigen
                     _state.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun selectTab(index: Int) {
        if (index in _state.value.openFiles.indices && index != _state.value.activeFileIndex) {
            _state.update { it.copy(activeFileIndex = index) }
            val filePath = _state.value.openFiles[index].path
            initializeLspSession(filePath)
        }
    }

    private fun closeTab(index: Int) {
        if (index !in _state.value.openFiles.indices) return

        val closedFilePath = _state.value.openFiles[index].path
        _state.update {
            val newFiles = it.openFiles.toMutableList().also { list -> list.removeAt(index) }
            val newDiagnostics = it.diagnostics.toMutableMap().also { map -> map.remove(closedFilePath) }

            val newIndex = when {
                newFiles.isEmpty() -> -1
                it.activeFileIndex >= index && it.activeFileIndex > 0 -> it.activeFileIndex - 1
                else -> it.activeFileIndex
            }
            it.copy(openFiles = newFiles, activeFileIndex = newIndex, diagnostics = newDiagnostics)
        }
        // Wenn der neue Index gültig ist, starte die LSP-Sitzung neu
        _state.value.activeFile?.path?.let { initializeLspSession(it) }
    }

    private fun updateContent(newContent: String) {
        val activeIndex = _state.value.activeFileIndex
        if (activeIndex == -1) return

        _state.update {
            val updatedFiles = it.openFiles.toMutableList()
            updatedFiles[activeIndex] = it.openFiles[activeIndex].copy(content = newContent)
            it.copy(openFiles = updatedFiles)
        }
        // lspClient?.sendNotification("textDocument/didChange", ...)
    }

    private fun saveFile() {
        viewModelScope.launch {
            _state.value.activeFile?.let { fileToSave ->
                 fileRepository.saveFile(fileToSave.path, fileToSave.content)
                     .onSuccess {
                         // TODO: Erfolgsmeldung anzeigen (z.B. Snackbar)
                         println("Datei ${fileToSave.path} gespeichert!")
                     }
                     .onFailure {
                         // TODO: Fehlermeldung anzeigen
                     }
            }
        }
    }
    
    private fun initializeLspSession(filePath: String) {
        // Implementierung aus früheren Schritten
    }
}
