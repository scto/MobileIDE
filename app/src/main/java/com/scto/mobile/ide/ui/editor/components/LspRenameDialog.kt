package com.scto.mobile.ide.ui.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File
import java.net.URI
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WorkspaceEdit

@Composable
fun LspRenameDialog(currentName: String, onDismiss: () -> Unit, onConfirmRename: (newName: String) -> Unit) {
    var newNameText by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Symbol umbenennen (LSP Rename)") },
        text = {
            Column {
                Text("Geben Sie den neuen Namen für '$currentName' ein:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newNameText,
                    onValueChange = { newNameText = it },
                    label = { Text("Neuer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newNameText.isNotBlank() && newNameText != currentName) {
                        onConfirmRename(newNameText)
                    }
                },
                enabled = newNameText.isNotBlank() && newNameText != currentName,
            ) {
                Text("Vorschau & Umbenennen")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
fun LspWorkspaceEditPreviewDialog(
    workspaceEdit: WorkspaceEdit,
    onDismiss: () -> Unit,
    onApplyEdits: (workspaceEdit: WorkspaceEdit) -> Unit,
) {
    val changesMap =
        remember(workspaceEdit) {
            val map = mutableMapOf<String, List<TextEdit>>()
            workspaceEdit.changes?.forEach { (uri, edits) -> map[uri] = edits }
            workspaceEdit.documentChanges?.forEach { either ->
                if (either.isLeft) {
                    val docEdit = either.left
                    map[docEdit.textDocument.uri] = docEdit.edits
                }
            }
            map
        }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = "Refactoring Diff-Vorschau (${changesMap.size} Datei(en) betroffen)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    changesMap.forEach { (uri, edits) ->
                        val fileName =
                            try {
                                File(URI(uri)).name
                            } catch (_: Exception) {
                                uri
                            }
                        item {
                            Card(
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        fileName,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        uri,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    edits.forEach { edit ->
                                        val line = edit.range.start.line + 1
                                        Text(
                                            text = "Zeile $line: Ersetze '${edit.range}' durch '${edit.newText}'",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onApplyEdits(workspaceEdit)
                            onDismiss()
                        }
                    ) {
                        Text("Änderungen anwenden")
                    }
                }
            }
        }
    }
}
