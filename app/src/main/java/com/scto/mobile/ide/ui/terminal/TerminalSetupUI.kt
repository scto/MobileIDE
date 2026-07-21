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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun ToolchainSelectionDialog(
    onConfirmSelection: (Set<String>) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val items = remember {
        listOf(
            // OpenJDK
            ToolchainItem("openjdk-17", "openjdk-17", isRecommended = false, category = "OpenJDK"),
            ToolchainItem("openjdk-21", "openjdk-21", isRecommended = true, category = "OpenJDK"),
            ToolchainItem("openjdk-24", "openjdk-24", isRecommended = false, category = "OpenJDK"),
            // Build-Tools
            ToolchainItem("build-tools-36.0.1-RC", "build-tools 36.0.1-RC", isRecommended = false, category = "Build-Tools"),
            ToolchainItem("build-tools-36.0.1", "build-tools 36.0.1", isRecommended = false, category = "Build-Tools"),
            ToolchainItem("build-tools-35.0.1", "build-tools 35.0.1", isRecommended = true, category = "Build-Tools"),
            ToolchainItem("build-tools-34.0.0", "build-tools 34.0.0", isRecommended = false, category = "Build-Tools"),
            ToolchainItem("build-tools-33.0.2", "build-tools 33.0.2", isRecommended = false, category = "Build-Tools"),
            ToolchainItem("build-tools-33.0.1", "build-tools 33.0.1", isRecommended = false, category = "Build-Tools"),
            ToolchainItem("build-tools-32.0.0", "build-tools 32.0.0", isRecommended = false, category = "Build-Tools"),
        )
    }

    // Default-State: Recommended packages pre-checked
    val selectedIds = remember {
        mutableStateSetOf<String>().apply {
            addAll(items.filter { it.isRecommended }.map { it.id })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Entwicklungstools auswählen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val grouped = items.groupBy { it.category }
                grouped.forEach { (category, categoryItems) ->
                    item {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    items(categoryItems) { item ->
                        val isChecked = selectedIds.contains(item.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isChecked) {
                                        selectedIds.remove(item.id)
                                    } else {
                                        selectedIds.add(item.id)
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedIds.add(item.id) else selectedIds.remove(item.id)
                                }
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
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "Recommended",
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
                onClick = { onConfirmSelection(selectedIds) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Installieren")
            }
        }
    )
}

@Composable
fun TerminalSetupBottomSheet(
    setupState: SetupState,
    onExpandToggle: () -> Unit,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val durationText = remember(setupState.startTimeMs) {
        if (setupState.startTimeMs == 0L) ""
        else {
            val elapsedSec = (System.currentTimeMillis() - setupState.startTimeMs) / 1000
            val min = elapsedSec / 60
            val sec = elapsedSec % 60
            String.format("%02d:%02d verstrichen", min, sec)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = setupState.status.ifEmpty { "Terminal Setup läuft..." },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (durationText.isNotEmpty()) {
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onExpandToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "Einklappen" else "Ausklappen"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Installation Output Logs:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                            .height(200.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
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
