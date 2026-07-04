package com.scto.mobile.ide.core.tooling.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scto.mobile.ide.core.tooling.api.Resource

/**
 * Full-screen Gradle task manager screen.
 *
 * Displays a [LazyColumn] of checkable task rows.
 * A [TopAppBar] contains the title "Tasks" and a [PlayArrow] icon button
 * that triggers the build and opens the log [ModalBottomSheet].
 *
 * @param projectPath Path to the Gradle project to build.
 * @param vm          Injected [GradleViewModel] (default: scoped to the composition).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradleTaskManagerScreen(
    projectPath: String,
    vm: GradleViewModel = viewModel(),
) {
    val availableTasks   by vm.availableTasks.collectAsStateWithLifecycle()
    val tasksFetchState  by vm.tasksFetchState.collectAsStateWithLifecycle()
    val isBuilding       by vm.isBuilding.collectAsStateWithLifecycle()
    val logLines         by vm.logLines.collectAsStateWithLifecycle()

    // Checked task set — local UI state, initialised from ViewModel selection
    var checkedTasks by remember { mutableStateOf(setOf<String>()) }
    var showLog      by remember { mutableStateOf(false) }

    // Fetch task list when the screen enters composition
    LaunchedEffect(projectPath) { vm.fetchTasks(projectPath) }

    // Auto-open log sheet when a build starts
    LaunchedEffect(isBuilding) { if (isBuilding) showLog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                actions = {
                    IconButton(
                        onClick  = {
                            vm.onPlayClicked(projectPath, checkedTasks.toList())
                        },
                        enabled  = !isBuilding && checkedTasks.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.PlayArrow,
                            contentDescription = "Run selected tasks",
                            tint = if (!isBuilding && checkedTasks.isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val state = tasksFetchState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is Resource.Error -> {
                    Text(
                        text     = "Failed to load tasks:\n${state.message}",
                        color    = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )
                }

                is Resource.Success -> {
                    if (availableTasks.isEmpty()) {
                        Text(
                            text     = "No tasks found.",
                            modifier = Modifier.align(Alignment.Center),
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        TaskCheckboxList(
                            tasks        = availableTasks,
                            checkedTasks = checkedTasks,
                            onToggle     = { task ->
                                checkedTasks = if (task in checkedTasks)
                                    checkedTasks - task
                                else
                                    checkedTasks + task
                            },
                        )
                    }
                }
            }
        }
    }

    // ── Log bottom sheet ─────────────────────────────────────────────────────
    if (showLog) {
        ModalBottomSheet(
            onDismissRequest = { showLog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            GradleLogBottomSheetContent(
                logLines   = logLines,
                isBuilding = isBuilding,
                onDismiss  = { showLog = false },
                modifier   = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
            )
        }
    }
}

// ─── Task checkbox list ───────────────────────────────────────────────────────

@Composable
private fun TaskCheckboxList(
    tasks:        List<String>,
    checkedTasks: Set<String>,
    onToggle:     (String) -> Unit,
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(vertical = 4.dp),
    ) {
        items(
            items = tasks,
            key   = { task -> task },           // Stable key = task path string
        ) { task ->
            TaskRow(
                taskName  = task,
                isChecked = task in checkedTasks,
                onToggle  = { onToggle(task) },
            )
        }
    }
}

@Composable
private fun TaskRow(
    taskName:  String,
    isChecked: Boolean,
    onToggle:  () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked         = isChecked,
            onCheckedChange = { onToggle() },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text  = taskName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
