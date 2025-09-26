package com.mobileide.git.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.mobileide.data.GitRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GitViewModel @Inject constructor(
    private val gitRepository: GitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GitState())
    val state = _state.asStateFlow()

    fun onEvent(event: GitEvent) {
        when (event) {
            is GitEvent.UrlChanged -> _state.update { it.copy(repoUrl = event.url) }
            is GitEvent.LocalPathChanged -> _state.update { it.copy(localPath = event.path) }
            is GitEvent.CommitMessageChanged -> _state.update { it.copy(commitMessage = event.message) }
            is GitEvent.StageFile -> stage(event.file)
            is GitEvent.UnstageFile -> unstage(event.file)
            is GitEvent.CloneClicked -> cloneRepository()
            is GitEvent.StatusClicked -> checkStatus()
            is GitEvent.CommitClicked -> commit()
            is GitEvent.PushClicked -> push()
            is GitEvent.PullClicked -> pull()
            is GitEvent.UserMessageShown -> _state.update { it.copy(userMessage = null) }
        }
    }

    private fun cloneRepository() { /* ... */ }

    private fun checkStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = gitRepository.getStatus(_state.value.localPath)
            result.onSuccess { statusResult ->
                _state.update { it.copy(isLoading = false, statusResult = statusResult) }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    private fun stage(file: String) {
        viewModelScope.launch {
            gitRepository.add(_state.value.localPath, file).onSuccess {
                checkStatus() 
            }.onFailure { e ->
                 _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    private fun unstage(file: String) {
        viewModelScope.launch {
            gitRepository.unstage(_state.value.localPath, file).onSuccess {
                checkStatus()
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    private fun commit() {
        viewModelScope.launch {
             _state.update { it.copy(isLoading = true, error = null) }
            gitRepository.commit(_state.value.localPath, _state.value.commitMessage)
                .onSuccess { commitHash ->
                    _state.update { it.copy(isLoading = false, commitMessage = "", userMessage = "Committed: ${commitHash.take(7)}") }
                    checkStatus()
                }.onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message, userMessage = "Commit failed: ${e.message}") }
                }
        }
    }
    
    private fun push() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            gitRepository.push(_state.value.localPath)
                .onSuccess { message ->
                    _state.update { it.copy(isLoading = false, userMessage = message) }
                }.onFailure { e ->
                    _state.update { it.copy(isLoading = false, userMessage = "Push failed: ${e.message}") }
                }
        }
    }

    private fun pull() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            gitRepository.pull(_state.value.localPath)
                .onSuccess { message ->
                    _state.update { it.copy(isLoading = false, userMessage = message) }
                    checkStatus() // Status nach Pull aktualisieren
                }.onFailure { e ->
                    _state.update { it.copy(isLoading = false, userMessage = "Pull failed: ${e.message}") }
                }
        }
    }
}
