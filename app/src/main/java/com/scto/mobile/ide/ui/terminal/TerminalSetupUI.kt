/*
 * MobileIDE - ToolchainSelectionDialog and TerminalSetupBottomSheet
 */

package com.scto.mobile.ide.ui.terminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class ToolchainItem(
    val id: String,
    val name: String,
    val isRecommended: Boolean = false,
    val category: String
)

@Composable
fun JdkSelectionDialog(
    onConfirmSelection: (String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val versions = remember {
        listOf(
            ToolchainItem("openjdk-17", "OpenJDK 17", isRecommended = false, category = "Java Development Kit"),
            ToolchainItem("openjdk-21", "OpenJDK 21", isRecommended = true, category = "Java Development Kit"),
            ToolchainItem("openjdk-24", "OpenJDK 24", isRecommended = false, category = "Java Development Kit")
        )
    }
    var selectedId by remember { mutableStateOf("openjdk-21") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "OpenJDK-Version auswählen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bitte wähle die gewünschte Java Development Kit Version:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                versions.forEach { item ->
                    val isSelected = selectedId == item.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedId = item.id }
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedId = item.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.isRecommended) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Empfohlen",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSelection(selectedId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Weiter")
            }
        }
    )
}

@Composable
fun BuildToolsSelectionDialog(
    onConfirmSelection: (String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val versions = remember {
        listOf(
            ToolchainItem("build-tools-36.0.1-RC", "Build-Tools 36.0.1-RC", isRecommended = false, category = "Android Build-Tools"),
            ToolchainItem("build-tools-36.0.1", "Build-Tools 36.0.1", isRecommended = false, category = "Android Build-Tools"),
            ToolchainItem("build-tools-35.0.1", "Build-Tools 35.0.1", isRecommended = true, category = "Android Build-Tools"),
            ToolchainItem("build-tools-34.0.0", "Build-Tools 34.0.0", isRecommended = false, category = "Android Build-Tools"),
            ToolchainItem("build-tools-33.0.2", "Build-Tools 33.0.2", isRecommended = false, category = "Android Build-Tools"),
            ToolchainItem("build-tools-32.0.0", "Build-Tools 32.0.0", isRecommended = false, category = "Android Build-Tools")
        )
    }
    var selectedId by remember { mutableStateOf("build-tools-35.0.1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Android Build-Tools auswählen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bitte wähle die gewünschte Android Build-Tools Version:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(versions) { item ->
                        val isSelected = selectedId == item.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedId = item.id }
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedId = item.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (item.isRecommended) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Empfohlen",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSelection(selectedId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Installieren")
            }
        }
    )
}

@Composable
fun PlatformSelectionDialog(
    onConfirmSelection: (String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val versions = remember {
        listOf(
            ToolchainItem("35", "Android 15 (API 35)", isRecommended = true, category = "Android Platform"),
            ToolchainItem("34", "Android 14 (API 34)", isRecommended = false, category = "Android Platform"),
            ToolchainItem("33", "Android 13 (API 33)", isRecommended = false, category = "Android Platform")
        )
    }
    var selectedId by remember { mutableStateOf("35") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Android Platform API auswählen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bitte wähle das gewünschte Android Platform SDK (API Level):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                versions.forEach { item ->
                    val isSelected = selectedId == item.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedId = item.id }
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedId = item.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.isRecommended) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Empfohlen",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSelection(selectedId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Weiter")
            }
        }
    )
}

@Composable
fun NdkSelectionDialog(
    onConfirmSelection: (String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val versions = remember {
        listOf(
            ToolchainItem("30.0.14904198", "Version 30.0.14904198", isRecommended = true, category = "Android NDK")
        )
    }
    var selectedId by remember { mutableStateOf("30.0.14904198") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ndk",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bitte wähle die gewünschte Android NDK Version:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                versions.forEach { item ->
                    val isSelected = selectedId == item.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedId = item.id }
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedId = item.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.isRecommended) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Empfohlen",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSelection(selectedId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Installieren")
            }
        }
    )
}

@Composable
fun CmakeSelectionDialog(
    onConfirmSelection: (String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val versions = remember {
        listOf(
            ToolchainItem("3.18", "Version 3.18", isRecommended = false, category = "CMake"),
            ToolchainItem("3.22", "Version 3.22", isRecommended = true, category = "CMake"),
            ToolchainItem("4.1.2", "Version 4.1.2", isRecommended = false, category = "CMake")
        )
    }
    var selectedId by remember { mutableStateOf("3.22") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cmake",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bitte wähle die gewünschte CMake Version:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(versions) { item ->
                        val isSelected = selectedId == item.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedId = item.id }
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedId = item.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (item.isRecommended) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Empfohlen",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSelection(selectedId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Installieren")
            }
        }
    )
}

@Composable
fun SetupProgressOverlay(
    progress: Float, // 0.0f bis 1.0f
    elapsedSeconds: Long,
    estimatedTotalSeconds: Long,
    title: String = "Linux RootFS wird installiert..."
) {
    val remainingSeconds = maxOf(0L, estimatedTotalSeconds - elapsedSeconds)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, fontWeight = FontWeight.Bold)
                Text(text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Verstrichen: ${formatTime(elapsedSeconds)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Restzeit ca.: ${formatTime(remainingSeconds)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

@Composable
fun TerminalSetupOverlayWindow(
    setupState: SetupState,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    val elapsedSeconds = remember(setupState.startTimeMs) {
        if (setupState.startTimeMs == 0L) 0L
        else (System.currentTimeMillis() - setupState.startTimeMs) / 1000
    }
    val progress = if (setupState.percentage >= 0f) setupState.percentage else 0f
    val estimatedTotalSeconds = if (progress > 0.05f) (elapsedSeconds / progress).toLong() else 180L
    val remainingSeconds = maxOf(0L, estimatedTotalSeconds - elapsedSeconds)

    val durationText = remember(elapsedSeconds, remainingSeconds) {
        if (elapsedSeconds == 0L) ""
        else "Verstrichen: ${formatTime(elapsedSeconds)} | Restzeit ca.: ${formatTime(remainingSeconds)}"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title & Action Buttons (Expand/Collapse, Share & Clear)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (setupState.isActive) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (setupState.isSuccess) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Status",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = setupState.status.ifEmpty { "Terminal-Setup läuft..." },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (setupState.percentage >= 0f) {
                            Text(
                                text = "${(setupState.percentage * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (durationText.isNotEmpty()) {
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Expand / Collapse Toggle Button
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Einklappen" else "Ausklappen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Share Button
                IconButton(
                    onClick = {
                        val logText = setupState.logs.joinToString("\n")
                        if (logText.isNotEmpty()) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "MobileIDE Terminal Setup Log")
                                putExtra(android.content.Intent.EXTRA_TEXT, logText)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Setup Log teilen"))
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Logs teilen",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Clear Button
                IconButton(onClick = onClearLogs) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Logs leeren",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Bar
                    if (setupState.percentage >= 0f) {
                        LinearProgressIndicator(
                            progress = { setupState.percentage },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Installation Output Logs:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Scrollable Log Window
                    val lazyListState = rememberLazyListState()
                    LaunchedEffect(setupState.logs.size) {
                        if (setupState.logs.isNotEmpty()) {
                            lazyListState.animateScrollToItem(setupState.logs.size - 1)
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        items(setupState.logs) { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF00FF66)
                            )
                        }
                    }
                }
            }
        }
    }
}
