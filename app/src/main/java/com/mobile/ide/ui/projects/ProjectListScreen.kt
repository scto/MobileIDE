// Copyright 2025 Thomas Schmid
package com.mobile.ide.ui.projects

import androidx.navigation.NavController

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.mobile.ide.R
// Importe korrigieren/ergänzen:
import com.mobile.ide.core.projects.ProjectTemplates
import com.mobile.ide.core.resources.Res
import com.mobile.ide.core.utils.WorkspaceManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(navController: NavController) {
    val context = LocalContext.current
    val projectDirPath = WorkspaceManager.getWorkspacePath(context)
    val projectDir = File(projectDirPath)

    var folderList by remember { mutableStateOf<List<String>>(emptyList()) }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(projectDir) {
        folderList = withContext(Dispatchers.IO) {
            if (projectDir.exists() && projectDir.isDirectory) {
                projectDir.listFiles { file ->
                    file.isDirectory && file.name != "logs"
                }
                    ?.map { it.name }
                    ?.sorted() 
                    ?: emptyList()
            } else {
                emptyList()
            }
        }
    }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.project_list_title)) },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, stringResource(R.string.action_settings))
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("new_project") },
                    icon = { Icon(Icons.Default.Add, stringResource(R.string.action_new_project)) },
                    text = { Text(stringResource(R.string.action_new_project)) }
                )
            }
        ) { innerPadding ->
            if (folderList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.project_list_empty))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folderList, key = { it }) { folderName ->
                        ProjectCard(folderName = folderName) {
                            navController.navigate("code_edit/$folderName")
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCard(folderName: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = folderName, style = MaterialTheme.typography.titleMedium)
        }
    }
}
