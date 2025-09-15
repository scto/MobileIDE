package com.mobileide.debug.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.mobileide.debug.data.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val logRepository: LogRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DebugState())
    val state = _state.asStateFlow()

    init {
        logRepository.getLogs()
            .onEach { logs ->
                _state.update { it.copy(logs = logs) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: DebugEvent) {
        when (event) {
            is DebugEvent.FilterChanged -> {
                _state.update { it.copy(filter = event.filter) }
            }
        }
    }
}
