package com.mobileide.feature.settings.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileide.feature.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.editorSettings.collectAsState()

    // Using local state to handle sliders/toggles before saving
    var fontSize by remember(settings.fontSize) { mutableStateOf(settings.fontSize) }
    var tabSize by remember(settings.tabSize) { mutableStateOf(settings.tabSize.toFloat()) }
    var showLineNumbers by remember(settings.showLineNumbers) { mutableStateOf(settings.showLineNumbers) }
    var wordWrap by remember(settings.wordWrap) { mutableStateOf(settings.wordWrap) }
    var autoComplete by remember(settings.autoComplete) { mutableStateOf(settings.autoComplete) }
    var autoIndent by remember(settings.autoIndent) { mutableStateOf(settings.autoIndent) }
    var stickyScroll by remember(settings.stickyScroll) { mutableStateOf(settings.stickyScroll) }
    var highlightLine by remember(settings.highlightLine) { mutableStateOf(settings.highlightLine) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Use a standard back icon here
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Font Size: ${fontSize.toInt()}")
            Slider(
                value = fontSize,
                onValueChange = { fontSize = it },
                onValueChangeFinished = { viewModel.updateFontSize(fontSize) },
                valueRange = 8f..32f
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Tab Size: ${tabSize.toInt()}")
            Slider(
                value = tabSize,
                onValueChange = { tabSize = it },
                onValueChangeFinished = { viewModel.updateTabSize(tabSize.toInt()) },
                valueRange = 2f..8f,
                steps = 6
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsToggle("Show Line Numbers", showLineNumbers) {
                showLineNumbers = it
                viewModel.updateShowLineNumbers(it)
            }

            SettingsToggle("Word Wrap", wordWrap) {
                wordWrap = it
                viewModel.updateWordWrap(it)
            }

            SettingsToggle("Auto Complete", autoComplete) {
                autoComplete = it
                viewModel.updateAutoComplete(it)
            }

            SettingsToggle("Auto Indent", autoIndent) {
                autoIndent = it
                viewModel.updateAutoIndent(it)
            }

            SettingsToggle("Sticky Scroll", stickyScroll) {
                stickyScroll = it
                viewModel.updateStickyScroll(it)
            }

            SettingsToggle("Highlight Current Line", highlightLine) {
                highlightLine = it
                viewModel.updateHighlightLine(it)
            }
        }
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}