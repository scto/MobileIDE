package com.scto.mobile.ide.ui.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProjectSettingsDialog(projectPath: String, onDismiss: () -> Unit) {
    var appName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("1.0") }
    var versionCode by remember { mutableStateOf("1") }

    // Placeholder UI for now
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Project Settings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = { Text("Version Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = versionCode,
                    onValueChange = { versionCode = it },
                    label = { Text("Version Code") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // TODO: Save to build.gradle.kts or project.json
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
