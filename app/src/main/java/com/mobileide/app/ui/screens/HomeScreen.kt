package com.mobileide.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.AppConstants
import com.mobileide.app.data.Project
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

// ══════════════════════════════════════════════════════════════════════════════
//  HomeScreen – Main entry point after Onboarding
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: IDEViewModel) {
    val context       = LocalContext.current
    val projects      by vm.projects.collectAsState()
    val recentProjects by vm.recentProjects.collectAsState()

    var showCreateDialog  by remember { mutableStateOf(false) }
    var showImportPicker  by remember { mutableStateOf(false) }
    var showOpenPicker    by remember { mutableStateOf(false) }
    var showCloneDialog   by remember { mutableStateOf(false) }
    var showOpenProjects  by remember { mutableStateOf(false) }
    var deleteTarget      by remember { mutableStateOf<Project?>(null) }

    // Logo pulse animation
    val pulse = rememberInfiniteTransition(label = "logo")
    val logoScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    LaunchedEffect(Unit) { vm.loadProjects() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Gradient header ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                IDESurface,
                                IDESurfaceVariant.copy(alpha = 0.7f),
                                IDEBackground
                            )
                        )
                    )
                    .padding(top = 48.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // App icon / logo
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(logoScale)
                            .clip(CircleShape)
                            .background(IDEBackground)
                            .border(2.dp,
                                Brush.sweepGradient(listOf(IDEPrimary, IDESecondary, IDETertiary, IDEPrimary)),
                                CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // App logo icon
                        Icon(Icons.Default.Code, null, Modifier.size(48.dp), tint = IDEPrimary)
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        AppConstants.APP_NAME,
                        style       = MaterialTheme.typography.headlineMedium,
                        fontWeight  = FontWeight.Bold,
                        color       = IDEOnBackground
                    )
                    Text(
                        "Android IDE for Termux  ·  v${AppConstants.APP_VERSION}",
                        style = MaterialTheme.typography.bodySmall,
                        color = IDEOnSurface
                    )

                    Spacer(Modifier.height(8.dp))

                    // Quick status pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill("${projects.size} project${if (projects.size != 1) "s" else ""}",
                            IDEPrimary, Icons.Default.FolderOpen)
                        StatusPill("Sora Editor", IDESecondary, Icons.Default.Code)
                        StatusPill("Kotlin 2.2", Color(0xFFCBA6F7), Icons.Default.Star)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Primary actions ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                SectionLabel("Start")

                // ── Create Project ─────────────────────────────────────────
                PrimaryActionCard(
                    icon        = Icons.Default.AddCircle,
                    title       = "Create Project",
                    description = "New Android app from a template",
                    accentColor = IDEPrimary,
                    gradient    = listOf(IDEPrimary.copy(alpha = 0.15f), IDESurface)
                ) { showCreateDialog = true }

                // ── Open Projects ──────────────────────────────────────────
                PrimaryActionCard(
                    icon        = Icons.Default.FolderOpen,
                    title       = "Open Project",
                    description = if (projects.isEmpty()) "No projects yet"
                                  else "${projects.size} project${if (projects.size != 1) "s" else ""} available",
                    accentColor = IDESecondary,
                    gradient    = listOf(IDESecondary.copy(alpha = 0.12f), IDESurface),
                    badge       = if (projects.isNotEmpty()) projects.size.toString() else null
                ) { showOpenProjects = !showOpenProjects; if (projects.isEmpty()) showOpenPicker = true }

                // Inline project list
                AnimatedVisibility(visible = showOpenProjects && projects.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        projects.take(5).forEach { project ->
                            ProjectListItem(
                                project  = project,
                                onOpen   = { vm.openProject(project); vm.navigate(Screen.EDITOR) },
                                onDelete = { deleteTarget = project }
                            )
                        }
                        if (projects.size > 5) {
                            TextButton(
                                onClick  = { showOpenProjects = false },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("+${projects.size - 5} more", color = IDEOnSurface, fontSize = 12.sp)
                            }
                        }
                        TextButton(
                            onClick  = { showOpenPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Browse filesystem…", fontSize = 12.sp)
                        }
                    }
                }

                // ── Import Project ─────────────────────────────────────────
                PrimaryActionCard(
                    icon        = Icons.Default.Archive,
                    title       = "Import Project",
                    description = "Open a project from a folder or ZIP",
                    accentColor = SyntaxNumber,
                    gradient    = listOf(SyntaxNumber.copy(alpha = 0.12f), IDESurface)
                ) { showImportPicker = true }

                // ── Clone Repository ───────────────────────────────────────
                PrimaryActionCard(
                    icon        = Icons.Default.Source,
                    title       = "Clone Repository",
                    description = "Clone from GitHub, GitLab or any Git URL",
                    accentColor = SyntaxAnnotation,
                    gradient    = listOf(SyntaxAnnotation.copy(alpha = 0.12f), IDESurface)
                ) { showCloneDialog = true }

                Spacer(Modifier.height(4.dp))
                SectionLabel("App")

                // ── Settings ───────────────────────────────────────────────
                SecondaryActionRow(
                    icon  = Icons.Default.Settings,
                    label = "Settings",
                    tint  = IDEOnSurface
                ) { vm.navigate(Screen.SETTINGS) }

                // ── Documentation ──────────────────────────────────────────
                SecondaryActionRow(
                    icon    = Icons.Default.Book,
                    label   = "Documentation",
                    tint    = IDEOnSurface,
                    trailing = {
                        Icon(Icons.Default.OpenInNew, null,
                            Modifier.size(14.dp), tint = IDEOutline)
                    }
                ) {
                    try {
                        val i = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.DOCS_URL))
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(i)
                    } catch (_: Exception) {}
                }

                SecondaryActionRow(
                    icon  = Icons.Default.BugReport,
                    label = "Report an Issue",
                    tint  = IDEOnSurface,
                    trailing = {
                        Icon(Icons.Default.OpenInNew, null,
                            Modifier.size(14.dp), tint = IDEOutline)
                    }
                ) {
                    try {
                        val i = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.ISSUES_URL))
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(i)
                    } catch (_: Exception) {}
                }

                SecondaryActionRow(
                    icon  = Icons.Default.AutoFixHigh,
                    label = "Setup Wizard",
                    tint  = IDEOnSurface
                ) { vm.navigate(Screen.SETUP_WIZARD) }

                Spacer(Modifier.height(24.dp))

                // Footer
                Text(
                    "Powered by Sora Editor · TextMate · Termux",
                    style = MaterialTheme.typography.labelSmall,
                    color = IDEOutline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate  = { name, pkg, minSdk, targetSdk, templateId ->
                vm.createProject(name, pkg, minSdk, targetSdk, templateId)
                showCreateDialog = false
            }
        )
    }

    // Import Project — folder picker
    if (showImportPicker) {
        com.mobileide.app.ui.components.FolderPickerDialog(
            title        = "Import Project",
            startPath    = "/storage/emulated/0/MobileIDEProjects",
            confirmLabel = "Import",
            onDismiss    = { showImportPicker = false },
            onConfirm    = { path ->
                val dir = java.io.File(path)
                if (dir.exists() && dir.isDirectory) {
                    vm.openProject(com.mobileide.app.data.Project(dir.name, dir.absolutePath))
                    vm.navigate(com.mobileide.app.viewmodel.Screen.EDITOR)
                }
                showImportPicker = false
            }
        )
    }

    // Open Project — folder picker (when project list is empty)
    if (showOpenPicker) {
        com.mobileide.app.ui.components.FolderPickerDialog(
            title        = "Open Project",
            startPath    = "/storage/emulated/0/MobileIDEProjects",
            confirmLabel = "Open",
            onDismiss    = { showOpenPicker = false },
            onConfirm    = { path ->
                val dir = java.io.File(path)
                if (dir.exists() && dir.isDirectory) {
                    vm.openProject(com.mobileide.app.data.Project(dir.name, dir.absolutePath))
                    vm.navigate(com.mobileide.app.viewmodel.Screen.EDITOR)
                }
                showOpenPicker = false
            }
        )
    }

    if (showCloneDialog) {
        CloneRepositoryDialog(
            vm        = vm,
            onDismiss = { showCloneDialog = false }
        )
    }

    deleteTarget?.let { project ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title   = { Text("Delete Project?") },
            text    = { Text("This permanently deletes '${project.name}' and all its files.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteProject(project); deleteTarget = null }) {
                    Text("Delete", color = IDETertiary)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Private composables
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style     = MaterialTheme.typography.labelSmall,
        color     = IDEPrimary,
        modifier  = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun StatusPill(label: String, color: Color, icon: ImageVector) {
    Surface(
        shape  = RoundedCornerShape(16.dp),
        color  = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, Modifier.size(12.dp), tint = color)
            Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PrimaryActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    gradient: List<Color>,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IDESurface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradient))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Surface(
                modifier = Modifier.size(48.dp),
                shape    = RoundedCornerShape(12.dp),
                color    = accentColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(26.dp), tint = accentColor)
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    color = IDEOnBackground)
                Text(description, fontSize = 12.sp, color = IDEOnSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.2f)
                ) {
                    Text(badge, fontSize = 11.sp, color = accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
                Spacer(Modifier.width(6.dp))
            }

            Icon(Icons.Default.ChevronRight, null,
                Modifier.size(20.dp), tint = accentColor.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun SecondaryActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = IDESurface,
        border   = BorderStroke(1.dp, IDEOutline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = tint)
            Spacer(Modifier.width(14.dp))
            Text(label, fontSize = 14.sp, color = IDEOnBackground, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
    }
}

@Composable
private fun ProjectListItem(
    project: Project,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        color    = IDESurfaceVariant,
        border   = BorderStroke(1.dp, IDEOutline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(8.dp),
                color = IDEPrimary.copy(alpha = 0.12f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Android, null, Modifier.size(20.dp), tint = IDEPrimary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(project.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    color = IDEOnBackground)
                Text(project.path, fontSize = 10.sp, color = IDEOutline,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = IDEOutline)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Create Project Dialog  — Android Studio-style templates
// ══════════════════════════════════════════════════════════════════════════════

data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val category: String,
    val defaultPackage: String = "com.example",
    val extraDeps: List<String> = emptyList()
)

val PROJECT_TEMPLATES = listOf(
    ProjectTemplate(
        id = "empty_compose",
        name = "Empty Compose Activity",
        description = "A blank activity with Jetpack Compose set up and ready to go",
        icon = Icons.Default.Layers,
        accentColor = Color(0xFF82AAFF),
        category = "Compose"
    ),
    ProjectTemplate(
        id = "compose_nav",
        name = "Compose Navigation",
        description = "Bottom-nav scaffold with multiple screens using Navigation Compose",
        icon = Icons.Default.AccountTree,
        accentColor = Color(0xFFCBA6F7),
        category = "Compose",
        extraDeps = listOf("androidx.navigation:navigation-compose:2.7.7")
    ),
    ProjectTemplate(
        id = "compose_mvvm",
        name = "Compose + ViewModel",
        description = "MVVM template with ViewModel, StateFlow and Compose UI",
        icon = Icons.Default.AccountTree,
        accentColor = Color(0xFFA6E3A1),
        category = "Compose",
        extraDeps = listOf("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    ),
    ProjectTemplate(
        id = "compose_room",
        name = "Compose + Room Database",
        description = "Local persistence with Room, ViewModel and Compose",
        icon = Icons.Default.Storage,
        accentColor = Color(0xFFFAB387),
        category = "Compose",
        extraDeps = listOf(
            "androidx.room:room-runtime:2.6.1",
            "androidx.room:room-ktx:2.6.1"
        )
    ),
    ProjectTemplate(
        id = "empty_view",
        name = "Empty Activity (View)",
        description = "Traditional View-based activity with XML layouts",
        icon = Icons.Default.Web,
        accentColor = Color(0xFF89DCEB),
        category = "Views"
    ),
    ProjectTemplate(
        id = "basic_activity",
        name = "Basic Activity (View)",
        description = "Activity with an AppBar, FAB and a Fragment",
        icon = Icons.Default.InsertDriveFile,
        accentColor = Color(0xFF89DCEB),
        category = "Views"
    ),
    ProjectTemplate(
        id = "no_activity",
        name = "No Activity",
        description = "An empty project with no pre-created activity",
        icon = Icons.Default.FolderOpen,
        accentColor = Color(0xFF6C7086),
        category = "Other"
    ),
    ProjectTemplate(
        id = "library",
        name = "Android Library",
        description = "An Android library module (.aar)",
        icon = Icons.Default.LibraryBooks,
        accentColor = Color(0xFFF38BA8),
        category = "Other"
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, packageName: String, minSdk: Int, targetSdk: Int, templateId: String) -> Unit
) {
    var step             by remember { mutableIntStateOf(0) }
    var selectedTemplate by remember { mutableStateOf(PROJECT_TEMPLATES.first()) }
    var projectName  by remember { mutableStateOf("MyApp") }
    var packageName  by remember { mutableStateOf("com.example.myapp") }
    var minSdk       by remember { mutableIntStateOf(26) }
    var targetSdk    by remember { mutableIntStateOf(35) }
    var language     by remember { mutableStateOf("Kotlin") }
    var nameError    by remember { mutableStateOf(false) }
    var pkgError     by remember { mutableStateOf(false) }
    var showMinMenu  by remember { mutableStateOf(false) }
    var showTgtMenu  by remember { mutableStateOf(false) }
    val sdkOptions = listOf(21,23,24,26,28,29,30,31,32,33,34,35)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(20.dp),
            color    = IDESurface,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Header
                Surface(color = IDESurfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (step == 1) {
                                    IconButton(onClick = { step = 0 },
                                        modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.ArrowBack, null,
                                            Modifier.size(18.dp), tint = IDEOnBackground)
                                    }
                                    Spacer(Modifier.width(6.dp))
                                }
                                Column {
                                    Text(if (step == 0) "Choose a Template" else "Configure Project",
                                        fontWeight = FontWeight.Bold, fontSize = 17.sp,
                                        color = IDEOnBackground)
                                    Text(if (step == 0) "Select a project type" else selectedTemplate.name,
                                        fontSize = 12.sp, color = IDEOnSurface)
                                }
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Close, null, Modifier.size(18.dp), tint = IDEOnSurface)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1  Template", "2  Configure").forEachIndexed { i, label ->
                                val active = step == i
                                Surface(shape = RoundedCornerShape(12.dp),
                                    color  = if (active) IDEPrimary.copy(alpha = 0.18f) else IDESurfaceVariant,
                                    border = BorderStroke(1.dp, if (active) IDEPrimary else IDEOutline)) {
                                    Text(label,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (active) IDEPrimary else IDEOnSurface)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = IDEOutline)

                // STEP 0: Template picker — plain Column + verticalScroll (no LazyColumn)
                if (step == 0) {
                    Column(modifier = Modifier.weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val categories = PROJECT_TEMPLATES.map { it.category }.distinct()
                        categories.forEach { category ->
                            Text(category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = IDEPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp))
                            PROJECT_TEMPLATES.filter { it.category == category }.forEach { tpl ->
                                TemplateCard(
                                    template   = tpl,
                                    isSelected = selectedTemplate.id == tpl.id,
                                    onClick    = { selectedTemplate = tpl }
                                )
                            }
                        }
                    }
                }

                // STEP 1: Configure fields
                if (step == 1) {
                    Column(modifier = Modifier.weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        // Template badge
                        Surface(shape = RoundedCornerShape(12.dp),
                            color  = selectedTemplate.accentColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, selectedTemplate.accentColor.copy(alpha = 0.3f))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(selectedTemplate.icon, null, Modifier.size(24.dp),
                                    tint = selectedTemplate.accentColor)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(selectedTemplate.name, fontWeight = FontWeight.SemiBold,
                                        color = IDEOnBackground, fontSize = 13.sp)
                                    Text(selectedTemplate.description, fontSize = 11.sp,
                                        color = IDEOnSurface, maxLines = 2)
                                }
                            }
                        }

                        // Project name
                        OutlinedTextField(
                            value = projectName,
                            onValueChange = {
                                projectName = it; nameError = false
                                if (packageName.startsWith("com.example.")) {
                                    val slug = it.trim().lowercase()
                                        .filter { c -> c.isLetterOrDigit() }.ifEmpty { "app" }
                                    packageName = "com.example.$slug"; pkgError = false
                                }
                            },
                            label = { Text("Project Name *") },
                            placeholder = { Text("MyAwesomeApp") },
                            isError = nameError,
                            supportingText = if (nameError) {{ Text("Name cannot be empty") }} else null,
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            colors = ideTextFieldColors(),
                            leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) }
                        )

                        // Package name
                        OutlinedTextField(
                            value = packageName,
                            onValueChange = { packageName = it; pkgError = false },
                            label = { Text("Package Name *") },
                            placeholder = { Text("com.example.myapp") },
                            isError = pkgError,
                            supportingText = if (pkgError) {{ Text("Must contain a dot, e.g. com.company.app") }}
                                             else {{ Text("Unique identifier for your app", fontSize = 10.sp) }},
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            colors = ideTextFieldColors(),
                            leadingIcon = { Icon(Icons.Default.Code, null, Modifier.size(18.dp)) }
                        )

                        // SDK selectors
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Min SDK", fontSize = 11.sp, color = IDEOnSurface,
                                    modifier = Modifier.padding(bottom = 4.dp))
                                Box {
                                    OutlinedButton(onClick = { showMinMenu = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, IDEOutline)) {
                                        Text("API $minSdk", modifier = Modifier.weight(1f),
                                            color = IDEOnBackground)
                                        Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
                                    }
                                    DropdownMenu(expanded = showMinMenu,
                                        onDismissRequest = { showMinMenu = false }) {
                                        sdkOptions.forEach { api ->
                                            DropdownMenuItem(text = {
                                                Column {
                                                    Text("API $api",
                                                        fontWeight = if (api == minSdk) FontWeight.Bold else FontWeight.Normal)
                                                    Text(sdkLabel(api), fontSize = 10.sp, color = IDEOnSurface)
                                                }
                                            }, onClick = { minSdk = api; showMinMenu = false })
                                        }
                                    }
                                }
                                Text(sdkLabel(minSdk), fontSize = 9.sp, color = IDEOnSurface)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Target SDK", fontSize = 11.sp, color = IDEOnSurface,
                                    modifier = Modifier.padding(bottom = 4.dp))
                                Box {
                                    OutlinedButton(onClick = { showTgtMenu = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, IDEOutline)) {
                                        Text("API $targetSdk", modifier = Modifier.weight(1f),
                                            color = IDEOnBackground)
                                        Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
                                    }
                                    DropdownMenu(expanded = showTgtMenu,
                                        onDismissRequest = { showTgtMenu = false }) {
                                        sdkOptions.reversed().forEach { api ->
                                            DropdownMenuItem(text = {
                                                Column {
                                                    Text("API $api",
                                                        fontWeight = if (api == targetSdk) FontWeight.Bold else FontWeight.Normal)
                                                    Text(sdkLabel(api), fontSize = 10.sp, color = IDEOnSurface)
                                                }
                                            }, onClick = { targetSdk = api; showTgtMenu = false })
                                        }
                                    }
                                }
                                Text(sdkLabel(targetSdk), fontSize = 9.sp, color = IDEOnSurface)
                            }
                        }

                        // Language
                        Column {
                            Text("Language", fontSize = 11.sp, color = IDEOnSurface,
                                modifier = Modifier.padding(bottom = 6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Kotlin","Java").forEach { lang ->
                                    FilterChip(selected = language == lang,
                                        onClick = { language = lang },
                                        label = { Text(lang) })
                                }
                            }
                        }

                        // Extra deps
                        if (selectedTemplate.extraDeps.isNotEmpty()) {
                            Surface(shape = RoundedCornerShape(8.dp), color = IDEBackground,
                                border = BorderStroke(1.dp, IDEOutline)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Additional dependencies:",
                                        fontSize = 11.sp, color = IDEOnSurface,
                                        modifier = Modifier.padding(bottom = 4.dp))
                                    selectedTemplate.extraDeps.forEach { dep ->
                                        Text("• $dep", fontSize = 10.sp, color = IDEPrimary,
                                            fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        // Save location
                        Surface(shape = RoundedCornerShape(8.dp),
                            color = IDESecondary.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, IDESecondary.copy(alpha = 0.2f))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, null, Modifier.size(14.dp), tint = IDESecondary)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "/storage/emulated/0/MobileIDEProjects/${projectName.trim().ifEmpty { "MyApp" }}",
                                    fontSize = 10.sp, color = IDESecondary,
                                    fontFamily = FontFamily.Monospace, maxLines = 2)
                            }
                        }
                    }
                }

                HorizontalDivider(color = IDEOutline)

                // Footer buttons
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (step == 0) {
                                step = 1
                            } else {
                                when {
                                    projectName.isBlank()  -> nameError = true
                                    !packageName.contains('.') -> pkgError = true
                                    else -> onCreate(
                                        projectName.trim(),
                                        packageName.trim(),
                                        minSdk,
                                        targetSdk,
                                        selectedTemplate.id
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = IDEPrimary)
                    ) {
                        if (step == 0) {
                            Text("Next")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Project", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun sdkLabel(api: Int): String = when (api) {
    21 -> "Android 5.0 Lollipop"
    23 -> "Android 6.0 Marshmallow"
    24 -> "Android 7.0 Nougat"
    26 -> "Android 8.0 Oreo"
    28 -> "Android 9.0 Pie"
    29 -> "Android 10"
    30 -> "Android 11"
    31 -> "Android 12"
    32 -> "Android 12L"
    33 -> "Android 13 Tiramisu"
    34 -> "Android 14 Upside Down Cake"
    35 -> "Android 15 Vanilla Ice Cream"
    36 -> "Android 16"
    else -> "API $api"
}
@Composable
private fun TemplateCard(template: ProjectTemplate, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = if (isSelected) template.accentColor.copy(alpha = 0.09f) else IDESurfaceVariant
        ),
        border   = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) template.accentColor else IDEOutline
        ),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp),
                color = template.accentColor.copy(alpha = 0.14f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(template.icon, null, Modifier.size(24.dp), tint = template.accentColor)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = IDEOnBackground)
                Text(template.description, fontSize = 11.sp, color = IDEOnSurface, maxLines = 2)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp),
                    tint = template.accentColor)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Import Project Dialog
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ImportProjectDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var path by remember { mutableStateOf("/sdcard/") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.Archive, null, tint = SyntaxNumber) },
        title = { Text("Import Project") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter the full path to an existing Android project folder.",
                    style = MaterialTheme.typography.bodySmall, color = IDEOnSurface)
                OutlinedTextField(
                    value       = path,
                    onValueChange = { path = it },
                    label       = { Text("Project Path") },
                    placeholder = { Text("/sdcard/MyProject") },
                    singleLine  = true,
                    modifier    = Modifier.fillMaxWidth(),
                    colors      = ideTextFieldColors()
                )
                Text("The folder must contain a settings.gradle.kts or settings.gradle file.",
                    fontSize = 11.sp, color = IDEOutline)
            }
        },
        confirmButton = {
            Button(onClick = { if (path.isNotBlank()) onImport(path.trim()) },
                enabled = path.isNotBlank(),
                colors  = ButtonDefaults.buttonColors(containerColor = SyntaxNumber)) {
                Text("Import")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ══════════════════════════════════════════════════════════════════════════════
//  Clone Repository Dialog
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun CloneRepositoryDialog(vm: IDEViewModel, onDismiss: () -> Unit) {
    var repoUrl     by remember { mutableStateOf("") }
    var targetDir   by remember { mutableStateOf("/sdcard/MobileIDEProjects/") }
    var branchName  by remember { mutableStateOf("") }
    var isCloning   by remember { mutableStateOf(false) }
    var urlError    by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.Source, null, tint = SyntaxAnnotation) },
        title = { Text("Clone Repository") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // URL
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = { repoUrl = it; urlError = false },
                    label = { Text("Repository URL") },
                    placeholder = { Text("https://github.com/user/repo.git") },
                    isError = urlError,
                    supportingText = if (urlError) {{ Text("Please enter a valid Git URL") }} else null,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Link, null, Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ideTextFieldColors()
                )

                // Target directory
                OutlinedTextField(
                    value = targetDir,
                    onValueChange = { targetDir = it },
                    label = { Text("Clone into directory") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ideTextFieldColors()
                )

                // Optional branch
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = { Text("Branch (optional, default: main)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ideTextFieldColors()
                )

                // Quick URL shortcuts
                Text("Quick fill:", fontSize = 11.sp, color = IDEOnSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("GitHub" to "https://github.com/", "GitLab" to "https://gitlab.com/").forEach { (label, prefix) ->
                        FilterChip(
                            selected = false,
                            onClick  = { if (!repoUrl.startsWith(prefix)) repoUrl = prefix },
                            label    = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Info note
                Surface(shape = RoundedCornerShape(8.dp),
                    color = IDESurfaceVariant, border = BorderStroke(1.dp, IDEOutline)) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, Modifier.size(14.dp).padding(top = 1.dp),
                            tint = IDEPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("Requires git installed in Termux:\npkg install git",
                            fontSize = 11.sp, color = IDEOnSurface, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (repoUrl.isBlank()) { urlError = true; return@Button }
                    val branchArg = if (branchName.isNotBlank()) " -b $branchName" else ""
                    val dir = targetDir.trimEnd('/') + "/" +
                              repoUrl.substringAfterLast("/").removeSuffix(".git")
                    val cmd = "mkdir -p '$targetDir' && git clone$branchArg '$repoUrl' '$dir' 2>&1"
                    vm.navigate(Screen.TERMINAL)
                    vm.runCommand(cmd)
                    // After clone, open as project
                    vm.showSnackbar("Cloning… check Terminal for progress")
                    onDismiss()
                },
                enabled = !isCloning,
                colors  = ButtonDefaults.buttonColors(containerColor = SyntaxAnnotation)
            ) {
                if (isCloning) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = IDEBackground)
                    Spacer(Modifier.width(6.dp))
                    Text("Cloning…")
                } else {
                    Icon(Icons.Default.ArrowDownward, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clone")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Shared text field colors ───────────────────────────────────────────────────
@Composable
private fun ideTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor    = IDEPrimary,
    unfocusedBorderColor  = IDEOutline,
    focusedContainerColor = IDEBackground,
    unfocusedContainerColor = IDEBackground,
    cursorColor           = IDEPrimary
)

// Alias so Kotlin doesn't need the full import
