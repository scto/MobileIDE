package com.scto.mobile.ide.core.tooling.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCodingToolingPanel(
    projectPath: String,
    initialPrompt: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedProvider by remember { mutableStateOf(AiderProvider.GEMINI_FLASH) }
    var availableModels by remember(selectedProvider) { mutableStateOf(AiderModelCatalog.getModelsForProvider(selectedProvider)) }
    var selectedModel by remember(selectedProvider) { mutableStateOf(availableModels.first()) }
    var selectedMode by remember { mutableStateOf(AiderChatMode.AUTO) }

    var isSubtreeOnly by remember { mutableStateOf(false) }
    var isBrowser by remember { mutableStateOf(false) }

    var promptInput by remember { mutableStateOf(initialPrompt) }
    val chatOutput = remember { mutableStateListOf<String>() }
    var isRunning by remember { mutableStateOf(false) }

    val defaultContextFiles = listOf("activeDevelopment.md", "progress.md", "systemDesign.md", "testStrategy.md", "uiStrategy.md", "techEnvironment.md")
    val selectedContextFiles = remember { mutableStateListOf<String>().apply { addAll(defaultContextFiles) } }

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var currentApiKeyInput by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    LaunchedEffect(chatOutput.size) {
        if (chatOutput.isNotEmpty()) {
            listState.animateScrollToItem(chatOutput.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AiderProvider.entries.forEach { provider ->
                FilterChip(
                    selected = selectedProvider == provider,
                    onClick = {
                        selectedProvider = provider
                        availableModels = AiderModelCatalog.getModelsForProvider(provider)
                        selectedModel = availableModels.first()
                    },
                    label = { Text(provider.displayName, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            var modelMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { modelMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedModel.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                DropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = { modelMenuExpanded = false }
                ) {
                    availableModels.forEach { modelSpec ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(modelSpec.name, fontWeight = FontWeight.Bold)
                                    Text(modelSpec.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                selectedModel = modelSpec
                                if (modelSpec.isArchitectDefault) selectedMode = AiderChatMode.ARCHITECT
                                modelMenuExpanded = false
                            }
                        )
                    }
                }
            }

            var modeMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { modeMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Modus: ${selectedMode.displayName}", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                DropdownMenu(
                    expanded = modeMenuExpanded,
                    onDismissRequest = { modeMenuExpanded = false }
                ) {
                    AiderChatMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(mode.displayName, fontWeight = FontWeight.Bold)
                                    Text(mode.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                selectedMode = mode
                                modeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            IconButton(onClick = {
                currentApiKeyInput = AiderBridgeService.getApiKey(context, selectedProvider)
                showApiKeyDialog = true
            }) {
                Icon(Icons.Default.VpnKey, contentDescription = "API-Key Konfigurieren", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = isSubtreeOnly,
                onClick = { isSubtreeOnly = !isSubtreeOnly },
                label = { Text("Sub-Tree Only (--subtree-only)", style = MaterialTheme.typography.labelSmall) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            if (selectedModel.isBrowserSupported) {
                FilterChip(
                    selected = isBrowser,
                    onClick = { isBrowser = !isBrowser },
                    label = { Text("Browser Mode (--browser)", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            if (chatOutput.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Schreibe einen Prompt, um Aider (${selectedModel.name}) zu starten...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(chatOutput) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Was möchtest du mit KI entwickeln/refaktoren?") },
                modifier = Modifier.weight(1f),
                singleLine = false,
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = {
                    if (promptInput.isNotBlank() && !isRunning) {
                        val prompt = promptInput
                        promptInput = ""
                        isRunning = true
                        chatOutput.add("\n👤 User: $prompt\n")

                        coroutineScope.launch {
                            AiderBridgeService.executeAiderStream(
                                context = context,
                                projectPath = projectPath,
                                model = selectedModel,
                                chatMode = selectedMode,
                                isSubtreeOnly = isSubtreeOnly,
                                isBrowser = isBrowser,
                                message = prompt,
                                selectedContextFiles = selectedContextFiles
                            ).onStart {
                                isRunning = true
                            }.onCompletion {
                                isRunning = false
                            }.catch { e ->
                                chatOutput.add("❌ Fehler: ${e.message}\n")
                                isRunning = false
                            }.collect { outputLine ->
                                chatOutput.add(outputLine)
                            }
                        }
                    }
                },
                enabled = promptInput.isNotBlank() && !isRunning
            ) {
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Senden")
                }
            }
        }
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("${selectedProvider.displayName} API-Key Speichern") },
            text = {
                Column {
                    Text("Der Key wird in ${selectedProvider.secretFileName} gespeichert und ist direkt mit scripts/aider_launcher.sh kompatibel.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentApiKeyInput,
                        onValueChange = { currentApiKeyInput = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    AiderBridgeService.saveApiKey(context, selectedProvider, currentApiKeyInput)
                    showApiKeyDialog = false
                }) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}
