package com.scto.mobile.ide.core.tooling.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.scto.mobile.ide.core.tooling.impl.GradleTask

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradleTasksDialog(
    tasks: List<GradleTask>,
    onDismiss: () -> Unit,
    onRunTasks: (taskNames: List<String>, flags: List<String>) -> Unit
) {
    val selectedTasks = remember { mutableStateMapOf<String, Boolean>() }
    val selectedStandardFlags = remember { mutableStateMapOf<String, Boolean>() }
    var extraFlagsText by remember { mutableStateOf("") }
    var isFlagsExpanded by remember { mutableStateOf(false) }

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

    val groupedTasks = remember(tasks) {
        tasks.groupBy { it.group ?: "Other" }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Gradle Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Collapsible Flags Section
                OutlinedCard(
                    onClick = { isFlagsExpanded = !isFlagsExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gradle Flags (${selectedStandardFlags.filter { it.value }.size} active)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isFlagsExpanded) "▼" else "▲",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (isFlagsExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
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

                            Spacer(modifier = Modifier.height(8.dp))
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

                Spacer(modifier = Modifier.height(12.dp))

                // Grouped Tasks List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    groupedTasks.forEach { (groupName, groupTasks) ->
                        item {
                            Text(
                                text = groupName.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        items(groupTasks) { task ->
                            val isChecked = selectedTasks[task.name] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTasks[task.name] = !isChecked }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { selectedTasks[task.name] = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = task.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    task.description?.let { desc ->
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons: Abbrechen / OK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val activeTasks = selectedTasks.filter { it.value }.keys.toList()
                            val activeFlags = selectedStandardFlags.filter { it.value }.keys.toList() +
                                    extraFlagsText.split(" ").filter { it.isNotBlank() }
                            if (activeTasks.isNotEmpty()) {
                                onRunTasks(activeTasks, activeFlags)
                            }
                            onDismiss()
                        },
                        enabled = selectedTasks.any { it.value }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
