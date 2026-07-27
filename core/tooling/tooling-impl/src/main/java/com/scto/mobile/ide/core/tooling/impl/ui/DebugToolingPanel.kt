package com.scto.mobile.ide.core.tooling.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scto.mobile.ide.core.tooling.impl.debugger.*

@Composable
fun DebugToolingPanel(
    projectPath: String,
    modifier: Modifier = Modifier
) {
    val status by DebugSessionManager.sessionStatus.collectAsState()
    val stackFrames by DebugSessionManager.stackFrames.collectAsState()
    val variables by DebugSessionManager.variables.collectAsState()
    val pausedLocation by DebugSessionManager.pausedLocation.collectAsState()
    val selectedFrameId by DebugSessionManager.selectedFrameId.collectAsState()

    var expressionInput by remember { mutableStateOf("") }
    var expressionResult by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Top Toolbar: Debug Control Action Buttons & Status Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Resume
                IconButton(
                    onClick = { DebugSessionManager.resume() },
                    enabled = status == DebugSessionStatus.PAUSED
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Weiter", tint = if (status == DebugSessionStatus.PAUSED) MaterialTheme.colorScheme.primary else Color.Gray)
                }
                // Pause
                IconButton(
                    onClick = { DebugSessionManager.pause() },
                    enabled = status == DebugSessionStatus.RUNNING
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pausieren", tint = if (status == DebugSessionStatus.RUNNING) MaterialTheme.colorScheme.tertiary else Color.Gray)
                }
                // Step Over
                IconButton(
                    onClick = { DebugSessionManager.stepOver() },
                    enabled = status == DebugSessionStatus.PAUSED
                ) {
                    Icon(Icons.Default.Redo, contentDescription = "Step Over", tint = if (status == DebugSessionStatus.PAUSED) MaterialTheme.colorScheme.secondary else Color.Gray)
                }
                // Step Into
                IconButton(
                    onClick = { DebugSessionManager.stepInto() },
                    enabled = status == DebugSessionStatus.PAUSED
                ) {
                    Icon(Icons.Default.South, contentDescription = "Step Into", tint = if (status == DebugSessionStatus.PAUSED) MaterialTheme.colorScheme.secondary else Color.Gray)
                }
                // Step Out
                IconButton(
                    onClick = { DebugSessionManager.stepOut() },
                    enabled = status == DebugSessionStatus.PAUSED
                ) {
                    Icon(Icons.Default.North, contentDescription = "Step Out", tint = if (status == DebugSessionStatus.PAUSED) MaterialTheme.colorScheme.secondary else Color.Gray)
                }
                // Stop
                IconButton(
                    onClick = { DebugSessionManager.stopDebugSession() },
                    enabled = status != DebugSessionStatus.IDLE && status != DebugSessionStatus.STOPPED
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop Debugger", tint = if (status != DebugSessionStatus.IDLE && status != DebugSessionStatus.STOPPED) MaterialTheme.colorScheme.error else Color.Gray)
                }
            }

            // Status Badge
            Surface(
                shape = CircleShape,
                color = when (status) {
                    DebugSessionStatus.PAUSED -> MaterialTheme.colorScheme.errorContainer
                    DebugSessionStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                    DebugSessionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = status.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Paused Location Banner
        if (pausedLocation != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pausiert an Breakpoint: ${pausedLocation?.first}:${pausedLocation?.second}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Content Area: Split View for Call Stack & Variables Tree
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Left: Call Stack
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                        text = "Call Stack",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    if (stackFrames.isEmpty()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Kein aktiver Stack Frame",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(stackFrames) { frame ->
                                val isSelected = frame.id == selectedFrameId
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { DebugSessionManager.selectStackFrame(frame.id) }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(4.dp)) {
                                        Text(
                                            text = frame.methodName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${frame.fileName}:${frame.lineNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right: Variables Tree
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                        text = "Variablen & Lokaler Kontext",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    if (variables.isEmpty()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Keine Variablen verfügbar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(variables) { variable ->
                                VariableTreeRow(variable = variable, indentLevel = 0)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Watch / Expression Evaluator Input Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = expressionInput,
                onValueChange = { expressionInput = it },
                placeholder = { Text("Ausdruck auswerten (z. B. count, this)...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = {
                    expressionResult = DebugSessionManager.evaluateExpression(expressionInput, selectedFrameId)
                },
                enabled = expressionInput.isNotBlank() && status == DebugSessionStatus.PAUSED
            ) {
                Text("Auswerten")
            }
        }

        if (expressionResult.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💡 $expressionResult",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun VariableTreeRow(variable: VariableInfo, indentLevel: Int) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(start = (indentLevel * 12).dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = variable.children.isNotEmpty()) { expanded = !expanded }
                .padding(vertical = 2.dp)
        ) {
            if (variable.children.isNotEmpty()) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = "${variable.name}: ",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = variable.value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (expanded && variable.children.isNotEmpty()) {
            variable.children.forEach { child ->
                VariableTreeRow(variable = child, indentLevel = indentLevel + 1)
            }
        }
    }
}
