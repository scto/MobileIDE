package com.mobileide.feature.settings.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
    var localFontSize by remember(settings.fontSize) { mutableStateOf(settings.fontSize) }
    var localTabSize by remember(settings.tabSize) { mutableStateOf(settings.tabSize.toFloat()) }
    var localShowLineNumbers by remember(settings.showLineNumbers) { mutableStateOf(settings.showLineNumbers) }
    var localWordWrap by remember(settings.wordWrap) { mutableStateOf(settings.wordWrap) }
    var localAutoComplete by remember(settings.autoComplete) { mutableStateOf(settings.autoComplete) }
    var localAutoIndent by remember(settings.autoIndent) { mutableStateOf(settings.autoIndent) }
    var localStickyScroll by remember(settings.stickyScroll) { mutableStateOf(settings.stickyScroll) }
    var localHighlightLine by remember(settings.highlightLine) { mutableStateOf(settings.highlightLine) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            Text("Font Size: ${localFontSize.toInt()}")
            Slider(
                value = localFontSize,
                onValueChange = { localFontSize = it },
                onValueChangeFinished = { viewModel.updateFontSize(localFontSize) },
                valueRange = 8f..32f
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Tab Size: ${localTabSize.toInt()}")
            Slider(
                value = localTabSize,
                onValueChange = { localTabSize = it },
                onValueChangeFinished = { viewModel.updateTabSize(localTabSize.toInt()) },
                valueRange = 2f..8f,
                steps = 6
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsToggle("Show Line Numbers", localShowLineNumbers) {
                localShowLineNumbers = it
                viewModel.updateShowLineNumbers(it)
            }

            SettingsToggle("Word Wrap", localWordWrap) {
                localWordWrap = it
                viewModel.updateWordWrap(it)
            }

            SettingsToggle("Auto Complete", localAutoComplete) {
                localAutoComplete = it
                viewModel.updateAutoComplete(it)
            }

            SettingsToggle("Auto Indent", localAutoIndent) {
                localAutoIndent = it
                viewModel.updateAutoIndent(it)
            }

            SettingsToggle("Sticky Scroll", localStickyScroll) {
                localStickyScroll = it
                viewModel.updateStickyScroll(it)
            }

            SettingsToggle("Highlight Current Line", localHighlightLine) {
                localHighlightLine = it
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
