// Copyright 2025 Thomas Schmid
package com.mobile.ide.ui.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.lerp
import androidx.navigation.NavController

import com.mobile.ide.R
import com.mobile.ide.core.utils.LogCatcher
import com.mobile.ide.core.utils.WorkspaceManager
import com.mobile.ide.core.files.FileTree
import com.mobile.ide.ui.editor.components.CodeEditorView
import com.mobile.ide.ui.editor.viewmodel.EditorViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.File

import org.json.JSONObject

sealed class BuildResultState {
    data class Finished(val message: String, val apkPath: String? = null) : BuildResultState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditScreen(folderName: String, navController: NavController, viewModel: EditorViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isMoreMenuExpanded by remember { mutableStateOf(false) }
    val workspacePath = WorkspaceManager.getWorkspacePath(context)
    val projectPath = File(workspacePath, folderName).absolutePath

    var showInitialLoader by remember { mutableStateOf(!viewModel.hasShownInitialLoader) }
    var isBuilding by remember { mutableStateOf(false) }
    var buildResult by remember { mutableStateOf<BuildResultState?>(null) }

    val hasWebAppConfig = remember(projectPath) {
        File(projectPath, "webapp.json").exists()
    }

    LaunchedEffect(projectPath) {
        viewModel.loadInitialFile(projectPath)
    }

    LaunchedEffect(Unit) {
        if (showInitialLoader) {
            delay(500L)
            showInitialLoader = false
            viewModel.onInitialLoaderShown()
        }
    }

    DismissibleNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                FileManagerDrawer(
                    projectPath = projectPath,
                    onFileClick = { file ->
                        viewModel.openFile(file)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.app_name), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        AnimatedDrawerToggle(
                            isOpen = drawerState.isOpen,
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.undo() }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.action_undo))
                        }
                        IconButton(onClick = { viewModel.redo() }) {
                            Icon(Icons.AutoMirrored.Filled.Redo, stringResource(R.string.action_redo))
                        }
                        IconButton(onClick = {
                            scope.launch {
                                viewModel.saveAllModifiedFiles(context)
                                navController.navigate("preview/$folderName")
                            }
                        }) {
                            Icon(Icons.Filled.PlayArrow, stringResource(R.string.action_run))
                        }
                        Box {
                            IconButton(onClick = { isMoreMenuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more_options))
                            }
                            DropdownMenu(
                                expanded = isMoreMenuExpanded,
                                onDismissRequest = { isMoreMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_save_all)) },
                                    onClick = {
                                        scope.launch { viewModel.saveAllModifiedFiles(context) }
                                        isMoreMenuExpanded = false
                                    }
                                )

                                if (hasWebAppConfig) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_build_apk)) },
                                        enabled = !isBuilding,
                                        onClick = {
                                            isMoreMenuExpanded = false
                                            scope.launch {
                                                isBuilding = true
                                                viewModel.saveAllModifiedFiles(context)

                                                val configFile = File(projectPath, "webapp.json")
                                                var pkg: String? = null
                                                var verName: String? = null
                                                var verCode: String? = null
                                                var iconPath = ""
                                                var permissions: Array<String>? = null
                                                var statusBarConfig: String? = null 

                                                if (configFile.exists()) {
                                                    try {
                                                        val jsonStr = withContext(Dispatchers.IO) { configFile.readText() }
                                                        val json = JSONObject(jsonStr)

                                                        pkg = json.optString("package", "com.example.webapp")
                                                        verName = json.optString("versionName", "1.0")
                                                        verCode = json.optString("versionCode", "1")

                                                        val iconName = json.optString("icon", "")
                                                        if (iconName.isNotEmpty()) {
                                                            val iconFile = File(projectPath, iconName)
                                                            if (iconFile.exists()) {
                                                                iconPath = iconFile.absolutePath
                                                            } else {
                                                                LogCatcher.w("Build", "Icon file not found: ${iconFile.absolutePath}")
                                                            }
                                                        }

                                                        val jsonPerms = json.optJSONArray("permissions")
                                                        if (jsonPerms != null && jsonPerms.length() > 0) {
                                                            val list = ArrayList<String>()
                                                            for (i in 0 until jsonPerms.length()) {
                                                                list.add(jsonPerms.getString(i))
                                                            }
                                                            permissions = list.toTypedArray()
                                                        }

                                                        val statusBarJson = json.optJSONObject("statusBar")
                                                        if (statusBarJson != null) {
                                                            statusBarConfig = statusBarJson.toString()
                                                            LogCatcher.d("Build", "Status bar configuration: $statusBarConfig")
                                                        }

                                                    } catch (e: Exception) {
                                                        LogCatcher.e("Build", "Failed to parse webapp.json", e)
                                                    }
                                                }

                                                val result = withContext(Dispatchers.IO) {
                                                    com.mobile.ide.build.ApkBuilder.bin(
                                                        context,           
                                                        workspacePath,     
                                                        projectPath,       
                                                        folderName,        
                                                        pkg,               
                                                        verName,           
                                                        verCode,           
                                                        iconPath,          
                                                        permissions        
                                                    )
                                                }

                                                isBuilding = false

                                                if (result.startsWith("error:")) {
                                                    LogCatcher.e("Build", "Build failed: $result")
                                                    buildResult =
                                                        BuildResultState.Finished(result, null)
                                                } else {
                                                    LogCatcher.i("Build", "Build successful: $result")
                                                    buildResult = BuildResultState.Finished(
                                                        context.getString(R.string.build_successful),
                                                        result
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SymbolBar(viewModel = viewModel)
                }
            },
            content = { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    AnimatedVisibility(visible = showInitialLoader || isBuilding) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            strokeCap = StrokeCap.Butt
                        )
                    }
                    EditCode(modifier = Modifier.fillMaxSize(), viewModel = viewModel)
                }
            }
        )

        buildResult?.let { result ->
            if (result is BuildResultState.Finished) {
                val isSuccess = result.apkPath != null

                AlertDialog(
                    onDismissRequest = { buildResult = null },
                    title = {
                        Text(if (isSuccess) stringResource(R.string.build_successful) else stringResource(R.string.build_failed))
                    },
                    text = {
                        Column {
                            if (isSuccess) {
                                Text(stringResource(R.string.build_apk_generated_install))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.build_output_path), style = MaterialTheme.typography.titleSmall)
                                Text("${result.apkPath}", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text(stringResource(R.string.build_error_message), color = MaterialTheme.colorScheme.error)
                                Text(result.message)
                            }
                        }
                    },
                    confirmButton = {
                        if (isSuccess && result.apkPath != null) {
                            TextButton(onClick = {
                                installApk(context, File(result.apkPath))
                                buildResult = null
                            }) {
                                Text(stringResource(R.string.action_install))
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { buildResult = null }) {
                            Text(stringResource(R.string.action_close))
                        }
                    }
                )
            }
        }
    }
}

private suspend fun ensureKeystoreExists(context: Context, workspacePath: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val keystoreName = "WebIDE.jks"
            val targetKeystore = File(workspacePath, keystoreName)

            if (!targetKeystore.exists()) {
                try {
                    context.assets.open(keystoreName).use { input ->
                        targetKeystore.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    return@withContext null
                }
            }
            targetKeystore.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}

private fun installApk(context: Context, apkFile: File) {
    if (!apkFile.exists()) {
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(context, context.getString(R.string.msg_enable_install_perms), Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }
    }

    try {
        val authority = "${context.packageName}.provider"
        val uri = FileProvider.getUriForFile(context, authority, apkFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)

    } catch (e: Exception) {
         LogCatcher.e("Install", "Error", e)
    }
}

private fun findBuiltApk(projectPath: String, appName: String): File? {
    val buildDir = File(projectPath, "build")

    if (buildDir.exists() && buildDir.isDirectory) {
        val specificFile = File(buildDir, "${appName}_release.apk")
        if (specificFile.exists()) return specificFile

        val apks = buildDir.listFiles { _, name -> name.lowercase().endsWith(".apk") }
        if (!apks.isNullOrEmpty()) {
            return apks.maxByOrNull { it.lastModified() }
        }
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymbolBar(viewModel: EditorViewModel) {
    val symbols = listOf("Tab", "<", ">", "/", "=", "\"", "'", "!", "?", ",", ";", ":", "(", ")", "[", "]", "{", "}", "+", "-", "*", "_", "&", "|")
    BottomAppBar(modifier = Modifier.imePadding().height(48.dp)) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            symbols.forEach { symbol ->
                Box(modifier = Modifier.clickable { viewModel.insertSymbol(symbol) }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text(text = symbol, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCode(modifier: Modifier = Modifier, viewModel: EditorViewModel) {
    val openFiles = viewModel.openFiles
    val activeFileIndex = viewModel.activeFileIndex
    val scope = rememberCoroutineScope()
    var expandedTabIndex by remember { mutableStateOf<Int?>(null) }
    val currentFiles by rememberUpdatedState(openFiles)
    val currentIndex by rememberUpdatedState(activeFileIndex)
    val pagerState = rememberPagerState(initialPage = activeFileIndex.coerceIn(0, maxOf(0, openFiles.size - 1)), pageCount = { currentFiles.size })

    LaunchedEffect(currentIndex, currentFiles.size) {
        if (currentFiles.isNotEmpty() && currentIndex >= 0 && currentIndex < currentFiles.size && pagerState.currentPage != currentIndex) {
            pagerState.scrollToPage(currentIndex)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (currentFiles.isNotEmpty() && page in currentFiles.indices && page != currentIndex) {
                viewModel.changeActiveFileIndex(page)
            }
        }
    }

    Column(modifier = modifier) {
        if (openFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.msg_no_files_opened)) }
        } else {
            ScrollableTabRow(selectedTabIndex = pagerState.currentPage.coerceIn(0, openFiles.size - 1), edgePadding = 0.dp, divider = {}, indicator = { tabPositions -> if (tabPositions.isNotEmpty() && pagerState.currentPage < tabPositions.size) { Box(modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]).height(3.dp).background(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(percent = 50))) } }) {
                openFiles.forEachIndexed { index, editorState ->
                    Box {
                        val displayName = if (editorState.isModified) "*${editorState.file.name}" else editorState.file.name
                        Tab(selected = pagerState.currentPage == index, onClick = { if (pagerState.currentPage == index) expandedTabIndex = index else scope.launch { pagerState.animateScrollToPage(index) } }, text = { Text(text = displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                        DropdownMenu(expanded = expandedTabIndex == index, onDismissRequest = { expandedTabIndex = null }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.action_close)) }, onClick = { expandedTabIndex = null; viewModel.closeFile(index) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.action_close_others)) }, onClick = { expandedTabIndex = null; viewModel.closeOtherFiles(index) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.action_close_all)) }, onClick = { expandedTabIndex = null; viewModel.closeAllFiles() })
                        }
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth(), userScrollEnabled = false, key = { index -> if (index < openFiles.size) openFiles[index].file.absolutePath else "empty_$index" }) { page ->
                if (page in openFiles.indices) {
                    CodeEditorView(modifier = Modifier.fillMaxSize(), state = openFiles[page], viewModel = viewModel)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}

@Composable
fun FileManagerDrawer(projectPath: String, onFileClick: (File) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(stringResource(R.string.title_file_tree), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
        FileTree(rootPath = projectPath, modifier = Modifier.fillMaxSize(), onFileClick = onFileClick)
    }
}

@Composable
fun AnimatedDrawerToggle(
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "DrawerToggleProgress"
    )

    IconButton(onClick = onClick, modifier = modifier) {
        val color = LocalContentColor.current
        Canvas(
            modifier = Modifier
                .size(24.dp)
                .padding(2.dp)
        ) {
            val strokeWidth = 2.dp.toPx()
            val cap = StrokeCap.Round
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val yOffset = 5.dp.toPx()

            val arrowheadSize = width * 0.3f

            withTransform({
                rotate(degrees = lerp(0f, 180f, progress), pivot = Offset(x = width / 2, y = height / 2))
            }) {
                drawLine(
                    color = color,
                    strokeWidth = strokeWidth,
                    cap = cap,
                    start = Offset(x = 0f, y = centerY),
                    end = Offset(x = width, y = centerY)
                )
            }

            val bottomInitialStart = Offset(x = 0f, y = centerY + yOffset)
            val bottomInitialEnd = Offset(x = width, y = centerY + yOffset)
            val bottomFinalStart = Offset(x = arrowheadSize, y = centerY - arrowheadSize)
            val bottomFinalEnd = Offset(x = 0f, y = centerY)

            drawLine(
                color = color,
                strokeWidth = strokeWidth,
                cap = cap,
                start = lerp(bottomInitialStart, bottomFinalStart, progress),
                end = lerp(bottomInitialEnd, bottomFinalEnd, progress)
            )

            val topInitialStart = Offset(x = 0f, y = centerY - yOffset)
            val topInitialEnd = Offset(x = width, y = centerY - yOffset)
            val finalTopStart = Offset(x = arrowheadSize, y = centerY + arrowheadSize)
            val finalTopEnd = Offset(x = 0f, y = centerY)

            drawLine(
                color = color,
                strokeWidth = strokeWidth,
                cap = cap,
                start = lerp(topInitialStart, finalTopStart, progress),
                end = lerp(topInitialEnd, finalTopEnd, progress)
            )
        }
    }
}