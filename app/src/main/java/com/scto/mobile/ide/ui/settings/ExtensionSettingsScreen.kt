package com.scto.mobile.ide.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rk.extension.InstallResult
import com.rk.extension.LocalExtension
import com.rk.extension.extensionManager
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extensions = extensionManager.localExtensions.values.toList()
    var showRestartDialog by remember { mutableStateOf(false) }
    var selectedExtensionForInfo by remember { mutableStateOf<LocalExtension?>(null) }
    var targetExtensionIdForUpdate by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedUri ->
                scope.launch {
                    try {
                        val tempFile = File(context.cacheDir, "temp_extension.tinaplug")
                        context.contentResolver.openInputStream(selectedUri)?.use { input ->
                            tempFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        val res = extensionManager.installExtensionFromZip(tempFile)
                        tempFile.delete()
                        if (res is InstallResult.Success) {
                            showRestartDialog = true
                        } else {
                            val errMsg =
                                when (res) {
                                    is InstallResult.ValidationFailed -> "Validation failed: ${res.error?.message}"
                                    is InstallResult.Error -> "Error: ${res.error}"
                                    else -> "Unknown error"
                                }
                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to copy/install: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    val updatePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedUri ->
                val extId = targetExtensionIdForUpdate ?: return@let
                scope.launch {
                    try {
                        val tempFile = File(context.cacheDir, "temp_extension.tinaplug")
                        context.contentResolver.openInputStream(selectedUri)?.use { input ->
                            tempFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        val res = extensionManager.installExtensionFromZip(tempFile)
                        tempFile.delete()
                        if (res is InstallResult.Success) {
                            if (res.extension.manifest.id == extId) {
                                Toast.makeText(context, "Extension updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                        context,
                                        "Extension installed successfully, but ID did not match update target.",
                                        Toast.LENGTH_LONG,
                                    )
                                    .show()
                            }
                            showRestartDialog = true
                        } else {
                            val errMsg =
                                when (res) {
                                    is InstallResult.ValidationFailed -> "Validation failed: ${res.error?.message}"
                                    is InstallResult.Error -> "Error: ${res.error}"
                                    else -> "Unknown error"
                                }
                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        targetExtensionIdForUpdate = null
                    }
                }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extensions", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(Icons.Default.Add, "Install Extension")
                    }
                },
            )
        }
    ) { innerPadding ->
        if (extensions.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No extensions installed", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(extensions) { extension ->
                    val isLoaded = extensionManager.loadedExtensions.containsKey(extension)
                    var isEnabled by
                        remember(extension.id) { mutableStateOf(!extensionManager.isExtensionDisabled(extension.id)) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = extension.manifest.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Version: ${extension.manifest.version}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        isEnabled = checked
                                        extensionManager.setExtensionDisabled(extension.id, !checked)
                                        showRestartDialog = true
                                    },
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = extension.manifest.description ?: "No description provided",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(onClick = { selectedExtensionForInfo = extension }) {
                                    Icon(Icons.Default.Info, contentDescription = "Info")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Details")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                TextButton(
                                    onClick = {
                                        targetExtensionIdForUpdate = extension.id
                                        updatePickerLauncher.launch("*/*")
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Update")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Update")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            val res = extensionManager.uninstallExtension(extension.id)
                                            if (res.isSuccess) {
                                                Toast.makeText(
                                                        context,
                                                        "Extension uninstalled successfully",
                                                        Toast.LENGTH_SHORT,
                                                    )
                                                    .show()
                                                showRestartDialog = true
                                            } else {
                                                Toast.makeText(
                                                        context,
                                                        "Error: ${res.exceptionOrNull()?.message}",
                                                        Toast.LENGTH_LONG,
                                                    )
                                                    .show()
                                            }
                                        }
                                    },
                                    colors =
                                        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Uninstall")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Restart Required") },
            text = { Text("Changes to extensions will take effect after restarting the application.") },
            confirmButton = { TextButton(onClick = { showRestartDialog = false }) { Text("OK") } },
        )
    }

    selectedExtensionForInfo?.let { ext ->
        AlertDialog(
            onDismissRequest = { selectedExtensionForInfo = null },
            title = { Text(ext.manifest.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ID: ${ext.manifest.id}", fontWeight = FontWeight.Bold)
                    Text("Version: ${ext.manifest.version}")
                    Text("Main Class: ${ext.manifest.mainClass}")
                    ext.manifest.author.let { Text("Author: ${it.displayName}") }
                    ext.manifest.minAppVersion?.let { Text("Min App Version: $it") }
                    ext.manifest.maxAppVersion?.let { Text("Max App Version: $it") }
                }
            },
            confirmButton = { TextButton(onClick = { selectedExtensionForInfo = null }) { Text("Close") } },
        )
    }
}
