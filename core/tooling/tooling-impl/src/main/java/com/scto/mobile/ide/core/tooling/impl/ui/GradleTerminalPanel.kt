package com.scto.mobile.ide.core.tooling.impl.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scto.mobile.ide.core.tooling.api.IpcProtocol

// ─── Color palette for log levels ────────────────────────────────────────────

private val ColorInfo    = Color(0xFFB0BEC5)  // Blue-grey
private val ColorWarn    = Color(0xFFFFF176)  // Yellow
private val ColorError   = Color(0xFFEF9A9A)  // Red
private val ColorDebug   = Color(0xFF90A4AE)  // Muted
private val ColorSuccess = Color(0xFFA5D6A7)  // Green
private val ColorProgress= Color(0xFF80DEEA)  // Cyan

private fun logLevelColor(level: String): Color = when (level.uppercase()) {
    IpcProtocol.LEVEL_WARN    -> ColorWarn
    IpcProtocol.LEVEL_ERROR   -> ColorError
    IpcProtocol.LEVEL_DEBUG   -> ColorDebug
    "SUCCESS"                 -> ColorSuccess
    "PROGRESS"                -> ColorProgress
    else                      -> ColorInfo
}

// ─── Main terminal panel ──────────────────────────────────────────────────────

/**
 * Full live-terminal Compose UI for Gradle build output.
 *
 * @param projectPath  Absolute path to the Gradle project directory.
 * @param initialTasks Pre-selected tasks to run (e.g. [":app:assembleDebug"]).
 * @param modifier     Optional modifier.
 * @param vm           ViewModel instance (can be injected for testing).
 */
@Composable
fun GradleTerminalPanel(
    projectPath: String,
    initialTasks: List<String> = listOf(":app:assembleDebug"),
    modifier: Modifier = Modifier,
    vm: GradleViewModel = viewModel(),
) {
    val logLines     by vm.logLines.collectAsStateWithLifecycle()
    val isBuilding   by vm.isBuilding.collectAsStateWithLifecycle()
    val tasks        by vm.availableTasks.collectAsStateWithLifecycle()

    // Auto-fetch tasks when the panel is first shown
    LaunchedEffect(projectPath) { vm.fetchTasks(projectPath) }

    var selectedTasks by remember { mutableStateOf(initialTasks) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))  // GitHub-dark background
            .padding(0.dp),
    ) {
        // ── Toolbar ──────────────────────────────────────────────────────────
        GradleToolbar(
            isBuilding    = isBuilding,
            selectedTasks = selectedTasks,
            onPlay        = { vm.onPlayClicked(projectPath, selectedTasks) },
            onCancel      = { vm.cancelBuild() },
            onClear       = { vm.clearLog() },
        )

        HorizontalDivider(color = Color(0xFF21262D), thickness = 1.dp)

        // ── Task selector chips ───────────────────────────────────────────────
        if (tasks.isNotEmpty()) {
            TaskChipRow(
                tasks          = tasks.take(20),  // Show first 20 for quick access
                selectedTasks  = selectedTasks,
                onToggle       = { task ->
                    selectedTasks = if (task in selectedTasks)
                        selectedTasks - task
                    else
                        selectedTasks + task
                },
            )
            HorizontalDivider(color = Color(0xFF21262D), thickness = 1.dp)
        }

        // ── Log output (scrollable terminal) ─────────────────────────────────
        GradleLogOutput(
            logLines   = logLines,
            modifier   = Modifier.weight(1f),
        )

        // ── Status bar ───────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isBuilding,
            enter   = fadeIn(),
            exit    = fadeOut(),
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color    = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ─── Toolbar ─────────────────────────────────────────────────────────────────

@Composable
private fun GradleToolbar(
    isBuilding:    Boolean,
    selectedTasks: List<String>,
    onPlay:        () -> Unit,
    onCancel:      () -> Unit,
    onClear:       () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Selected task label
        Text(
            text       = if (selectedTasks.isEmpty()) "No tasks selected"
                         else selectedTasks.joinToString(", "),
            color      = Color(0xFF8B949E),
            fontSize   = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier   = Modifier.weight(1f),
            maxLines   = 1,
        )

        Spacer(Modifier.width(8.dp))

        // Clear button
        IconButton(onClick = onClear, enabled = !isBuilding) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear log",
                tint = Color(0xFF8B949E),
            )
        }

        // Play / Cancel button
        if (isBuilding) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel build",
                    tint = Color(0xFFEF9A9A),
                )
            }
        } else {
            IconButton(
                onClick  = onPlay,
                enabled  = selectedTasks.isNotEmpty(),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run tasks",
                    tint = if (selectedTasks.isNotEmpty()) Color(0xFFA5D6A7) else Color(0xFF484F58),
                )
            }
        }
    }
}

// ─── Task chips ───────────────────────────────────────────────────────────────

@Composable
private fun TaskChipRow(
    tasks:         List<String>,
    selectedTasks: List<String>,
    onToggle:      (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
    ) {
        tasks.forEach { task ->
            val selected = task in selectedTasks
            FilterChip(
                selected = selected,
                onClick  = { onToggle(task) },
                label    = {
                    Text(
                        text       = task,
                        fontSize   = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                },
                modifier = Modifier.padding(end = 4.dp),
                shape    = RoundedCornerShape(4.dp),
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor     = Color(0xFF1F6FEB).copy(alpha = 0.3f),
                    selectedLabelColor         = Color(0xFF58A6FF),
                    containerColor             = Color(0xFF21262D),
                    labelColor                 = Color(0xFF8B949E),
                ),
            )
        }
    }
}

// ─── Log output ───────────────────────────────────────────────────────────────

@Composable
private fun GradleLogOutput(
    logLines: List<LogDisplayItem>,
    modifier:  Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new lines arrive
    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) {
            listState.animateScrollToItem(logLines.size - 1)
        }
    }

    LazyColumn(
        state    = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1117))
            .padding(horizontal = 8.dp),
    ) {
        items(logLines, key = { it.id }) { item ->
            LogLine(item)
        }
    }
}

@Composable
private fun LogLine(item: LogDisplayItem) {
    val color = logLevelColor(item.level)
    val weight = if (item.isResult) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
    ) {
        // Level badge
        Text(
            text       = item.level.take(5).padEnd(5),
            color      = color.copy(alpha = 0.7f),
            fontSize   = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = weight,
            modifier   = Modifier.width(44.dp),
        )

        Spacer(Modifier.width(6.dp))

        // Log text
        Text(
            text       = item.text,
            color      = color,
            fontSize   = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = weight,
            softWrap   = true,
        )
    }
}
