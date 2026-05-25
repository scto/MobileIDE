package com.mobileide.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileide.app.data.FileNode

sealed class FileAction {
    data class NewFile(val parent: FileNode) : FileAction()
    data class NewFolder(val parent: FileNode) : FileAction()
    data class Rename(val node: FileNode) : FileAction()
    data class Delete(val node: FileNode) : FileAction()
    data class CopyPath(val node: FileNode) : FileAction()
}

@Composable
fun FileContextMenu(
    node: FileNode,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAction: (FileAction) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (node.isDirectory) {
            DropdownMenuItem(
                text = { Text("New File") },
                leadingIcon = { Icon(Icons.Default.InsertDriveFile, null) },
                onClick = { onAction(FileAction.NewFile(node)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("New Folder") },
                leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
                onClick = { onAction(FileAction.NewFolder(node)); onDismiss() }
            )
            HorizontalDivider()
        }
        DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(Icons.Default.Edit, null) },
            onClick = { onAction(FileAction.Rename(node)); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Copy Path") },
            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
            onClick = { onAction(FileAction.CopyPath(node)); onDismiss() }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            onClick = { onAction(FileAction.Delete(node)); onDismiss() }
        )
    }
}

@Composable
fun NewFileDialog(
    parentPath: String,
    isFolder: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String) -> Unit
) {
    var name by remember { mutableStateOf(if (isFolder) "new_folder" else "NewFile.kt") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isFolder) "New Folder" else "New File") },
        text = {
            Column {
                Text("Location: $parentPath", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) { onCreate(name.trim()); onDismiss() } }) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RenameDialog(
    node: FileNode,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit
) {
    var newName by remember { mutableStateOf(node.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (newName.isNotBlank()) { onRename(newName.trim()); onDismiss() } }) {
                Text("Rename")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DeleteConfirmDialog(
    node: FileNode,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${if (node.isDirectory) "Folder" else "File"}?") },
        text = {
            Text("Permanently delete '${node.name}'${if (node.isDirectory) " and all its contents" else ""}?")
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
