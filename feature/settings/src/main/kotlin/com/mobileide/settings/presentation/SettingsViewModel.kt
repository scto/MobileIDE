package com.mobileide.settings.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // Hier wird später ein SettingsRepository injiziert
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnThemeChanged -> {
                _state.update {
                    it.copy(settings = it.settings.copy(theme = event.theme))
                }
                // Hier würde man die Einstellung im Repository speichern
                // viewModelScope.launch { settingsRepository.setTheme(event.theme) }
            }
        }
    }
}
