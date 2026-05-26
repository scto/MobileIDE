// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mobile.ide.R
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectorySelector(
    initialPath: String = "/storage/emulated/0",
    onPathSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val rootPath = "/storage/emulated/0"
    var currentPath by remember { mutableStateOf(initialPath.ifEmpty { rootPath }) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val directoryList by
        remember(currentPath) {
            derivedStateOf {
                try {
                    val currentDir = File(currentPath)
                    if (currentDir.exists() && currentDir.isDirectory && currentDir.canRead()) {
                        currentDir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name.lowercase() }
                            ?: emptyList()
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
        }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            currentPath = currentPath,
            onDismiss = { showCreateFolderDialog = false },
            onFolderCreated = { newFolderName ->
                try {
                    val newFolder = File(currentPath, newFolderName)
                    if (newFolder.exists()) {
                        // Handling folder exists
                    } else if (newFolder.mkdir()) {
                        val tempPath = currentPath
                        currentPath = newFolder.absolutePath
                        currentPath = tempPath
                    } else {
                        // Handling error
                    }
                } catch (e: Exception) {
                    // Handling exception
                }
                showCreateFolderDialog = false
            },
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f), shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.dir_selector_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Row {
                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(
                                Icons.Default.CreateNewFolder,
                                contentDescription = stringResource(R.string.dir_selector_create_folder_desc),
                            )
                        }
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.dir_selector_close_desc),
                            )
                        }
                    }
                }

                PathBreadcrumbs(
                    path = currentPath,
                    rootPath = rootPath,
                    onPathSegmentClicked = { newPath -> currentPath = newPath },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color,
                )

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (currentPath != rootPath && File(currentPath).parent != null) {
                            item {
                                Card(
                                    onClick = { currentPath = File(currentPath).parent ?: rootPath },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = stringResource(R.string.dir_selector_go_back),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.dir_selector_go_back),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                }
                            }
                        }

                        items(directoryList, key = { it.absolutePath }) { file ->
                            Card(onClick = { currentPath = file.absolutePath }, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = stringResource(R.string.dir_selector_dir_desc),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        if (directoryList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(R.string.dir_selector_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    FastScrollbar(
                        listState = listState,
                        modifier = Modifier.align(Alignment.CenterEnd).offset(x = 12.dp),
                    )
                }

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.dir_selector_cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onPathSelected(currentPath) }) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.dir_selector_select_dir))
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun FastScrollbar(listState: LazyListState, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo.size

    if (totalItems <= visibleItems) return

    val thumbHeightDp = 48.dp

    val scrollProgress =
        remember(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset,
            totalItems,
            visibleItems,
            isDragging,
        ) {
            if (isDragging) {
                null
            } else if (totalItems > visibleItems) {
                val index = listState.firstVisibleItemIndex.toFloat()
                val offset = listState.firstVisibleItemScrollOffset.toFloat()
                val itemHeight = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
                (index + offset / itemHeight.toFloat()) / (totalItems - visibleItems).coerceAtLeast(1).toFloat()
            } else {
                0f
            }
        }

    BoxWithConstraints(
        modifier = modifier.fillMaxHeight().padding(end = 8.dp, top = 24.dp, bottom = 24.dp).width(40.dp)
    ) {
        val maxHeight = constraints.maxHeight.toFloat()
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
        val availableHeight = maxHeight - thumbHeightPx

        val thumbOffsetY =
            if (isDragging) {
                dragOffset.coerceIn(0f, availableHeight)
            } else {
                (scrollProgress ?: 0f) * availableHeight
            }

        Box(
            modifier =
                Modifier.align(Alignment.Center).width(40.dp).fillMaxHeight().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragOffset = (offset.y - thumbHeightPx / 2f).coerceIn(0f, availableHeight)
                            val progress = dragOffset / availableHeight
                            val maxScrollIndex = (totalItems - 1).coerceAtLeast(0)
                            val targetIndex = (progress * maxScrollIndex.toFloat()).toInt().coerceIn(0, maxScrollIndex)
                            coroutineScope.launch { listState.scrollToItem(targetIndex, scrollOffset = 0) }
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                    ) { change, dragAmount ->
                        change.consume()
                        dragOffset = (dragOffset + dragAmount.y).coerceIn(0f, availableHeight)
                        val progress = dragOffset / availableHeight
                        val maxScrollIndex = (totalItems - 1).coerceAtLeast(0)
                        val targetIndex = (progress * maxScrollIndex.toFloat()).toInt().coerceIn(0, maxScrollIndex)
                        coroutineScope.launch { listState.scrollToItem(targetIndex, scrollOffset = 0) }
                    }
                }
        ) {
            Box(
                modifier =
                    Modifier.align(Alignment.Center)
                        .width(3.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )
        }

        Box(
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .offset(y = with(density) { thumbOffsetY.toDp() })
                    .width(6.dp)
                    .height(thumbHeightDp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isDragging) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragOffset = thumbOffsetY
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                        ) { change, dragAmount ->
                            change.consume()
                            dragOffset = (dragOffset + dragAmount.y).coerceIn(0f, availableHeight)
                            val progress = dragOffset / availableHeight
                            val maxScrollIndex = (totalItems - 1).coerceAtLeast(0)
                            val targetIndex = (progress * maxScrollIndex.toFloat()).toInt().coerceIn(0, maxScrollIndex)
                            coroutineScope.launch { listState.scrollToItem(targetIndex, scrollOffset = 0) }
                        }
                    }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFolderDialog(currentPath: String, onDismiss: () -> Unit, onFolderCreated: (String) -> Unit) {
    val context = LocalContext.current
    var folderName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dir_selector_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.dir_selector_location, currentPath),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                OutlinedTextField(
                    value = folderName,
                    onValueChange = {
                        folderName = it
                        errorMessage =
                            when {
                                it.isEmpty() -> null
                                it.contains('/') || it.contains('\\') ->
                                    context.getString(R.string.dir_selector_error_slash)
                                it == "." || it == ".." -> context.getString(R.string.dir_selector_error_invalid)
                                else -> null
                            }
                    },
                    label = { Text(stringResource(R.string.dir_selector_folder_name)) },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotEmpty() && errorMessage == null) {
                        onFolderCreated(folderName)
                    }
                },
                enabled = folderName.isNotEmpty() && errorMessage == null,
            ) {
                Text(stringResource(R.string.dir_selector_btn_create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dir_selector_cancel)) } },
    )
}

@Composable
private fun PathBreadcrumbs(path: String, rootPath: String, onPathSegmentClicked: (String) -> Unit) {
    data class PathSegment(val name: String, val fullPath: String, val isClickable: Boolean)

    val segments by
        remember(path, rootPath) {
            derivedStateOf {
                val pathParts = path.split('/').filter { it.isNotEmpty() }
                val rootParts = rootPath.split('/').filter { it.isNotEmpty() }

                val result = mutableListOf<PathSegment>()
                var currentPathBuilder = ""

                pathParts.forEachIndexed { index, part ->
                    currentPathBuilder += "/$part"
                    val clickable = index >= rootParts.size - 1
                    result.add(PathSegment(part, currentPathBuilder, clickable))
                }
                result
            }
        }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(segments.size) { index ->
            val segment = segments[index]
            val isLast = index == segments.size - 1

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = segment.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        when {
                            isLast -> MaterialTheme.colorScheme.primary
                            !segment.isClickable -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    modifier =
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = segment.isClickable && !isLast) {
                                onPathSegmentClicked(segment.fullPath)
                            }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                )

                if (!isLast) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
