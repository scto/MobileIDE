package com.scto.mobile.ide.features.git.conflict

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitConflictResolutionDialog(projectPath: String, onDismiss: () -> Unit, onMergeCompleted: () -> Unit) {
    val scope = rememberCoroutineScope()
    var conflictingFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var currentFileContent by remember { mutableStateOf("") }
    var parsedConflictFile by remember { mutableStateOf<ParsedConflictFile?>(null) }
    val chunkResolutions = remember { mutableStateMapOf<Int, String>() }

    var showAbortConfirmDialog by remember { mutableStateOf(false) }
    var showCommitDialog by remember { mutableStateOf(false) }
    var mergeCommitMessage by remember { mutableStateOf("Merge branch 'incoming' into HEAD") }
    var isProcessing by remember { mutableStateOf(false) }

    // Load initial conflicts
    LaunchedEffect(projectPath) {
        isProcessing = true
        val files = GitConflictManager.getConflictingFiles(projectPath)
        conflictingFiles = files
        if (files.isNotEmpty()) {
            selectedFilePath = files.first()
        }
        isProcessing = false
    }

    // Load selected file content & parse conflict chunks
    LaunchedEffect(selectedFilePath) {
        val relPath = selectedFilePath ?: return@LaunchedEffect
        val file = File(projectPath, relPath)
        if (file.exists()) {
            val content = file.readText()
            currentFileContent = content
            val parsed = GitConflictParser.parseConflictFile(relPath, content)
            parsedConflictFile = parsed
            chunkResolutions.clear()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallMerge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("3-Wege Merge Konfliktlösung", style = MaterialTheme.typography.titleMedium)
                }
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = "${conflictingFiles.size} Konflikt-Dateien",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isProcessing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (conflictingFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("✅ Keine offenen Merge-Konflikte vorhanden!", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    // File Selector Dropdown
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        Text("Datei: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        var expandedDropdown by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedDropdown,
                            onExpandedChange = { expandedDropdown = !expandedDropdown },
                            modifier = Modifier.weight(1f),
                        ) {
                            OutlinedTextField(
                                value = selectedFilePath ?: "",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown)
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                            ) {
                                conflictingFiles.forEach { file ->
                                    DropdownMenuItem(
                                        text = { Text(file, fontFamily = FontFamily.Monospace) },
                                        onClick = {
                                            selectedFilePath = file
                                            expandedDropdown = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Conflict Chunks List
                    val chunks = parsedConflictFile?.chunks ?: emptyList()
                    if (chunks.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "Keine Konflikt-Marker in dieser Datei gefunden.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)) {
                            items(chunks.size) { index ->
                                val chunk = chunks[index]
                                var customText by remember { mutableStateOf("") }
                                var isCustomMode by remember { mutableStateOf(false) }

                                Card(
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = "Konflikt Block ${chunk.id} von ${chunks.size}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary,
                                            )
                                            if (chunkResolutions.containsKey(chunk.id)) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(4.dp),
                                                ) {
                                                    Text(
                                                        "Gelöst",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Local / HEAD block
                                        Text(
                                            "Lokal (HEAD):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64B5F6),
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Surface(
                                            color = Color(0xFF1E293B),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier =
                                                Modifier.fillMaxWidth()
                                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                                                    .padding(6.dp),
                                        ) {
                                            Text(
                                                text = chunk.localText.ifBlank { "<Leer>" },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF93C5FD),
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Incoming / MERGE_HEAD block
                                        Text(
                                            "Eingehend (MERGE_HEAD):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF81C784),
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Surface(
                                            color = Color(0xFF14532D).copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier =
                                                Modifier.fillMaxWidth()
                                                    .border(1.dp, Color(0xFF166534), RoundedCornerShape(4.dp))
                                                    .padding(6.dp),
                                        ) {
                                            Text(
                                                text = chunk.incomingText.ifBlank { "<Leer>" },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFA7F3D0),
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Action buttons
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Button(
                                                onClick = { chunkResolutions[chunk.id] = chunk.localText },
                                                colors =
                                                    ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Text("Lokal", style = MaterialTheme.typography.labelSmall)
                                            }

                                            Button(
                                                onClick = { chunkResolutions[chunk.id] = chunk.incomingText },
                                                colors =
                                                    ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Text("Eingehend", style = MaterialTheme.typography.labelSmall)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    chunkResolutions[chunk.id] =
                                                        "${chunk.localText}\n${chunk.incomingText}"
                                                },
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Text("Beide", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Mark current file resolved (git add)
                Button(
                    onClick = {
                        val relPath = selectedFilePath ?: return@Button
                        scope.launch {
                            isProcessing = true
                            val resolvedContent =
                                GitConflictParser.rebuildResolvedFileContent(currentFileContent, chunkResolutions)
                            val success = GitConflictManager.resolveFileConflict(projectPath, relPath, resolvedContent)
                            if (success) {
                                conflictingFiles = GitConflictManager.getConflictingFiles(projectPath)
                                selectedFilePath = conflictingFiles.firstOrNull()
                            }
                            isProcessing = false
                        }
                    },
                    enabled =
                        selectedFilePath != null && chunkResolutions.size == (parsedConflictFile?.chunks?.size ?: -1),
                ) {
                    Text("Datei als gelöst markieren (git add)")
                }

                // Complete Merge commit button
                Button(onClick = { showCommitDialog = true }, enabled = conflictingFiles.isEmpty()) {
                    Text("Merge abschließen")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { showAbortConfirmDialog = true }) {
                Text("Merge abbrechen", color = MaterialTheme.colorScheme.error)
            }
        },
    )

    // Abort Confirmation Dialog
    if (showAbortConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAbortConfirmDialog = false },
            title = { Text("Merge wirklich abbrechen?") },
            text = { Text("Alle ungespeicherten Konfliktlösungen werden zurückgesetzt (git merge --abort).") },
            confirmButton = {
                Button(
                    onClick = {
                        showAbortConfirmDialog = false
                        scope.launch {
                            GitConflictManager.abortMerge(projectPath)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Abbrechen bestätigen")
                }
            },
            dismissButton = { TextButton(onClick = { showAbortConfirmDialog = false }) { Text("Abbrechen") } },
        )
    }

    // Complete Merge Commit Dialog
    if (showCommitDialog) {
        AlertDialog(
            onDismissRequest = { showCommitDialog = false },
            title = { Text("Merge Commit erstellen") },
            text = {
                Column {
                    Text("Alle Konflikte gelöst. Gib eine Commit-Nachricht ein:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mergeCommitMessage,
                        onValueChange = { mergeCommitMessage = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCommitDialog = false
                        scope.launch {
                            GitConflictManager.completeMerge(projectPath, mergeCommitMessage)
                            onMergeCompleted()
                            onDismiss()
                        }
                    }
                ) {
                    Text("Commit & Fertigstellen")
                }
            },
            dismissButton = { TextButton(onClick = { showCommitDialog = false }) { Text("Abbrechen") } },
        )
    }
}
