package com.scto.mobile.ide.features.layoutpreview.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Preview
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
import com.scto.mobile.ide.features.layoutpreview.ComposablePreviewTarget
import com.scto.mobile.ide.features.layoutpreview.LayoutPreviewRenderer
import com.scto.mobile.ide.features.layoutpreview.PreviewRenderState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutPreviewBottomSheet(
    projectPath: String,
    targets: List<ComposablePreviewTarget>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTargetIndex by remember { mutableStateOf(0) }
    var renderState by remember { mutableStateOf<PreviewRenderState>(PreviewRenderState.Idle) }
    var autoRefreshOnSave by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val activeTarget = targets.getOrNull(selectedTargetIndex)

    fun triggerRender() {
        activeTarget?.let { target ->
            coroutineScope.launch {
                LayoutPreviewRenderer.renderPreview(context, projectPath, target).collect { state ->
                    renderState = state
                }
            }
        }
    }

    LaunchedEffect(activeTarget) {
        if (activeTarget != null && renderState is PreviewRenderState.Idle) {
            triggerRender()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp)
        ) {
            // Header Row: Dropdown, Refresh, Auto-Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Composable Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = activeTarget?.functionName ?: "Keine Composable gewählt",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Composable Funktion") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        targets.forEachIndexed { index, target ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(target.functionName, fontWeight = FontWeight.Bold)
                                        if (target.hasPreviewAnnotation) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("@Preview", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedTargetIndex = index
                                    isDropdownExpanded = false
                                    triggerRender()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { triggerRender() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Erneut rendern", tint = MaterialTheme.colorScheme.primary)
                }

                FilterChip(
                    selected = autoRefreshOnSave,
                    onClick = { autoRefreshOnSave = !autoRefreshOnSave },
                    label = { Text("Auto-Save", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Preview Render Output Canvas
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (val state = renderState) {
                        is PreviewRenderState.Idle -> {
                            Text("Bereit zum Rendern...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is PreviewRenderState.Loading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.message, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is PreviewRenderState.Success -> {
                            if (state.bitmap != null) {
                                Image(
                                    bitmap = state.bitmap,
                                    contentDescription = "Compose Layout Preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Layout Preview aktiv (${activeTarget?.functionName})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Gedruckt / Gerendert in ${String.format("%.2f", state.renderTimeMs / 1000.0)} s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        is PreviewRenderState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF3E2723), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "❌ Kompilierungsfehler beim Preview-Render",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFFFF5252),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
