package com.scto.mobile.ide.core.tooling.impl.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scto.mobile.ide.core.tooling.impl.GradleTask
import com.scto.mobile.ide.core.tooling.api.ToolingLogCategory
import com.scto.mobile.ide.core.tooling.api.ToolingLogEntry
import com.scto.mobile.ide.core.tooling.impl.GradleTaskManagerImpl
import com.scto.mobile.ide.core.tooling.impl.ToolingLogManagerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ToolingBottomSheet(
    projectPath: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCategory by remember { mutableStateOf(ToolingLogCategory.BUILD) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = activeCategory.ordinal,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            ToolingLogCategory.values().forEach { category ->
                Tab(
                    selected = activeCategory == category,
                    onClick = { activeCategory = category },
                    text = {
                        Text(
                            text = when (category) {
                                ToolingLogCategory.TERMINAL_ERRORS -> "Terminal Logs"
                                ToolingLogCategory.PROJECT_DIAGNOSIS -> "Diagnosis"
                                ToolingLogCategory.IDE_LOG -> "IDE Log"
                                ToolingLogCategory.BUILD -> "Build / Tasks"
                                ToolingLogCategory.LSP -> "LSP"
                            }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeCategory) {
                ToolingLogCategory.BUILD -> {
                    BuildAndTasksPanel(projectPath = projectPath)
                }
                else -> {
                    ToolingLogPanel(category = activeCategory)
                }
            }
        }
    }
}

@Composable
fun BuildAndTasksPanel(
    projectPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var gradleTasks by remember { mutableStateOf<List<GradleTask>>(emptyList()) }
    val selectedTasks = remember { mutableStateMapOf<String, Boolean>() }
    var isLoadingTasks by remember { mutableStateOf(false) }
    var isRunningTasks by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(projectPath) {
        if (projectPath.isNotEmpty()) {
            isLoadingTasks = true
            gradleTasks = withContext(Dispatchers.IO) {
                GradleTaskManagerImpl.getTasks(context, projectPath)
            }
            isLoadingTasks = false
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gradle Tasks",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            isLoadingTasks = true
                            gradleTasks = withContext(Dispatchers.IO) {
                                GradleTaskManagerImpl.getTasks(context, projectPath)
                            }
                            isLoadingTasks = false
                        }
                    },
                    enabled = !isLoadingTasks
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Tasks")
                }
                IconButton(
                    onClick = {
                        val selectedNames = selectedTasks.filter { it.value }.keys.toList()
                        if (selectedNames.isNotEmpty()) {
                            coroutineScope.launch {
                                isRunningTasks = true
                                ToolingLogManagerImpl.clearLogs(ToolingLogCategory.BUILD)
                                GradleTaskManagerImpl.runTasks(context, projectPath, selectedNames).collect {
                                    // Outputs logged in runTasks
                                }
                                isRunningTasks = false
                            }
                        }
                    },
                    enabled = !isRunningTasks && selectedTasks.any { it.value }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run Tasks", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (isLoadingTasks) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.weight(0.4f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(8.dp)) {
                items(gradleTasks) { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTasks[task.name] = !(selectedTasks[task.name] ?: false)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedTasks[task.name] ?: false,
                            onCheckedChange = { selectedTasks[task.name] = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(task.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            task.description?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Build Output",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(modifier = Modifier.weight(0.6f)) {
            ToolingLogPanel(category = ToolingLogCategory.BUILD)
        }
    }
}

@Composable
fun ToolingLogPanel(category: ToolingLogCategory) {
    val logs = remember { mutableStateListOf<ToolingLogEntry>() }
    val listState = rememberLazyListState()

    LaunchedEffect(category) {
        logs.clear()
        logs.addAll(ToolingLogManagerImpl.getLogs(category))
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    LaunchedEffect(category) {
        ToolingLogManagerImpl.logFlow.collect { entry ->
            if (entry.category == category) {
                logs.add(entry)
                try {
                    listState.scrollToItem(logs.size - 1)
                } catch (_: Exception) {}
            }
        }
    }

    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No logs in this category", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(8.dp)
        ) {
            items(logs) { log ->
                val time = remember(log.timestamp) {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(70.dp)
                    )
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = when (log.level) {
                            "ERROR" -> MaterialTheme.colorScheme.error
                            "WARN" -> Color(0xFFFFA000)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}
