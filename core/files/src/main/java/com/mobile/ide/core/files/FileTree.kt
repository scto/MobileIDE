// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.files

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.mobile.ide.core.files.R
import com.mobile.ide.core.resources.Res

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.File

data class FileNode(
    val file: File,
    val isDirectory: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTree(
    rootPath: String,
    modifier: Modifier = Modifier,
    onFileClick: (File) -> Unit
) {
    var rootFiles by remember { mutableStateOf<List<FileNode>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var containerWidth by remember { mutableStateOf(0) }
    var itemWidths by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val maxContentWidth = itemWidths.values.maxOrNull() ?: 0
    val isHorizontalScrollEnabled = maxContentWidth > containerWidth && containerWidth > 0
    val horizontalScrollState = rememberScrollState()

    var expandedNodes by remember(rootPath) { mutableStateOf(setOf(File(rootPath).path)) }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedFileNode by remember { mutableStateOf<FileNode?>(null) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    val onSmartToggle: (FileNode) -> Unit = smartToggle@{ node ->
        val path = node.file.path
        if (expandedNodes.contains(path)) {
            expandedNodes -= path
            return@smartToggle
        }
        scope.launch(Dispatchers.IO) {
            val children = node.file.listFiles()
            val childCount = children?.size ?: 0
            if (childCount != 1 || children?.first()?.isFile == true) {
                withContext(Dispatchers.Main) {
                    expandedNodes += path
                }
            } else {
                val pathsToExpend = mutableListOf<String>()
                var currentFile = node.file
                var currentChildren = children
                while (currentChildren?.size == 1 && currentChildren.first().isDirectory) {
                    pathsToExpend.add(currentFile.path)
                    val singleChild = currentChildren.first()
                    currentFile = singleChild
                    currentChildren = currentFile.listFiles()
                }
                pathsToExpend.add(currentFile.path)
                withContext(Dispatchers.Main) {
                    expandedNodes += pathsToExpend
                }
            }
        }
    }
    
    fun refreshDirectory(directory: File) {
        scope.launch {
            val path = directory.absolutePath
            if (expandedNodes.contains(path)) {
                expandedNodes -= path
                delay(20)
                expandedNodes += path
            }
        }
    }
    
    LaunchedEffect(isHorizontalScrollEnabled) {
        if (!isHorizontalScrollEnabled) {
            horizontalScrollState.animateScrollTo(0)
        }
    }
    
    LaunchedEffect(rootPath) {
        val rootFile = File(rootPath)
        if (rootFile.exists()) {
            rootFiles = listOf(FileNode(file = rootFile, isDirectory = rootFile.isDirectory))
        } else {
            rootFiles = emptyList()
        }
    }
    
    if (rootFiles.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Box(
            modifier = modifier
                .onSizeChanged { containerWidth = it.width }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        state = horizontalScrollState,
                        enabled = isHorizontalScrollEnabled
                    ),
                contentPadding = PaddingValues(end = 8.dp, top = 4.dp, bottom = 4.dp)
            ) {
                items(rootFiles, key = { it.file.path }) { node ->
                    FileNodeItem(
                        node = node,
                        depth = 0,
                        expandedNodes = expandedNodes,
                        onToggle = onSmartToggle,
                        onFileClick = onFileClick,
                        onLongClick = {
                            selectedFileNode = it
                            showBottomSheet = true
                        },
                        onWidthMeasured = { path, width ->
                            if (itemWidths[path] != width) itemWidths = itemWidths + (path to width)
                        },
                        onDisposed = { path ->
                            itemWidths = itemWidths - path
                        }
                    )
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            FileActionBottomSheet(
                node = selectedFileNode!!,
                onDismiss = { scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) showBottomSheet = false } },
                onDeleteRequest = { showDeleteConfirmationDialog = true },
                onCreateFileRequest = { showCreateFileDialog = true },
                onCreateFolderRequest = { showCreateFolderDialog = true },
                onRenameRequest = { showRenameDialog = true }
            )
        }
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text(stringResource(R.string.file_tree_confirm_deletion)) },
            text = { Text(stringResource(R.string.file_tree_delete_message, selectedFileNode?.file?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        showBottomSheet = false
                        selectedFileNode?.let { node ->
                            scope.launch {
                                val parent = node.file.parentFile ?: File(rootPath)
                                val success = withContext(Dispatchers.IO) {
                                    if (node.isDirectory) node.file.deleteRecursively() else node.file.delete()
                                }
                                  if (success) refreshDirectory(parent)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.file_tree_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) { Text(stringResource(R.string.file_tree_cancel)) }
            }
        )
    }

    if (showCreateFileDialog) {
        InputDialog(
            title = stringResource(R.string.file_tree_new_file),
            label = stringResource(R.string.file_tree_file_name),
            onDismiss = { showCreateFileDialog = false },
            onConfirm = { name ->
                showCreateFileDialog = false
                showBottomSheet = false
                selectedFileNode?.let { node ->
                    val parentDir = if (node.isDirectory) node.file else node.file.parentFile
                    parentDir?.let {
                        scope.launch {
                            try {
                                val newFile = File(it, name)
                                val success = withContext(Dispatchers.IO) { newFile.createNewFile() }
                                if (success) {
                                    Toast.makeText(context, context.getString(R.string.file_tree_file_created), Toast.LENGTH_SHORT).show()
                                    refreshDirectory(it)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.file_tree_file_exists), Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.file_tree_create_failed_reason, e.message), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        )
    }

    if (showCreateFolderDialog) {
        InputDialog(
            title = stringResource(R.string.file_tree_new_folder),
            label = stringResource(R.string.file_tree_folder_name),
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                showCreateFolderDialog = false
                showBottomSheet = false
                selectedFileNode?.let { node ->
                    val parentDir = if (node.isDirectory) node.file else node.file.parentFile
                    parentDir?.let {
                        scope.launch {
                            val newDir = File(it, name)
                            val success = withContext(Dispatchers.IO) { newDir.mkdirs() }
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.file_tree_folder_created), Toast.LENGTH_SHORT).show()
                                refreshDirectory(it)
                            } else {
                                Toast.makeText(context, context.getString(R.string.file_tree_create_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        )
    }

    if (showRenameDialog) {
        InputDialog(
            title = stringResource(R.string.file_tree_rename),
            label = stringResource(R.string.file_tree_new_name),
            initialValue = selectedFileNode?.file?.name ?: "",
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                showRenameDialog = false
                showBottomSheet = false
                selectedFileNode?.let { node ->
                    scope.launch {
                        val parent = node.file.parentFile ?: return@launch
                        val newFile = File(parent, newName)
                        val success = withContext(Dispatchers.IO) { node.file.renameTo(newFile) }
                        if (success) {
                            Toast.makeText(context, context.getString(R.string.file_tree_renamed), Toast.LENGTH_SHORT).show()
                            refreshDirectory(parent)
                        } else {
                            Toast.makeText(context, context.getString(R.string.file_tree_rename_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileNodeItem(
    node: FileNode,
    depth: Int,
    expandedNodes: Set<String>,
    onToggle: (FileNode) -> Unit,
    onFileClick: (File) -> Unit,
    onLongClick: (FileNode) -> Unit,
    onWidthMeasured: (String, Int) -> Unit,
    onDisposed: (String) -> Unit
) {
    val isExpanded = expandedNodes.contains(node.file.path)
    val animationSpec = tween<Float>(durationMillis = 150)
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "arrowAnimation",
        animationSpec = animationSpec
    )

    val children by remember(isExpanded, node) {
        derivedStateOf {
            if (isExpanded && node.isDirectory) {
                node.file.listFiles()
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    ?.map { FileNode(file = it, isDirectory = it.isDirectory) }
                    ?: emptyList()
            } else {
                emptyList()
            }
        }
    }
    
    DisposableEffect(node.file.path) {
        onDispose { onDisposed(node.file.path) }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {
                    if (node.isDirectory) {
                        onToggle(node)
                    } else {
                        onFileClick(node.file)
                    }
                },
                onLongClick = { onLongClick(node) }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { onWidthMeasured(node.file.path, it.width) }
                .padding(start = (depth * 16).dp)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (node.isDirectory) Icons.Default.Folder else Icons.Default.Description
            val tint = if (node.isDirectory) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.7f)
            if (node.isDirectory) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.file_tree_expand),
                    modifier = Modifier.size(24.dp).rotate(arrowRotation)
                )
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }
            Icon(icon, null, Modifier.size(20.dp), tint = tint)
            Spacer(modifier = Modifier.width(8.dp))
            Text(node.file.name, maxLines = 1, overflow = TextOverflow.Visible, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(100.dp))
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(150)),
            exit = shrinkVertically(animationSpec = tween(150))
        ) {
            Column {
                children.forEach { child ->
                    FileNodeItem(
                        node = child,
                        depth = depth + 1,
                        expandedNodes = expandedNodes,
                        onToggle = onToggle,
                        onFileClick = onFileClick,
                        onLongClick = onLongClick,
                        onWidthMeasured = onWidthMeasured,
                        onDisposed = onDisposed
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomSheetActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = if (color != Color.Unspecified) color else LocalContentColor.current
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tint
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = tint,
            fontSize = 16.sp
        )
    }
}

@Composable
fun FileActionBottomSheet(
    node: FileNode,
    onDismiss: () -> Unit,
    onDeleteRequest: () -> Unit,
    onCreateFileRequest: () -> Unit,
    onCreateFolderRequest: () -> Unit,
    onRenameRequest: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        BottomSheetActionItem(
            icon = Icons.Default.Description,
            text = stringResource(R.string.file_tree_new_file),
            onClick = {
                onCreateFileRequest()
                onDismiss()
            }
        )
        BottomSheetActionItem(
            icon = Icons.Default.CreateNewFolder,
            text = stringResource(R.string.file_tree_new_folder),
            onClick = {
                onCreateFolderRequest()
                onDismiss()
            }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        BottomSheetActionItem(
            icon = Icons.Default.DriveFileRenameOutline,
            text = stringResource(R.string.file_tree_rename),
            onClick = {
                onRenameRequest()
                onDismiss()
            }
        )
        BottomSheetActionItem(
            icon = Icons.Default.ContentCopy,
            text = stringResource(R.string.file_tree_copy_path),
            onClick = {
                clipboardManager.setText(AnnotatedString(node.file.absolutePath))
                Toast.makeText(context, context.getString(R.string.file_tree_path_copied), Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        )
        BottomSheetActionItem(
            icon = Icons.Default.Delete,
            text = stringResource(R.string.file_tree_delete),
            color = MaterialTheme.colorScheme.error,
            onClick = {
                onDeleteRequest()
                onDismiss()
            }
        )
    }
}

@Composable
fun InputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank()
            ) { Text(stringResource(R.string.file_tree_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.file_tree_cancel)) }
        }
    )
}