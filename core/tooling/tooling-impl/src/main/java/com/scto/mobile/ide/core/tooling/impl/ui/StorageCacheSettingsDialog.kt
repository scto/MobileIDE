package com.scto.mobile.ide.core.tooling.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scto.mobile.ide.core.tooling.impl.cache.DependencyCacheItem
import com.scto.mobile.ide.core.tooling.impl.cache.GradleCacheAnalyzer
import com.scto.mobile.ide.core.tooling.impl.cache.StorageCacheSummary
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val df = DecimalFormat("#,##0.#")
    return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCacheSettingsDialog(
    projectPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var summary by remember { mutableStateOf<StorageCacheSummary?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var confirmDialogType by remember { mutableStateOf<ConfirmActionType?>(null) }
    var actionMessage by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            isLoading = true
            summary = GradleCacheAnalyzer.analyzeStorageCache(context, projectPath)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.9f),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Speicher & Cache Verwaltung", style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = { refresh() }, enabled = !isLoading) {
                    Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (summary != null) {
                    val sum = summary!!
                    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                    Text(
                        text = "Zuletzt aktualisiert: ${sdf.format(Date(sum.lastUpdatedTimeMs))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Storage Breakdown Cards
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Gesamter belegter Speicher: ${formatBytes(sum.totalBytes)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            StorageCategoryRow("Gradle-Cache", formatBytes(sum.gradleCacheBytes), MaterialTheme.colorScheme.primary)
                            StorageCategoryRow("Android SDK Cache", formatBytes(sum.androidSdkCacheBytes), MaterialTheme.colorScheme.tertiary)
                            StorageCategoryRow("Build-Outputs (build/)", formatBytes(sum.buildOutputsBytes), MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Actions Row
                    Text("Verwaltungs-Aktionen", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { confirmDialogType = ConfirmActionType.CLEAR_ORPHANED },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Verwaiste löschen", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { confirmDialogType = ConfirmActionType.CLEAR_BUILD },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Build-Outputs leeren", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { confirmDialogType = ConfirmActionType.CLEAR_ALL },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Gradle-Cache leeren", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Top Dependencies List
                    Text("Größte Einzelabhängigkeiten", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(sum.topDependencies) { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.coordinate, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(item.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(formatBytes(item.sizeBytes), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )

    // Confirmation Dialogs
    confirmDialogType?.let { type ->
        val title = when (type) {
            ConfirmActionType.CLEAR_ALL -> "Gesamten Gradle-Cache löschen?"
            ConfirmActionType.CLEAR_ORPHANED -> "Verwaiste Einträge löschen?"
            ConfirmActionType.CLEAR_BUILD -> "Build-Outputs leeren?"
        }
        val msg = when (type) {
            ConfirmActionType.CLEAR_ALL -> "Warnung: Beim nächsten Build müssen alle Bibliotheken neu heruntergeladen werden (Internetverbindung erforderlich). Freizugebener Speicher: ${formatBytes(summary?.gradleCacheBytes ?: 0L)}"
            ConfirmActionType.CLEAR_ORPHANED -> "Es werden nur ungenutzte Bibliotheks-Caches entfernt. Freizugebener Speicher: ${formatBytes(summary?.orphanedBytes ?: 0L)}"
            ConfirmActionType.CLEAR_BUILD -> "Alle build/-Verzeichnisse des Projekts werden geleert. Freizugebener Speicher: ${formatBytes(summary?.buildOutputsBytes ?: 0L)}"
        }

        AlertDialog(
            onDismissRequest = { confirmDialogType = null },
            title = { Text(title) },
            text = { Text(msg) },
            confirmButton = {
                Button(
                    onClick = {
                        val targetType = confirmDialogType
                        confirmDialogType = null
                        scope.launch {
                            isLoading = true
                            when (targetType) {
                                ConfirmActionType.CLEAR_ALL -> GradleCacheAnalyzer.clearEntireGradleCache(context)
                                ConfirmActionType.CLEAR_ORPHANED -> GradleCacheAnalyzer.clearOrphanedCache(context)
                                ConfirmActionType.CLEAR_BUILD -> GradleCacheAnalyzer.clearBuildOutputs(projectPath)
                                else -> {}
                            }
                            refresh()
                        }
                    },
                    colors = if (type == ConfirmActionType.CLEAR_ALL) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) {
                    Text("Löschen bestätigen")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDialogType = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

enum class ConfirmActionType {
    CLEAR_ALL,
    CLEAR_ORPHANED,
    CLEAR_BUILD
}

@Composable
fun StorageCategoryRow(label: String, sizeText: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
        Text(sizeText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
