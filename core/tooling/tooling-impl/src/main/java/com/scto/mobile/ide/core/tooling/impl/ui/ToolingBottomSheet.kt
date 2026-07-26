package com.scto.mobile.ide.core.tooling.impl.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scto.mobile.ide.core.tooling.api.ToolingLogCategory
import com.scto.mobile.ide.core.tooling.api.ToolingLogEntry
import com.scto.mobile.ide.core.tooling.impl.*
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
            .fillMaxHeight(0.65f)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuildAndTasksPanel(
    projectPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var gradleTasks by remember { mutableStateOf<List<GradleTask>>(emptyList()) }
    val selectedTasks = remember { mutableStateMapOf<String, Boolean>() }
    val selectedStandardFlags = remember { mutableStateMapOf<String, Boolean>() }
    var extraFlagsText by remember { mutableStateOf("") }
    var isFlagsExpanded by remember { mutableStateOf(false) }

    var isLoadingTasks by remember { mutableStateOf(false) }
    var isRunningTasks by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val buildLogs = remember { mutableStateListOf<GradleLogLine>() }

    val standardFlags = remember {
        listOf(
            "--info",
            "--debug",
            "--warn",
            "--stacktrace",
            "--scan",
            "--offline",
            "--refresh-dependencies",
            "--dry-run",
            "--parallel",
            "--continue"
        )
    }

    LaunchedEffect(projectPath) {
        if (projectPath.isNotEmpty()) {
            isLoadingTasks = true
            gradleTasks = withContext(Dispatchers.IO) {
                GradleTaskManagerImpl.getTasks(context, projectPath, forceRefresh = false)
            }
            isLoadingTasks = false
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        // Flags Section
        OutlinedCard(
            onClick = { isFlagsExpanded = !isFlagsExpanded },
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gradle Flags (${selectedStandardFlags.filter { it.value }.size} active)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = if (isFlagsExpanded) "▼" else "▲", style = MaterialTheme.typography.labelSmall)
                }

                if (isFlagsExpanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        standardFlags.forEach { flag ->
                            val isSelected = selectedStandardFlags[flag] ?: false
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStandardFlags[flag] = !isSelected },
                                label = { Text(flag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = extraFlagsText,
                        onValueChange = { extraFlagsText = it },
                        label = { Text("Extra Flags (-P, -D)", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Header Row with Refresh and Run
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
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
                                GradleTaskManagerImpl.getTasks(context, projectPath, forceRefresh = true)
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
                        val activeFlags = selectedStandardFlags.filter { it.value }.keys.toList() +
                                extraFlagsText.split(" ").filter { it.isNotBlank() }
                        if (selectedNames.isNotEmpty()) {
                            coroutineScope.launch {
                                isRunningTasks = true
                                buildLogs.clear()
                                ToolingLogManagerImpl.clearLogs(ToolingLogCategory.BUILD)
                                GradleTaskManagerImpl.runTasks(context, projectPath, selectedNames, activeFlags).collect { logLine ->
                                    buildLogs.add(logLine)
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
            Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(4.dp)
            ) {
                items(gradleTasks) { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTasks[task.name] = !(selectedTasks[task.name] ?: false)
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedTasks[task.name] ?: false,
                            onCheckedChange = { selectedTasks[task.name] = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(task.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            task.description?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Build Output",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(modifier = Modifier.weight(0.65f)) {
            GradleLogPanel(buildLogs = buildLogs)
        }
    }
}

@Composable
fun GradleLogPanel(buildLogs: List<GradleLogLine>) {
    val listState = rememberLazyListState()

    LaunchedEffect(buildLogs.size) {
        if (buildLogs.isNotEmpty()) {
            try {
                listState.scrollToItem(buildLogs.size - 1)
            } catch (_: Exception) {}
        }
    }

    if (buildLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Kein Build-Output vorhanden", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(6.dp)
        ) {
            items(buildLogs) { log ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text(
                        text = "${log.lineNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(36.dp).padding(end = 8.dp)
                    )
                    Text(
                        text = log.rawText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = when (log.level) {
                            GradleLogLevel.ERROR -> Color(0xFFFF5252)
                            GradleLogLevel.WARN -> Color(0xFFFFB300)
                            GradleLogLevel.SUCCESS -> Color(0xFF4CAF50)
                            GradleLogLevel.TASK -> Color(0xFF29B6F6)
                            GradleLogLevel.INFO -> MaterialTheme.colorScheme.primary
                            GradleLogLevel.DEFAULT -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
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
            Text("Keine Logs in dieser Kategorie", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
