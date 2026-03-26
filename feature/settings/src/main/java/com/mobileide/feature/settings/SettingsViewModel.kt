package com.mobileide.feature.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EditorSettings(
    val fontSize: Float = 14f,
    val tabSize: Int = 4,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val autoComplete: Boolean = true,
    val autoIndent: Boolean = true,
    val stickyScroll: Boolean = false,
    val highlightLine: Boolean = true
)

data class Keybinding(
    val commandId: String,
    val keyCombination: String
)

class SettingsViewModel : ViewModel() {

    private val _editorSettings = MutableStateFlow(EditorSettings())
    val editorSettings: StateFlow<EditorSettings> = _editorSettings.asStateFlow()

    private val _appTheme = MutableStateFlow("System Default")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _keybindings = MutableStateFlow(
        listOf(
            Keybinding("editor.action.save", "Ctrl+S"),
            Keybinding("editor.action.find", "Ctrl+F"),
            Keybinding("editor.action.replace", "Ctrl+H"),
            Keybinding("editor.action.commentLine", "Ctrl+/")
        )
    )
    val keybindings: StateFlow<List<Keybinding>> = _keybindings.asStateFlow()

    fun updateFontSize(size: Float) {
        _editorSettings.update { it.copy(fontSize = size) }
    }

    fun updateTabSize(size: Int) {
        _editorSettings.update { it.copy(tabSize = size) }
    }

    fun updateShowLineNumbers(enabled: Boolean) {
        _editorSettings.update { it.copy(showLineNumbers = enabled) }
    }

    fun updateWordWrap(enabled: Boolean) {
        _editorSettings.update { it.copy(wordWrap = enabled) }
    }

    fun updateAutoComplete(enabled: Boolean) {
        _editorSettings.update { it.copy(autoComplete = enabled) }
    }

    fun updateAutoIndent(enabled: Boolean) {
        _editorSettings.update { it.copy(autoIndent = enabled) }
    }

    fun updateStickyScroll(enabled: Boolean) {
        _editorSettings.update { it.copy(stickyScroll = enabled) }
    }

    fun updateHighlightLine(enabled: Boolean) {
        _editorSettings.update { it.copy(highlightLine = enabled) }
    }

    fun updateAppTheme(theme: String) {
        _appTheme.value = theme
    }
}
