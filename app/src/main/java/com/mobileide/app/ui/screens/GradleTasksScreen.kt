package com.mobileide.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

data class GradleTask(
    val name: String,
    val description: String,
    val group: TaskGroup,
    val command: String,
    val isFavorite: Boolean = false
)

enum class TaskGroup(val label: String, val icon: ImageVector, val color: Color) {
    BUILD   ("Build",        Icons.Default.Build,          Color(0xFF82AAFF)),
    INSTALL ("Install",      Icons.Default.InstallMobile,  Color(0xFFC3E88D)),
    TEST    ("Test",         Icons.Default.BugReport,      Color(0xFFFAB387)),
    LINT    ("Lint",         Icons.Default.FindInPage,     Color(0xFFCBA6F7)),
    CLEAN   ("Clean",        Icons.Default.CleaningServices, Color(0xFFFF9CAC)),
    REPORT  ("Reports",      Icons.Default.Assessment,     Color(0xFF89DCEB)),
    OTHER   ("Other",        Icons.Default.MoreHoriz,      Color(0xFFBAC2DE)),
}

val DEFAULT_TASKS = listOf(
    GradleTask("assembleDebug",       "Build debug APK",                   TaskGroup.BUILD,   "gradle assembleDebug"),
    GradleTask("assembleRelease",     "Build release APK",                 TaskGroup.BUILD,   "gradle assembleRelease"),
    GradleTask("bundleDebug",         "Build debug AAB",                   TaskGroup.BUILD,   "gradle bundleDebug"),
    GradleTask("bundleRelease",       "Build release AAB",                 TaskGroup.BUILD,   "gradle bundleRelease"),
    GradleTask("compileDebugKotlin",  "Compile Kotlin sources (debug)",    TaskGroup.BUILD,   "gradle compileDebugKotlin"),
    GradleTask("build",               "Full build (assemble + test)",      TaskGroup.BUILD,   "gradle build"),
    GradleTask("installDebug",        "Build and install debug APK",       TaskGroup.INSTALL, "gradle installDebug"),
    GradleTask("installRelease",      "Build and install release APK",     TaskGroup.INSTALL, "gradle installRelease"),
    GradleTask("uninstallAll",        "Uninstall all APK variants",        TaskGroup.INSTALL, "gradle uninstallAll"),
    GradleTask("test",                "Run all unit tests",                TaskGroup.TEST,    "gradle test"),
    GradleTask("testDebugUnitTest",   "Run debug unit tests",              TaskGroup.TEST,    "gradle testDebugUnitTest"),
    GradleTask("connectedAndroidTest","Run instrumented tests on device",  TaskGroup.TEST,    "gradle connectedAndroidTest"),
    GradleTask("testCoverage",        "Generate test coverage report",     TaskGroup.TEST,    "gradle createDebugCoverageReport"),
    GradleTask("lint",                "Run Lint checks",                   TaskGroup.LINT,    "gradle lint"),
    GradleTask("lintDebug",           "Run Lint on debug variant",         TaskGroup.LINT,    "gradle lintDebug"),
    GradleTask("lintFix",             "Auto-fix lint issues",              TaskGroup.LINT,    "gradle lintFix"),
    GradleTask("clean",               "Delete all build outputs",          TaskGroup.CLEAN,   "gradle clean"),
    GradleTask("cleanBuildCache",     "Clear Gradle build cache",          TaskGroup.CLEAN,   "gradle cleanBuildCache"),
    GradleTask("dependencies",        "Print dependency tree",             TaskGroup.REPORT,  "gradle app:dependencies"),
    GradleTask("dependencyInsight",   "Insight for specific dependency",   TaskGroup.REPORT,  "gradle dependencyInsight --dependency androidx.core"),
    GradleTask("tasks",               "List all available tasks",          TaskGroup.OTHER,   "gradle tasks --all"),
    GradleTask("properties",          "Print project properties",          TaskGroup.OTHER,   "gradle properties"),
    GradleTask("signingReport",       "Show signing key info",             TaskGroup.OTHER,   "gradle signingReport"),
    GradleTask("wrapper",             "Regenerate Gradle wrapper",         TaskGroup.OTHER,   "gradle wrapper --gradle-version 8.7"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradleTasksScreen(vm: IDEViewModel) {
    val project by vm.currentProject.collectAsState()
    var favorites by remember { mutableStateOf<Set<String>>(setOf("assembleDebug", "clean", "testDebugUnitTest")) }
    var selectedGroup by remember { mutableStateOf<TaskGroup?>(null) }
    var customCommand by remember { mutableStateOf("") }
    var expandedGroups by remember { mutableStateOf(TaskGroup.values().toSet()) }
    var query by remember { mutableStateOf("") }

    val filteredTasks = remember(selectedGroup, query) {
        DEFAULT_TASKS.filter { task ->
            (selectedGroup == null || task.group == selectedGroup) &&
            (query.isEmpty() || task.name.contains(query, ignoreCase = true) ||
             task.description.contains(query, ignoreCase = true))
        }
    }

    val favoriteTasks = DEFAULT_TASKS.filter { it.name in favorites }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gradle Tasks", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.EDITOR) }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Search bar
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Filter tasks…", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                    focusedContainerColor = IDESurface, unfocusedContainerColor = IDESurface,
                    cursorColor = IDEPrimary
                )
            )

            // Custom command
            Surface(color = IDESurface) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customCommand, onValueChange = { customCommand = it },
                        placeholder = { Text("gradle custom task…", fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.weight(1f), singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                            focusedContainerColor = IDEBackground, unfocusedContainerColor = IDEBackground
                        )
                    )
                    Button(onClick = {
                        if (customCommand.isNotBlank()) {
                            val cmd = if (customCommand.startsWith("gradle ")) customCommand
                                      else "gradle $customCommand"
                            runTask(vm, project?.path, cmd)
                            customCommand = ""
                        }
                    }, enabled = customCommand.isNotBlank()) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Run")
                    }
                }
            }

            HorizontalDivider(color = IDEOutline)

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {

                // ── Favorites ────────────────────────────────────────────
                if (favoriteTasks.isNotEmpty() && query.isEmpty() && selectedGroup == null) {
                    item {
                        SectionHeader("⭐  Favorites", IDEPrimary, count = favoriteTasks.size,
                            expanded = true, onToggle = {})
                    }
                    items(favoriteTasks) { task ->
                        TaskRow(
                            task = task.copy(isFavorite = task.name in favorites),
                            projectPath = project?.path,
                            onRun = { runTask(vm, project?.path, "${task.command}") },
                            onToggleFavorite = {
                                favorites = if (task.name in favorites) favorites - task.name
                                else favorites + task.name
                            }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)); HorizontalDivider(color = IDEOutline) }
                }

                // ── Grouped tasks ─────────────────────────────────────
                val groups = filteredTasks.groupBy { it.group }.entries
                    .sortedBy { it.key.ordinal }

                groups.forEach { (group, tasks) ->
                    val isExp = group in expandedGroups
                    item(key = group.name) {
                        SectionHeader(
                            title   = group.label,
                            color   = group.color,
                            count   = tasks.size,
                            expanded = isExp,
                            icon    = group.icon,
                            onToggle = {
                                expandedGroups = if (isExp) expandedGroups - group else expandedGroups + group
                            }
                        )
                    }
                    if (isExp) {
                        items(tasks, key = { it.name }) { task ->
                            TaskRow(
                                task = task.copy(isFavorite = task.name in favorites),
                                projectPath = project?.path,
                                onRun = { runTask(vm, project?.path, task.command) },
                                onToggleFavorite = {
                                    favorites = if (task.name in favorites) favorites - task.name
                                    else favorites + task.name
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    color: Color,
    count: Int,
    expanded: Boolean,
    icon: ImageVector? = null,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IDESurface)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(it, null, Modifier.size(16.dp), tint = color)
            Spacer(Modifier.width(8.dp))
        }
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            color = IDEOnBackground, modifier = Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
            Text("$count", fontSize = 10.sp, color = color,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            null, Modifier.size(16.dp), tint = IDEOnSurface
        )
    }
}

@Composable
private fun TaskRow(
    task: GradleTask,
    projectPath: String?,
    onRun: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IDEBackground)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(task.name, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                fontFamily = FontFamily.Monospace, color = IDEOnBackground)
            Text(task.description, fontSize = 11.sp, color = IDEOnSurface)
        }
        // Favorite toggle
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
            Icon(
                if (task.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                null, Modifier.size(16.dp),
                tint = if (task.isFavorite) IDEPrimary else IDEOutline
            )
        }
        // Run button
        Surface(
            onClick = onRun,
            shape = RoundedCornerShape(8.dp),
            color = task.group.color.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, task.group.color.copy(alpha = 0.4f))
        ) {
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(14.dp), tint = task.group.color)
                Spacer(Modifier.width(4.dp))
                Text("Run", fontSize = 11.sp, color = task.group.color)
            }
        }
    }
    HorizontalDivider(color = IDEOutline.copy(alpha = 0.3f), modifier = Modifier.padding(start = 16.dp))
}

private fun runTask(vm: IDEViewModel, projectPath: String?, command: String) {
    val path = projectPath ?: return
    vm.navigate(Screen.TERMINAL)
    val fullCmd = if (command.startsWith("gradle "))
        "cd '$path' && $command --stacktrace 2>&1"
    else
        "cd '$path' && gradle $command --stacktrace 2>&1"
    vm.runCommand(fullCmd)
}
