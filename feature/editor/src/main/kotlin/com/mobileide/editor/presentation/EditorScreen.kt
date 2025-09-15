package com.mobileide.editor.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel = hiltViewModel(),
    filePath: String?
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(filePath) {
        filePath?.let {
            if (state.openFiles.none { f -> f.path == it }) {
                viewModel.onEvent(EditorEvent.FileOpened(it))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.activeFile?.path?.substringAfterLast('/') ?: "Editor") },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(EditorEvent.ToggleSearchBar) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search in file")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.showSearchBar) {
                OutlinedTextField(
                    value = state.searchTerm,
                    onValueChange = { viewModel.onEvent(EditorEvent.SearchTermChanged(it)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    placeholder = { Text("Suchen im Dokument...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }

            if (state.openFiles.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = state.activeFileIndex,
                    edgePadding = 0.dp
                ) {
                    state.openFiles.forEachIndexed { index, file ->
                        Tab(
                            selected = state.activeFileIndex == index,
                            onClick = { viewModel.onEvent(EditorEvent.TabSelected(index)) },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(file.path.substringAfterLast('/'), style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.onEvent(EditorEvent.FileClosed(index)) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Tab", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        )
                    }
                }
            }

            val activeFile = state.activeFile
            if (activeFile != null) {
                // Die Transformation wird jetzt mit dem Suchbegriff aus dem State initialisiert.
                val visualTransformation = remember(state.searchTerm) {
                    CodeHighlightingTransformation(state.searchTerm)
                }

                BasicTextField(
                    value = activeFile.content,
                    onValueChange = { viewModel.onEvent(EditorEvent.ContentChanged(it)) },
                    visualTransformation = visualTransformation,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Keine Datei geöffnet. Öffne eine im Explorer.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
