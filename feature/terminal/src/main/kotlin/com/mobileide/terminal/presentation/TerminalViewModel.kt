package com.mobileide.terminal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.mobileide.terminal.data.TerminalRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val terminalRepository: TerminalRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TerminalState())
    val state = _state.asStateFlow()

    fun onEvent(event: TerminalEvent) {
        when (event) {
            is TerminalEvent.ExecuteCommand -> execute(event.command)
        }
    }

    private fun execute(command: String) {
        val newLines = mutableListOf<String>()
        newLines.add("> $command") // Zeige den eingegebenen Befehl an

        terminalRepository.execute(command, _state.value.workingDir)
            .onStart {
                _state.update { it.copy(isExecuting = true) }
            }
            .onEach { line ->
                newLines.add(line)
                _state.update { it.copy(outputLines = it.outputLines + newLines) }
                newLines.clear() // Verhindert doppeltes Hinzufügen
            }
            .launchIn(viewModelScope)
    }
}
