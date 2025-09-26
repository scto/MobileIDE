package com.mobileide.projectpicker.presentation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectPickerScreen(
    viewModel: ProjectPickerViewModel = hiltViewModel(),
    onProjectSelected: () -> Unit,
    onNavigateToTemplates: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Persist permission to access the directory
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                     Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                
                // Hier benötigen wir eine Methode, um den echten Pfad aus der URI zu bekommen.
                // Dies ist auf neueren Android-Versionen komplex. Für dieses Beispiel
                // nehmen wir an, dass wir den Pfad irgendwie extrahieren können.
                // In einer echten App wäre hier mehr Logik nötig.
                // Für unser Beispiel verwenden wir den Pfad, der in `uri.path` steht
                val path = uri.path ?: ""
                val realPath = path.substringAfter("primary:")
                val fullPath = "/storage/emulated/0/$realPath"
                viewModel.onEvent(ProjectPickerEvent.OpenProject(fullPath))
                onProjectSelected()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("MobileIDE - Project Explorer") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onNavigateToTemplates,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("New Project")
                }
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        directoryPickerLauncher.launch(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                     Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Open Project")
                }
            }
            Divider()
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text(
                            "Recent Projects",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(state.recentProjects) { project ->
                        RecentProjectItem(project = project, onClick = {
                            viewModel.onEvent(ProjectPickerEvent.OpenProject(project.path))
                            onProjectSelected()
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun RecentProjectItem(project: RecentProject, onClick: () -> Unit) {
    ListItem(
        headlineText = { Text(project.name) },
        supportingText = { Text(project.path) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
