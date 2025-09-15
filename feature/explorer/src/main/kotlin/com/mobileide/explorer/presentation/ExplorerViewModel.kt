package com.mobileide.explorer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileide.data.FileRepository // <-- KORREKTER IMPORT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExplorerState())
    val state = _state.asStateFlow()

    init {
        loadFiles(_state.value.currentPath)
    }

    fun onEvent(event: ExplorerEvent) {
        when (event) {
            is ExplorerEvent.OpenDirectory -> loadFiles(event.path)
            is ExplorerEvent.NavigateUp -> {
                val parentPath = File(_state.value.currentPath).parent
                if (parentPath != null && parentPath.length > 1) { 
                    loadFiles(parentPath)
                }
            }
        }
    }

    private fun loadFiles(path: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            fileRepository.getFiles(path)
                .onSuccess { files ->
                    _state.update {
                        it.copy(isLoading = false, files = files, currentPath = path)
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
