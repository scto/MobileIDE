package com.scto.mobile.ide.core.tooling.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InstallMobile
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
import java.io.File
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
            .fillMaxHeight(0.75f)
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
                                ToolingLogCategory.AI -> "AI"
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
                ToolingLogCategory.AI -> {
                    com.scto.mobile.ide.ui.editor.aicoding.AiCodingToolingPanel(projectPath = projectPath)
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
    var selectedVariant by remember { mutableStateOf(BuildHelper.getLastBuildVariant(context, projectPath)) }
    var customVariantText by remember { mutableStateOf("") }
    var isCustomVariant by remember { mutableStateOf(false) }

    var gradleTasks by remember { mutableStateOf<List<GradleTask>>(emptyList()) }
    val selectedTasks = remember { mutableStateMapOf<String, Boolean>() }
    val selectedStandardFlags = remember { mutableStateMapOf<String, Boolean>() }
    var extraFlagsText by remember { mutableStateOf("") }
    var isFlagsExpanded by remember { mutableStateOf(false) }

    var isLoadingTasks by remember { mutableStateOf(false) }
    var isRunningBuild by remember { mutableStateOf(false) }
    var buildStatus by remember { mutableStateOf<BuildStatus?>(null) }
    val buildLogs = remember { mutableStateListOf<GradleLogLine>() }
    val coroutineScope = rememberCoroutineScope()

    val standardVariants = remember { listOf("assembleDebug", "assembleRelease", "bundleRelease") }
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

    fun startBuild(taskToRun: String) {
        val activeFlags = selectedStandardFlags.filter { it.value }.keys.toList() +
                extraFlagsText.split(" ").filter { it.isNotBlank() }
        coroutineScope.launch {
            isRunningBuild = true
            buildStatus = null
            buildLogs.clear()
            ToolingLogManagerImpl.clearLogs(ToolingLogCategory.BUILD)

            var exitCode = 0
            GradleTaskManagerImpl.runTasks(context, projectPath, listOf(taskToRun), activeFlags).collect { logLine ->
                buildLogs.add(logLine)
                if (logLine.rawText.contains("Execution finished with exit code:")) {
                    val codeStr = logLine.rawText.substringAfter("code:").trim()
                    exitCode = codeStr.toIntOrNull() ?: 0
                }
            }

            if (exitCode == 0) {
                val apkFile = withContext(Dispatchers.IO) { BuildHelper.findGeneratedApk(projectPath) }
                buildStatus = BuildStatus.Success(apkFile)
            } else {
                buildStatus = BuildStatus.Error("Build fehlgeschlagen mit Exit-Code $exitCode")
            }
            isRunningBuild = false
        }
    }

    var showKeystoreDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        // Build Variant Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Build Variant (APK / Bundle)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { showKeystoreDialog = true }) {
                        Text("🔑 Signierung", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    standardVariants.forEach { variant ->
                        FilterChip(
                            selected = !isCustomVariant && selectedVariant == variant,
                            onClick = {
                                isCustomVariant = false
                                selectedVariant = variant
                                BuildHelper.saveLastBuildVariant(context, projectPath, variant)
                            },
                            label = { Text(variant, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    FilterChip(
                        selected = isCustomVariant,
                        onClick = { isCustomVariant = true },
                        label = { Text("Eigene...", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                if (isCustomVariant) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customVariantText,
                        onValueChange = {
                            customVariantText = it
                            selectedVariant = it
                            BuildHelper.saveLastBuildVariant(context, projectPath, it)
                        },
                        label = { Text("Custom Task (z. B. assemblePlayStoreDebug)", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val taskToRun = if (isCustomVariant && customVariantText.isNotBlank()) customVariantText else selectedVariant
                        startBuild(taskToRun)
                    },
                    enabled = !isRunningBuild,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Build APK ($selectedVariant) starten")
                }
            }
        }

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
                        text = "Gradle Flags (${selectedStandardFlags.filter { it.value }.size} aktiv)",
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
                        label = { Text("Zusätzliche Flags (-P, -D)", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Result Status Card (Success / Error)
        buildStatus?.let { status ->
            when (status) {
                is BuildStatus.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🎉 BUILD ERFOLGREICH!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            status.apkFile?.let { apk ->
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Pfad: ${apk.absolutePath}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "Größe: ${BuildHelper.formatFileSize(apk.length())}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { BuildHelper.installApk(context, apk) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Installieren", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { BuildHelper.openApkExternal(context, apk) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Öffnen", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            } ?: run {
                                Text(
                                    text = "Build erfolgreich, jedoch konnte keine APK-Datei automatisch gefunden werden.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                is BuildStatus.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "❌ BUILD FEHLGESCHLAGEN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = status.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Build Output Log",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(modifier = Modifier.weight(1f)) {
            GradleLogPanel(buildLogs = buildLogs, autoScrollToError = buildStatus is BuildStatus.Error)
        }
    }

    if (showKeystoreDialog) {
        com.scto.mobile.ide.core.apkbuilder.ui.KeystoreManagerDialog(
            projectPath = projectPath,
            onDismiss = { showKeystoreDialog = false }
        )
    }
}

sealed class BuildStatus {
    data class Success(val apkFile: File?) : BuildStatus()
    data class Error(val message: String) : BuildStatus()
}

@Composable
fun GradleLogPanel(
    buildLogs: List<GradleLogLine>,
    autoScrollToError: Boolean = false
) {
    val listState = rememberLazyListState()

    LaunchedEffect(buildLogs.size) {
        if (buildLogs.isNotEmpty() && !autoScrollToError) {
            try {
                listState.scrollToItem(buildLogs.size - 1)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(autoScrollToError) {
        if (autoScrollToError && buildLogs.isNotEmpty()) {
            val firstErrorIndex = buildLogs.indexOfFirst { it.level == GradleLogLevel.ERROR }
            if (firstErrorIndex >= 0) {
                try {
                    listState.animateScrollToItem(firstErrorIndex)
                } catch (_: Exception) {}
            }
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
