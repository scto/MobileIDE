package com.mobileide.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.data.EditorTab
import com.mobileide.app.data.FileNode
import com.mobileide.app.data.Language
import com.mobileide.app.ui.components.*
import com.mobileide.app.ui.components.ProjectSlidingPanel
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen
import java.io.File

// ── File-type icon helper ─────────────────────────────────────────────────────
private fun fileIcon(language: Language): ImageVector = when (language) {
    Language.KOTLIN, Language.GRADLE -> Icons.Default.Code
    Language.JAVA     -> Icons.Default.LocalCafe
    Language.XML      -> Icons.Default.Code
    Language.JSON     -> Icons.Default.DataArray
    Language.MARKDOWN -> Icons.Default.Description
    Language.HTML     -> Icons.Default.Code
    Language.CSS      -> Icons.Default.ColorLens
    Language.PYTHON   -> Icons.Default.Terminal
    Language.BASH     -> Icons.Default.Terminal
    else              -> Icons.Default.InsertDriveFile
}

private fun fileIconTint(language: Language) = when (language) {
    Language.KOTLIN, Language.GRADLE -> IDEPrimary
    Language.JAVA     -> IDETertiary
    Language.XML      -> IDESecondary.copy(alpha = 0.85f)
    Language.JSON     -> IDESecondary
    Language.MARKDOWN -> IDEOnSurface
    Language.HTML     -> IDETertiary.copy(alpha = 0.85f)
    Language.CSS      -> Color(0xFF89DCEB)
    else              -> IDEOnSurface
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: IDEViewModel) {
    val project      by vm.currentProject.collectAsState()
    val openTabs     by vm.openTabs.collectAsState()
    val activeIdx    by vm.activeTabIndex.collectAsState()
    val activeTab    by vm.activeTab.collectAsState()
    val fileTree     by vm.fileTree.collectAsState()
    val isBuilding   by vm.isBuilding.collectAsState()
    val buildErrors  by vm.buildErrors.collectAsState()
    val termLines    by vm.terminalLines.collectAsState()
    val builtApk     by vm.builtApkPath.collectAsState()
    val snackMsg     by vm.snackbarMessage.collectAsState()
    val recentFiles  by vm.recentFiles.collectAsState()
    val editorSettings by vm.editorSettings.collectAsState()
    val themeName    by vm.currentThemeName.collectAsState()
    val outline      by vm.codeOutline.collectAsState()
    val cursorPos: CursorPos by vm.cursorPos.collectAsState()

    var showSearch     by remember { mutableStateOf(false) }
    var showSnippets   by remember { mutableStateOf(false) }
    var showErrorPanel by remember { mutableStateOf(false) }
    var showMenu       by remember { mutableStateOf(false) }
    var showTemplate   by remember { mutableStateOf(false) }
    var showRecent     by remember { mutableStateOf(false) }
    var showOutline    by remember { mutableStateOf(false) }
    var showSlider     by remember { mutableStateOf(false) }

    val snackbarHost = remember { SnackbarHostState() }
    val tabListState = rememberLazyListState()

    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            vm.clearSnackbar()
        }
    }
    LaunchedEffect(buildErrors.isNotEmpty()) {
        if (buildErrors.isNotEmpty()) showErrorPanel = true
    }
    // Scroll tab bar to active tab
    LaunchedEffect(activeIdx) {
        if (openTabs.isNotEmpty() && activeIdx < openTabs.size) {
            tabListState.animateScrollToItem(activeIdx)
        }
    }
    // Persist session when tabs change
    LaunchedEffect(openTabs, activeIdx) {
        project?.let { proj ->
            vm.saveSession(proj.path, openTabs, activeIdx)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(modifier = Modifier.padding(pad).fillMaxSize()) {

            // ── Top Bar ────────────────────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.navigate(Screen.HOME) }) {
                        Icon(Icons.Default.ArrowBack, null, tint = IDEOnBackground)
                    }
                    Text(
                        project?.name ?: "Editor",
                        color = IDEOnBackground,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { vm.refreshFileTree(); showSlider = true }) {
                        Icon(Icons.Default.FolderOpen, null,
                            tint = if (showSlider) IDEPrimary else IDEOnSurface)
                    }
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, null, tint = IDEOnSurface)
                    }
                    IconButton(onClick = { vm.navigate(Screen.PROJECT_SEARCH) }) {
                        Icon(Icons.Default.ManageSearch, null, tint = IDEOnSurface)
                    }
                    Box {
                        IconButton(onClick = { showRecent = !showRecent }) {
                            Icon(Icons.Default.History, null, tint = IDEOnSurface)
                        }
                        RecentFilesPanel(
                            recentFiles = recentFiles, visible = showRecent,
                            onDismiss   = { showRecent = false },
                            onOpenFile  = { f -> vm.openFile(f) }
                        )
                    }
                    IconButton(
                        onClick  = { vm.saveCurrentFile() },
                        enabled  = activeTab?.isModified == true
                    ) {
                        Icon(Icons.Default.Save, null,
                            tint = if (activeTab?.isModified == true) IDESecondary else IDEOutline)
                    }
                    IconButton(onClick = { vm.buildProject() }, enabled = !isBuilding) {
                        if (isBuilding)
                            CircularProgressIndicator(
                                Modifier.size(20.dp), color = IDEPrimary, strokeWidth = 2.dp)
                        else
                            Icon(Icons.Default.PlayArrow, null, tint = IDESecondary)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = IDEOnBackground)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save All") },
                                leadingIcon = { Icon(Icons.Default.SaveAlt, null) },
                                onClick = { vm.saveAllFiles(); showMenu = false })
                            DropdownMenuItem(
                                text = { Text("Format File") },
                                leadingIcon = { Icon(Icons.Default.AutoFixHigh, null) },
                                onClick = { vm.formatCurrentFile(); showMenu = false })
                            DropdownMenuItem(
                                text = { Text("Organize Imports") },
                                leadingIcon = { Icon(Icons.Default.Sort, null) },
                                onClick = { vm.organizeImports(); showMenu = false })
                            DropdownMenuItem(
                                text = { Text("New from Template") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, null) },
                                onClick = { showTemplate = true; showMenu = false })
                            DropdownMenuItem(
                                text = { Text("Snippets") },
                                leadingIcon = { Icon(Icons.Default.LibraryBooks, null) },
                                onClick = { showSnippets = true; showMenu = false })
                            DropdownMenuItem(
                                text = { Text("Editor Settings") },
                                leadingIcon = { Icon(Icons.Default.Tune, null) },
                                onClick = { vm.navigate(Screen.EDITOR_SETTINGS); showMenu = false })
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Clean Project") },
                                leadingIcon = { Icon(Icons.Default.CleaningServices, null) },
                                onClick = { vm.cleanProject(); showMenu = false })
                            if (builtApk != null) {
                                DropdownMenuItem(
                                    text = { Text("Install APK") },
                                    leadingIcon = { Icon(Icons.Default.InstallMobile, null) },
                                    onClick = { vm.installApk(); showMenu = false })
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Git") },
                                leadingIcon = { Icon(Icons.Default.Source, null) },
                                onClick = { vm.navigate(Screen.GIT); showMenu = false })
                            DropdownMenuItem(
                                text = { Text("TODOs") },
                                leadingIcon = { Icon(Icons.Default.List, null) },
                                onClick = { vm.navigate(Screen.TODO_PANEL); showMenu = false })
                            DropdownMenuItem(
                                text = { Text("Terminal") },
                                leadingIcon = { Icon(Icons.Default.Terminal, null) },
                                onClick = { vm.navigate(Screen.TERMINAL); showMenu = false })
                            DropdownMenuItem(
                                text = { Text("LogCat") },
                                leadingIcon = { Icon(Icons.Default.BugReport, null) },
                                onClick = { vm.navigate(Screen.LOGCAT); showMenu = false })
                        }
                    }
                }
            }

            BuildStatusBanner(termLines, isBuilding, Modifier.fillMaxWidth())

            // ── Tab Bar — AndroidIDE style ─────────────────────────────────
            if (openTabs.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        state = tabListState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(openTabs) { i, tab ->
                            TabChip(
                                tab      = tab,
                                isActive = i == activeIdx,
                                onClick  = { vm.selectTab(i) },
                                onClose      = { vm.closeTab(i) },
                                onCloseOthers = { vm.closeOtherTabs(i) },
                                onCloseAll    = { vm.closeAllTabs() }
                            )
                        }
                    }
                    HorizontalDivider(color = IDEOutline)
                }
            }

            // ── Search bar ─────────────────────────────────────────────────
            SearchReplaceBar(
                visible     = showSearch,
                textContent = activeTab?.content ?: "",
                onClose     = { showSearch = false },
                onApplyReplace = { newContent ->
                    val tab = activeTab ?: return@SearchReplaceBar
                    vm.updateTabContent(activeIdx, newContent)
                }
            )

            activeTab?.let { tab ->
                BreadcrumbBar(tab.file, project?.let { File(it.path) })
            }

            // ── Single editor area (NO SPLIT) ─────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                if (activeTab != null) {
                    key(activeTab!!.file.absolutePath) {
                        SoraCodeEditor(
                            content         = activeTab!!.content,
                            language        = activeTab!!.language,
                            settings        = editorSettings,
                            themeName       = themeName,
                            onContentChange = { newContent ->
                                vm.updateTabContent(activeIdx, newContent)
                            },
                            onEditorReady   = { vm.activeEditorRef = java.lang.ref.WeakReference(it) },
                            onCursorChange = { pos -> vm.updateCursorPos(pos) },
                            modifier       = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    EmptyEditorHint(Modifier.fillMaxSize())
                }

                BuildErrorPanel(
                    errors     = buildErrors,
                    visible    = showErrorPanel,
                    onErrorClick = { err ->
                        val f = File(err.file)
                        if (f.exists()) vm.openFile(f)
                    },
                    onClose  = { showErrorPanel = false },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                )
            }

            // ── Status bar ─────────────────────────────────────────────────
            activeTab?.let { tab ->
                EditorStatusBar(
                    fileName  = tab.name,
                    lineCount = tab.content.lines().size,
                    cursorLine = cursorPos.line,
                    cursorCol  = cursorPos.column,
                    isModified = tab.isModified,
                    language   = tab.language.label
                )
            }
        }
    }

    // ── Sliding panel overlay ──────────────────────────────────────────────
    ProjectSlidingPanel(
        project        = project,
        fileTree       = fileTree,
        isVisible      = showSlider,
        onDismiss      = { showSlider = false },
        onFileClick    = { node -> vm.openFile(node.file); showSlider = false },
        onFolderToggle = { node -> vm.toggleFolder(node) },
        onNewFile      = { dir -> vm.createFile(dir, "NewFile.kt") },
        onNewFolder    = { dir -> vm.createFolder(dir, "new_folder") },
        onRenameFile   = { file, name -> vm.renameFile(file, name) },
        onDeleteFile   = { file -> vm.deleteFile(file) },
        onCopyPath     = { file -> vm.copyPathToClipboard(file) }
    )

    if (showSnippets) {
        AlertDialog(
            onDismissRequest = { showSnippets = false },
            title            = { Text("Snippets") },
            text             = { Text("Snippet manager coming soon.") },
            confirmButton    = {
                TextButton(onClick = { showSnippets = false }) { Text("Close") }
            }
        )
    }

    if (showOutline) {
        CodeOutlineDialog(
            symbols = outline,
            onDismiss = { showOutline = false },
            onNavigateTo = { showOutline = false }
        )
    }

    if (showTemplate) {
        val currentDir = activeTab?.file?.parentFile
            ?: project?.let { File(it.path, "app/src/main/java") }
        val defaultPkg = project?.let {
            runCatching {
                val mf = File(it.path, "app/src/main/AndroidManifest.xml").readText()
                Regex("""package="([^"]+)"""").find(mf)?.groupValues?.get(1) ?: "com.example"
            }.getOrDefault("com.example")
        } ?: "com.example"
        FileTemplateDialog(
            targetDir      = currentDir ?: File("."),
            defaultPackage = defaultPkg,
            onDismiss      = { showTemplate = false },
            onCreate = { fileName, content ->
                val dir = currentDir ?: return@FileTemplateDialog
                File(dir, fileName).writeText(content)
                vm.openFile(File(dir, fileName))
                project?.let { vm.buildFileTree(File(it.path)) }
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Tab chip — AndroidIDE style with context menu
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun TabChip(
    tab: EditorTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onCloseOthers: () -> Unit,
    onCloseAll: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val bgColor = if (isActive) MaterialTheme.colorScheme.background
                  else          MaterialTheme.colorScheme.surface
    val textColor = if (isActive) IDEOnBackground else IDEOnSurface

    Box {
        Row(
            modifier = Modifier
                .background(bgColor)
                .combinedClickable(
                    onClick     = onClick,
                    onLongClick = { showMenu = true }
                )
                .padding(start = 10.dp, end = 4.dp, top = 0.dp, bottom = 0.dp)
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File type icon
            Icon(
                fileIcon(tab.language),
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = if (isActive) fileIconTint(tab.language) else IDEOutline
            )
            Spacer(Modifier.width(5.dp))
            // Modified dot
            if (tab.isModified) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(IDESecondary, RoundedCornerShape(50))
                )
                Spacer(Modifier.width(4.dp))
            }
            // File name
            Text(
                tab.name,
                color    = textColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
            Spacer(Modifier.width(4.dp))
            // Close button
            IconButton(
                onClick  = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close, null,
                    Modifier.size(12.dp),
                    tint = if (isActive) IDEOnBackground else IDEOutline
                )
            }
        }

        // Active tab indicator line at bottom
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(IDEPrimary)
            )
        }

        // Context menu
        DropdownMenu(
            expanded         = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Close this file") },
                leadingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) },
                onClick = { onClose(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Close other files") },
                leadingIcon = { Icon(Icons.Default.DoneAll, null, Modifier.size(16.dp)) },
                onClick = { onCloseOthers(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Close all files") },
                leadingIcon = { Icon(Icons.Default.CloseFullscreen, null, Modifier.size(16.dp)) },
                onClick = { onCloseAll(); showMenu = false }
            )
        }
    }

    // Tab separator
    VerticalDivider(
        modifier = Modifier.height(36.dp),
        color    = IDEOutline.copy(alpha = 0.4f)
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun EmptyEditorHint(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Code, null, Modifier.size(64.dp), tint = IDEOutline)
            Spacer(Modifier.height(12.dp))
            Text("Open a file from Explorer", color = IDEOnSurface)
            Text("Powered by Sora Editor + TextMate",
                color = IDEOutline, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FileTreePanel(
    nodes: List<FileNode>, modifier: Modifier,
    onFileClick: (FileNode) -> Unit,
    onNewFile: (FileNode) -> Unit, onNewFolder: (FileNode) -> Unit,
    onRename: (FileNode, String) -> Unit, onDelete: (FileNode) -> Unit,
    onCopyPath: (FileNode) -> Unit
) {
    var ctxNode  by remember { mutableStateOf<FileNode?>(null) }
    var menuExp  by remember { mutableStateOf(false) }
    var showNewF by remember { mutableStateOf(false) }
    var showNewD by remember { mutableStateOf(false) }
    var showRen  by remember { mutableStateOf(false) }
    var showDel  by remember { mutableStateOf(false) }

    Column(modifier = modifier
        .background(MaterialTheme.colorScheme.surface)
        .verticalScroll(rememberScrollState())) {
        nodes.forEach { node ->
            Box {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .combinedClickable(
                            onClick     = { onFileClick(node) },
                            onLongClick = { ctxNode = node; menuExp = true })
                        .padding(
                            start  = (12 + node.depth * 12).dp,
                            top    = 5.dp,
                            bottom = 5.dp,
                            end    = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val ic = when (node.file.extension) {
                        "kt"  -> IDEPrimary
                        "xml" -> IDETertiary.copy(alpha = 0.8f)
                        "kts" -> IDESecondary.copy(alpha = 0.8f)
                        else  -> if (node.isDirectory) SyntaxNumber else IDEOnSurface
                    }
                    Icon(
                        if (node.isDirectory)
                            if (node.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
                        else Icons.Default.InsertDriveFile,
                        null, Modifier.size(15.dp), tint = ic
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(node.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (node.isDirectory) IDEOnBackground else IDEOnSurface)
                }
                if (ctxNode?.file?.absolutePath == node.file.absolutePath) {
                    FileContextMenu(node, menuExp, { menuExp = false }) { action ->
                        when (action) {
                            is FileAction.NewFile   -> { ctxNode = node; showNewF = true }
                            is FileAction.NewFolder -> { ctxNode = node; showNewD = true }
                            is FileAction.Rename    -> { ctxNode = node; showRen  = true }
                            is FileAction.Delete    -> { ctxNode = node; showDel  = true }
                            is FileAction.CopyPath  -> onCopyPath(node)
                        }
                    }
                }
            }
        }
    }
    val ctx = ctxNode
    if (ctx != null) {
        if (showNewF) NewFileDialog(ctx.file.absolutePath, false, { showNewF = false })
            { name -> onNewFile(FileNode(File(ctx.file, name), ctx.depth + 1)) }
        if (showNewD) NewFileDialog(ctx.file.absolutePath, true, { showNewD = false })
            { name -> onNewFolder(FileNode(File(ctx.file, name), ctx.depth + 1)) }
        if (showRen) RenameDialog(ctx, { showRen = false }) { name -> onRename(ctx, name) }
        if (showDel) DeleteConfirmDialog(ctx, { showDel = false }) { onDelete(ctx) }
    }
}

@Composable
private fun VerticalDivider(modifier: Modifier, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.material3.VerticalDivider(modifier = modifier, color = color)
}
