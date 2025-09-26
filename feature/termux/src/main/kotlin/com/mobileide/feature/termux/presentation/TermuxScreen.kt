package com.mobileide.termux.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxScreen(viewModel: TermuxViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var packageToUninstall by remember { mutableStateOf<com.mobileide.data.TermuxPackage?>(null) }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(TermuxEvent.UserMessageShown)
        }
    }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(TermuxEvent.ShowInstallDialog) }) {
                Icon(Icons.Default.Add, contentDescription = "Install Package")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (state.error != null) {
                Text(
                    text = "Error: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.packages) { pkg ->
                    PackageRow(
                        pkg = pkg,
                        onUninstall = { packageToUninstall = it }
                    )
                    Divider()
                }
            }
        }
    }

    if (state.showInstallDialog) {
        InstallPackageDialog(
            onDismiss = { viewModel.onEvent(TermuxEvent.DismissInstallDialog) },
            onInstall = { packageName ->
                viewModel.onEvent(TermuxEvent.InstallPackage(packageName))
                viewModel.onEvent(TermuxEvent.DismissInstallDialog)
            }
        )
    }

    packageToUninstall?.let { pkg ->
        UninstallConfirmDialog(
            pkg = pkg,
            onDismiss = { packageToUninstall = null },
            onConfirm = {
                viewModel.onEvent(TermuxEvent.UninstallPackage(it))
                packageToUninstall = null
            }
        )
    }
}

@Composable
fun PackageRow(pkg: com.mobileide.data.TermuxPackage, onUninstall: (com.mobileide.data.TermuxPackage) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(pkg.name, style = MaterialTheme.typography.bodyLarge)
            Text(pkg.version, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = { onUninstall(pkg) }) {
            Icon(Icons.Default.Delete, contentDescription = "Uninstall ${pkg.name}")
        }
    }
}

@Composable
fun InstallPackageDialog(onDismiss: () -> Unit, onInstall: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Install Package") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Package name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onInstall(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun UninstallConfirmDialog(pkg: com.mobileide.data.TermuxPackage, onDismiss: () -> Unit, onConfirm: (com.mobileide.data.TermuxPackage) -> Unit) {
     AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uninstall ${pkg.name}?") },
        text = { Text("Are you sure you want to uninstall this package?") },
        confirmButton = {
            Button(onClick = { onConfirm(pkg) }) { Text("Uninstall") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
