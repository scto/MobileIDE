package com.mobileide.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.data.*
import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LogTag
import com.mobileide.app.ui.theme.*
import com.mobileide.app.utils.ProjectPanelAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ══════════════════════════════════════════════════════════════════════════════
//  ProjectSlidingPanel
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ProjectSlidingPanel(
    project: Project?,
    fileTree: List<FileNode>,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onFileClick: (FileNode) -> Unit,
    onFolderToggle: (FileNode) -> Unit,
    onNewFile: (File) -> Unit,
    onNewFolder: (File) -> Unit,
    onRenameFile: (File, String) -> Unit,
    onDeleteFile: (File) -> Unit,
    onCopyPath: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val panelWidth  = (screenWidth * 0.82f).coerceAtMost(340.dp)

    var selectedTab    by remember { mutableStateOf(PanelTab.PROJECT) }
    var viewMode       by remember { mutableStateOf(ExplorerViewMode.EXPLORER) }
    var showViewMenu   by remember { mutableStateOf(false) }

    // Parsed data — loaded lazily
    var modules        by remember { mutableStateOf<List<AndroidModule>>(emptyList()) }
    var manifestInfo   by remember { mutableStateOf<ProjectManifestInfo?>(null) }
    var strings        by remember { mutableStateOf<List<StringResource>>(emptyList()) }
    var iconStyle      by remember { mutableStateOf("Material") }
    var dataFiles      by remember { mutableStateOf<List<File>>(emptyList()) }
    var ramUsage       by remember { mutableStateOf("…") }

    // Load project data when panel opens
    LaunchedEffect(isVisible, project) {
        if (!isVisible || project == null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val root   = File(project.path)
                modules    = ProjectPanelAnalyzer.findModules(root)
                Logger.info(LogTag.PROJECT_MGR, "modules found: ${modules.size}")

                val appModuleDir = modules.firstOrNull { it.name == ":app" }
                    ?.let { File(it.path) }
                    ?: File(project.path, "app")

                if (appModuleDir.exists()) {
                    manifestInfo = ProjectPanelAnalyzer.parseManifest(appModuleDir)
                    strings      = ProjectPanelAnalyzer.parseStrings(appModuleDir)
                    iconStyle    = ProjectPanelAnalyzer.detectIconStyle(appModuleDir)
                }

                // RAM
                val rt     = Runtime.getRuntime()
                val used   = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                val total  = rt.maxMemory() / (1024 * 1024)
                ramUsage   = "$used MB / ${total} MB"

                // App data (RKB Data tab)
                val pkg = project.path.let {
                    try {
                        File(it, "app/src/main/AndroidManifest.xml").readText()
                            .let { txt -> Regex("""package="([^"]+)"""").find(txt)?.groupValues?.get(1) }
                    } catch (_: Exception) { null }
                } ?: "com.mobileide.app"
                dataFiles = ProjectPanelAnalyzer.listAppDataDirs(pkg)
            } catch (e: Exception) {
                Logger.error(LogTag.PROJECT_MGR, "panel load error: ${e.message}")
            }
        }
    }

    // Dim overlay
    AnimatedVisibility(
        visible = isVisible,
        enter   = fadeIn(tween(200)),
        exit    = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss)
        )
    }

    // Sliding panel
    AnimatedVisibility(
        visible = isVisible,
        enter   = slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it },
        exit    = slideOutHorizontally(tween(250, easing = FastOutSlowInEasing)) { -it }
    ) {
        Surface(
            modifier = modifier.width(panelWidth).fillMaxHeight(),
            color    = IDESurface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header ──────────────────────────────────────────────────
                PanelHeader(
                    projectName = project?.name ?: "No project",
                    ramUsage    = ramUsage,
                    selectedTab = selectedTab,
                    onTabSelect = { tab -> selectedTab = tab; if (tab == PanelTab.PROJECT) viewMode = ExplorerViewMode.EXPLORER },
                    viewMode    = viewMode,
                    showViewMenu = showViewMenu,
                    onViewMenuToggle = { showViewMenu = !showViewMenu },
                    onViewModeSelect = { viewMode = it; showViewMenu = false }
                )

                HorizontalDivider(color = IDEOutline)

                // ── Content ──────────────────────────────────────────────────
                when (selectedTab) {
                    PanelTab.PROJECT -> ProjectTab(
                        viewMode    = viewMode,
                        project     = project,
                        fileTree    = fileTree,
                        modules     = modules,
                        manifestInfo = manifestInfo,
                        strings     = strings,
                        iconStyle   = iconStyle,
                        onFileClick    = onFileClick,
                        onFolderToggle = onFolderToggle,
                        onNewFile      = onNewFile,
                        onNewFolder    = onNewFolder,
                        onRenameFile   = onRenameFile,
                        onDeleteFile   = onDeleteFile,
                        onCopyPath     = onCopyPath
                    )
                    PanelTab.ANDROID -> AndroidTab(
                        modules     = modules,
                        viewMode    = viewMode,
                        project     = project,
                        manifestInfo = manifestInfo,
                        strings     = strings,
                        iconStyle   = iconStyle
                    )
                    PanelTab.DATA    -> DataTab(
                        dataFiles   = dataFiles,
                        pkgName     = project?.path ?: ""
                    )
                }
            }
        }
    }
}

// ── Panel Header ───────────────────────────────────────────────────────────────

@Composable
private fun PanelHeader(
    projectName: String,
    ramUsage: String,
    selectedTab: PanelTab,
    onTabSelect: (PanelTab) -> Unit,
    viewMode: ExplorerViewMode,
    showViewMenu: Boolean,
    onViewMenuToggle: () -> Unit,
    onViewModeSelect: (ExplorerViewMode) -> Unit
) {
    Column(modifier = Modifier.background(IDESurfaceVariant)) {
        // App icon + project info + RAM
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape,
                color = IDEPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Android, null, Modifier.size(22.dp), tint = IDEPrimary)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(projectName, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = IDEOnBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("RAM: $ramUsage", fontSize = 10.sp, color = IDEOnSurface,
                    fontFamily = FontFamily.Monospace)
            }
        }

        // Tab row: Project | AndroidPE | RKB Data
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PanelTab.values().forEach { tab ->
                val isActive = tab == selectedTab
                Surface(
                    onClick = { onTabSelect(tab) },
                    shape   = RoundedCornerShape(8.dp),
                    color   = if (isActive) IDEPrimary.copy(alpha = 0.18f) else Color.Transparent,
                    border  = if (isActive) BorderStroke(1.dp, IDEPrimary.copy(alpha = 0.5f)) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        when (tab) {
                            PanelTab.PROJECT -> "Project"
                            PanelTab.ANDROID -> "AndroidPE"
                            PanelTab.DATA    -> "RKB Data"
                        },
                        modifier  = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        fontSize  = 11.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color     = if (isActive) IDEPrimary else IDEOnSurface,
                        maxLines  = 1
                    )
                }
            }
        }

        // Action bar: New Module | sort | search
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {

            Surface(onClick = {}, shape = RoundedCornerShape(8.dp),
                color = IDEPrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, IDEPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, Modifier.size(14.dp), tint = IDEPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("New Module", fontSize = 11.sp, color = IDEPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            // View mode button
            Box {
                IconButton(onClick = onViewMenuToggle, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.FilterList, null, Modifier.size(18.dp),
                        tint = if (showViewMenu) IDEPrimary else IDEOnSurface)
                }
                DropdownMenu(expanded = showViewMenu,
                    onDismissRequest = { onViewMenuToggle() }) {
                    ExplorerViewMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(viewModeIcon(mode), null, Modifier.size(16.dp),
                                        tint = if (viewMode == mode) IDEPrimary else IDEOnSurface)
                                    Spacer(Modifier.width(8.dp))
                                    Text(mode.label,
                                        color = if (viewMode == mode) IDEPrimary else IDEOnBackground)
                                }
                            },
                            onClick = { onViewModeSelect(mode) }
                        )
                    }
                }
            }

            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = IDEOnSurface)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  PROJECT TAB
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProjectTab(
    viewMode: ExplorerViewMode,
    project: Project?,
    fileTree: List<FileNode>,
    modules: List<AndroidModule>,
    manifestInfo: ProjectManifestInfo?,
    strings: List<StringResource>,
    iconStyle: String,
    onFileClick: (FileNode) -> Unit,
    onFolderToggle: (FileNode) -> Unit,
    onNewFile: (File) -> Unit,
    onNewFolder: (File) -> Unit,
    onRenameFile: (File, String) -> Unit,
    onDeleteFile: (File) -> Unit,
    onCopyPath: (File) -> Unit
) {
    when (viewMode) {
        ExplorerViewMode.EXPLORER,
        ExplorerViewMode.TREE_COMPONENT -> FileTreeView(
            fileTree    = fileTree,
            project     = project,
            onFileClick    = onFileClick,
            onFolderToggle = onFolderToggle,
            onNewFile      = onNewFile,
            onNewFolder    = onNewFolder,
            onRenameFile   = onRenameFile,
            onDeleteFile   = onDeleteFile,
            onCopyPath     = onCopyPath
        )
        ExplorerViewMode.PACKAGE        -> PackageView(project, modules)
        ExplorerViewMode.MANAGE         -> ManageView(
            modules      = modules,
            manifestInfo = manifestInfo,
            strings      = strings,
            iconStyle    = iconStyle,
            project      = project
        )
    }
}

// ── Raw file tree ──────────────────────────────────────────────────────────────

@Composable
private fun FileTreeView(
    fileTree: List<FileNode>,
    project: Project?,
    onFileClick: (FileNode) -> Unit,
    onFolderToggle: (FileNode) -> Unit,
    onNewFile: (File) -> Unit,
    onNewFolder: (File) -> Unit,
    onRenameFile: (File, String) -> Unit,
    onDeleteFile: (File) -> Unit,
    onCopyPath: (File) -> Unit
) {
    var contextNode  by remember { mutableStateOf<FileNode?>(null) }
    var showCtxMenu  by remember { mutableStateOf(false) }
    var showNewFile  by remember { mutableStateOf(false) }
    var showNewDir   by remember { mutableStateOf(false) }
    var showRename   by remember { mutableStateOf(false) }
    var showDelete   by remember { mutableStateOf(false) }

    if (fileTree.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FolderOpen, null, Modifier.size(40.dp), tint = IDEOutline)
                Text("No files visible", color = IDEOnSurface, fontSize = 13.sp)
                Text("Tap a file in the project folder", color = IDEOutline, fontSize = 11.sp)
            }
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)) {
        items(fileTree, key = { it.file.absolutePath }) { node ->
            Box {
                FileTreeRow(
                    node = node,
                    onClick = {
                        if (node.isDirectory) onFolderToggle(node) else onFileClick(node)
                    },
                    onLongClick = { contextNode = node; showCtxMenu = true }
                )
                if (contextNode?.file?.absolutePath == node.file.absolutePath && showCtxMenu) {
                    FileContextMenu(
                        node = node,
                        expanded = showCtxMenu,
                        onDismiss = { showCtxMenu = false }
                    ) { action ->
                        showCtxMenu = false
                        when (action) {
                            is FileAction.NewFile   -> { contextNode = node; showNewFile = true }
                            is FileAction.NewFolder -> { contextNode = node; showNewDir  = true }
                            is FileAction.Rename    -> { contextNode = node; showRename  = true }
                            is FileAction.Delete    -> { contextNode = node; showDelete  = true }
                            is FileAction.CopyPath  -> onCopyPath(node.file)
                        }
                    }
                }
            }
        }
    }

    contextNode?.let { ctx ->
        if (showNewFile) NewFileDialog(ctx.file.absolutePath, false, { showNewFile = false })
            { name -> onNewFile(File(ctx.file, name)) }
        if (showNewDir) NewFileDialog(ctx.file.absolutePath, true, { showNewDir = false })
            { name -> onNewFolder(File(ctx.file, name)) }
        if (showRename) RenameDialog(ctx, { showRename = false })
            { name -> onRenameFile(ctx.file, name) }
        if (showDelete) DeleteConfirmDialog(ctx, { showDelete = false }) { onDeleteFile(ctx.file) }
    }
}

@Composable
private fun FileTreeRow(
    node: FileNode,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val indent = (12 + node.depth * 14).dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = indent, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand indicator for directories
        if (node.isDirectory) {
            Icon(
                if (node.isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                null, Modifier.size(14.dp), tint = IDEOnSurface
            )
            Spacer(Modifier.width(2.dp))
        } else {
            Spacer(Modifier.width(16.dp))
        }

        // File-type icon
        val (icon, tint) = fileIcon(node)
        Icon(icon, null, Modifier.size(15.dp), tint = tint)
        Spacer(Modifier.width(7.dp))

        // Name
        Text(
            buildString {
                append(node.name)
                if (node.isDirectory && isGradleModule(node.file)) append("  (module)")
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (node.isDirectory) IDEOnBackground else IDEOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Package / Android Studio view ────────────────────────────────────────────

@Composable
private fun PackageView(project: Project?, modules: List<AndroidModule>) {
    if (project == null) return
    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {

        // Project root node
        item {
            PVNode(Icons.Default.FolderOpen, project.name, IDEPrimary, 0, bold = true) {}
        }

        // Modules
        modules.forEach { module ->
            item(key = module.name) {
                PVNode(Icons.Default.Archive, module.name, IDESecondary, 1) {}
            }
        }

        // Gradle Scripts pseudo-node
        item {
            PVNode(Icons.Default.Construction, "Gradle Scripts", Color(0xFFFAB387), 1) {}
        }

        // Gradle files
        project.path.let { root ->
            listOf("build.gradle.kts", "settings.gradle.kts", "gradle.properties",
                   "gradle/libs.versions.toml").forEach { rel ->
                val f = File(root, rel)
                if (f.exists()) item(key = rel) {
                    PVNode(gradleIcon(), rel, Color(0xFFFAB387), 2) {}
                }
            }
        }
    }
}

@Composable
private fun PVNode(
    icon: ImageVector, label: String, tint: Color,
    depth: Int, bold: Boolean = false, onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = (8 + depth * 16).dp, top = 6.dp, bottom = 6.dp, end = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = tint)
        Spacer(Modifier.width(8.dp))
        Text(label, color = tint,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodySmall)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ANDROID PE TAB
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AndroidTab(
    modules: List<AndroidModule>,
    viewMode: ExplorerViewMode,
    project: Project?,
    manifestInfo: ProjectManifestInfo?,
    strings: List<StringResource>,
    iconStyle: String
) {
    when (viewMode) {
        ExplorerViewMode.MANAGE -> ManageView(modules, manifestInfo, strings, iconStyle, project)
        else -> LazyColumn(modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {

            if (project != null) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp),
                            tint = IDEPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(project.name, fontWeight = FontWeight.Bold, color = IDEOnBackground)
                    }
                }
            }

            if (modules.isEmpty()) {
                item {
                    Text("  :app",
                        style = MaterialTheme.typography.bodySmall,
                        color = IDESecondary,
                        modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 4.dp))
                }
            } else {
                items(modules) { module ->
                    ModuleRow(module)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()
                    .padding(start = 24.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Construction, null, Modifier.size(16.dp),
                        tint = Color(0xFFFAB387))
                    Spacer(Modifier.width(8.dp))
                    Text("Gradle Scripts", color = Color(0xFFFAB387),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ModuleRow(module: AndroidModule) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(modifier = Modifier.fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(start = 24.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                null, Modifier.size(14.dp), tint = IDEOnSurface)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Archive, null, Modifier.size(16.dp), tint = IDESecondary)
            Spacer(Modifier.width(6.dp))
            Text(module.name, color = IDESecondary, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }

        // Dependencies
        AnimatedVisibility(visible = expanded && module.dependencies.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 48.dp)) {
                Text("Dependencies:", fontSize = 10.sp, color = IDEOnSurface,
                    modifier = Modifier.padding(bottom = 2.dp))
                module.dependencies.forEach { dep ->
                    Row(modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        // Dependency arrow
                        Box(modifier = Modifier.width(20.dp).height(1.dp)
                            .background(IDEPrimary.copy(alpha = 0.5f)))
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Archive, null, Modifier.size(12.dp),
                            tint = IDEPrimary.copy(alpha = 0.7f))
                        Spacer(Modifier.width(4.dp))
                        Text(dep, fontSize = 10.sp, color = IDEPrimary,
                            fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  MANAGE VIEW
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ManageView(
    modules: List<AndroidModule>,
    manifestInfo: ProjectManifestInfo?,
    strings: List<StringResource>,
    iconStyle: String,
    project: Project?
) {
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val manifest = manifestInfo ?: ProjectManifestInfo()

    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Current Module header
        item {
            Text("Current Module",
                style = MaterialTheme.typography.labelSmall,
                color = IDESecondary,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 4.dp))
        }

        // Reference card
        item {
            Card(colors = CardDefaults.cardColors(containerColor = IDEBackground),
                border = BorderStroke(1.dp, IDEOutline)) {
                Column(modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reference", fontWeight = FontWeight.SemiBold, color = IDEOnBackground)
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("This current module does not depend on another module.",
                            fontSize = 11.sp, color = IDEOnSurface, modifier = Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(12.dp),
                            color = IDESecondary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, IDESecondary.copy(alpha = 0.4f))) {
                            Text("Modules : ${modules.size}",
                                fontSize = 11.sp, color = IDESecondary, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                            Text("Select", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                            Text("Add project dependencies", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Module components grid
        item {
            val items = listOf(
                ManageItem("Activities",  Icons.Default.Smartphone,         IDESecondary,    manifest.activities.size.toString()),
                ManageItem("Permissions", Icons.Default.Lock,                IDETertiary,     manifest.permissions.size.toString()),
                ManageItem("Services",    Icons.Default.Terminal,            IDEPrimary,      manifest.services.size.toString()),
                ManageItem("Receivers",   Icons.Default.Sensors,             SyntaxAnnotation,manifest.receivers.size.toString()),
                ManageItem("Providers",   Icons.Default.Storage,             SyntaxNumber,    manifest.providers.size.toString()),
                ManageItem("Strings",     Icons.Default.Translate,           IDESecondary,    strings.size.toString()),
                ManageItem("Icons",       Icons.Default.Image,               IDEPrimary,      iconStyle),
            )
            ManageGrid(items, expandedSection) { sec ->
                expandedSection = if (expandedSection == sec) null else sec
            }
        }

        // Detail list when a section is expanded
        expandedSection?.let { section ->
            val detail: List<String> = when (section) {
                "Activities"  -> manifest.activities
                "Permissions" -> manifest.permissions
                "Services"    -> manifest.services
                "Receivers"   -> manifest.receivers
                "Providers"   -> manifest.providers
                "Strings"     -> strings.take(50).map { "${it.name} = ${it.value}" }
                else          -> emptyList()
            }
            if (detail.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = IDEBackground),
                        border = BorderStroke(1.dp, IDEOutline)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(section, fontWeight = FontWeight.SemiBold, color = IDEPrimary,
                                modifier = Modifier.padding(bottom = 6.dp))
                            detail.forEach { item ->
                                Text("• $item", fontSize = 11.sp, color = IDEOnSurface,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        // Project section
        item {
            Text("Project",
                style = MaterialTheme.typography.labelSmall,
                color = IDESecondary,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
        }

        item {
            val projectItems = listOf(
                ManageItem("Search",  Icons.Default.Search,    IDEOnSurface, "1"),
                ManageItem("Archive", Icons.Default.Archive,   IDESecondary, "1"),
                ManageItem("Analysis",Icons.Default.Analytics, IDEOnSurface, "…"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                projectItems.forEach { item ->
                    ManageCard(item, Modifier.weight(1f)) {}
                }
            }
        }
    }
}

private data class ManageItem(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val badge: String
)

@Composable
private fun ManageGrid(
    items: List<ManageItem>,
    expandedSection: String?,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item ->
                    ManageCard(item, Modifier.weight(1f)) { onSelect(item.label) }
                }
                // Fill remaining cells
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ManageCard(item: ManageItem, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = IDESurface),
        border = BorderStroke(1.dp, IDEOutline)) {
        Column(modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(item.icon, null, Modifier.size(24.dp), tint = item.tint)
            Spacer(Modifier.height(6.dp))
            Text(item.label, fontSize = 11.sp, color = IDEOnBackground, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Surface(shape = RoundedCornerShape(8.dp),
                color = item.tint.copy(alpha = 0.15f)) {
                Text(item.badge,
                    fontSize = 10.sp, color = item.tint, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  RKB DATA TAB
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DataTab(dataFiles: List<File>, pkgName: String) {
    val displayPkg = pkgName.substringAfterLast("/")
        .ifEmpty { "jkas.androidpe" }

    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp), tint = IDEPrimary)
                Spacer(Modifier.width(8.dp))
                Text(displayPkg, fontWeight = FontWeight.Bold, color = IDEOnBackground,
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            }
        }

        if (dataFiles.isEmpty()) {
            item {
                Column(modifier = Modifier.padding(start = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("cache", "code_cache", "databases", "files",
                           "no_backup", "shared_prefs").forEach { name ->
                        DataDirRow(name, IDEOnSurface)
                    }
                }
            }
        } else {
            items(dataFiles) { file ->
                DataDirRow(file.name, IDEOnSurface)
            }
        }
    }
}

@Composable
private fun DataDirRow(name: String, tint: Color) {
    Row(modifier = Modifier.fillMaxWidth()
        .padding(start = 24.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Folder, null, Modifier.size(15.dp), tint = IDEOnSurface.copy(alpha = 0.6f))
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Helpers
// ══════════════════════════════════════════════════════════════════════════════

private fun fileIcon(node: FileNode): Pair<ImageVector, Color> {
    if (node.isDirectory) return when {
        node.file.name == "src"      -> Icons.Default.Source           to IDEPrimary
        node.file.name == "res"      -> Icons.Default.Image            to SyntaxAnnotation
        node.file.name == "java"     -> Icons.Default.Code             to IDEPrimary
        node.file.name == "kotlin"   -> Icons.Default.Code             to IDEPrimary
        node.file.name == "drawable" -> Icons.Default.Image            to SyntaxAnnotation
        node.file.name == "layout"   -> Icons.Default.ViewCompact      to IDESecondary
        node.file.name == "values"   -> Icons.Default.List             to IDESecondary
        isGradleModule(node.file)    -> Icons.Default.Archive          to IDESecondary
        else                         -> Icons.Default.Folder           to IDEOnSurface
    }
    return when (node.file.extension.lowercase()) {
        "kt", "kts"   -> Icons.Default.Code           to IDEPrimary
        "java"        -> Icons.Default.Code           to SyntaxNumber
        "xml"         -> Icons.Default.Code           to IDETertiary.copy(alpha = 0.8f)
        "json"        -> Icons.Default.DataObject     to IDESecondary
        "md"          -> Icons.Default.Description    to IDEOnSurface
        "png","jpg",
        "jpeg","webp" -> Icons.Default.Image          to SyntaxAnnotation
        "gradle","kts"-> gradleIcon()                 to Color(0xFFFAB387)
        "properties"  -> Icons.Default.Settings       to IDEOnSurface
        "gitignore"   -> Icons.Default.Source         to IDEOutline
        "sh","bat"    -> Icons.Default.Terminal       to IDESecondary
        else          -> Icons.Default.InsertDriveFile to IDEOnSurface
    }
}

private fun gradleIcon() = Icons.Default.Construction

private fun isGradleModule(dir: File) =
    File(dir, "build.gradle.kts").exists() || File(dir, "build.gradle").exists()

private fun viewModeIcon(mode: ExplorerViewMode): ImageVector = when (mode) {
    ExplorerViewMode.TREE_COMPONENT -> Icons.Default.AccountTree
    ExplorerViewMode.PACKAGE        -> Icons.Default.Archive
    ExplorerViewMode.EXPLORER       -> Icons.Default.FolderOpen
    ExplorerViewMode.MANAGE         -> Icons.Default.Settings
}
