package com.mobileide.git.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitScreen(viewModel: GitViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.state) {
        viewModel.state.collectLatest {
            it.userMessage?.let { message ->
                snackbarHostState.showSnackbar(message)
                viewModel.onEvent(GitEvent.UserMessageShown)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Remote-Aktionen (Push/Pull)
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.onEvent(GitEvent.PullClicked) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Pull")
                        Spacer(Modifier.width(4.dp))
                        Text("Pull")
                    }
                    Button(onClick = { viewModel.onEvent(GitEvent.PushClicked) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Push")
                        Spacer(Modifier.width(4.dp))
                        Text("Push")
                    }
                }
            }

            // Status
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Button(onClick = { viewModel.onEvent(GitEvent.StatusClicked) }) { Text("Refresh Status") }
                Spacer(Modifier.height(8.dp))
                Text("Branch: ${state.statusResult.branch}", style = MaterialTheme.typography.titleMedium)
                if (state.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }

            // Staged Files
            val stagedFiles = state.statusResult.added + state.statusResult.changed
            if (stagedFiles.isNotEmpty()) {
                item { Text("Staged Changes", style = MaterialTheme.typography.titleSmall) }
                items(stagedFiles.toList()) { file ->
                    FileStatusRow(file = file, icon = Icons.Default.Remove, "Unstage") {
                        viewModel.onEvent(GitEvent.UnstageFile(file))
                    }
                }
            }
            
            // Unstaged Files
            val unstagedFiles = state.statusResult.modified + state.statusResult.untracked
            if (unstagedFiles.isNotEmpty()) {
                item { Text("Unstaged Changes", style = MaterialTheme.typography.titleSmall) }
                items(unstagedFiles.toList()) { file ->
                    FileStatusRow(file = file, icon = Icons.Default.Add, "Stage") {
                        viewModel.onEvent(GitEvent.StageFile(file))
                    }
                }
            }

            // Commit Section
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                OutlinedTextField(
                    value = state.commitMessage,
                    onValueChange = { viewModel.onEvent(GitEvent.CommitMessageChanged(it)) },
                    label = { Text("Commit-Nachricht") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.onEvent(GitEvent.CommitClicked) }) { Text("Commit") }
            }
        }
    }
}

@Composable
fun FileStatusRow(file: String, icon: ImageVector, iconDescription: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(file, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onAction) {
            Icon(icon, contentDescription = iconDescription)
        }
    }
}
