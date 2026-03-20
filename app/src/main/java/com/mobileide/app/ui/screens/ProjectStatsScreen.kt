package com.mobileide.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.mobileide.app.utils.CodeFormatter
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ProjectStats(
    val totalFiles: Int = 0,
    val kotlinFiles: Int = 0,
    val javaFiles: Int = 0,
    val xmlFiles: Int = 0,
    val otherFiles: Int = 0,
    val totalLines: Int = 0,
    val codeLines: Int = 0,
    val commentLines: Int = 0,
    val blankLines: Int = 0,
    val totalFunctions: Int = 0,
    val totalClasses: Int = 0,
    val totalSizeBytes: Long = 0L,
    val largestFile: String = "",
    val largestFileLines: Int = 0,
    val mostRecentFile: String = "",
    val lastModified: Long = 0L,
    val topFiles: List<Pair<String, Int>> = emptyList()  // file name -> line count
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectStatsScreen(vm: IDEViewModel) {
    val project by vm.currentProject.collectAsState()
    var stats by remember { mutableStateOf<ProjectStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(project?.path) {
        if (project == null) return@LaunchedEffect
        isLoading = true
        scope.launch(Dispatchers.IO) {
            stats = analyzeProject(File(project!!.path))
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project Statistics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.EDITOR) }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isLoading = true
                        scope.launch(Dispatchers.IO) {
                            stats = project?.let { analyzeProject(File(it.path)) }
                            isLoading = false
                        }
                    }) { Icon(Icons.Default.Refresh, null, tint = IDEPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = IDEPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text("Analyzing project…", color = IDEOnSurface)
                }
            }
        } else {
            val s = stats
            if (s == null) {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text("No project open", color = IDEOnSurface)
                }
            } else {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Summary Cards ──────────────────────────────────────
                    Text("OVERVIEW", style = MaterialTheme.typography.labelSmall, color = IDEPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(Icons.Default.Folder, s.totalFiles.toString(), "Files",
                            IDEPrimary, Modifier.weight(1f))
                        StatCard(Icons.Default.Code, formatNum(s.totalLines), "Lines",
                            IDESecondary, Modifier.weight(1f))
                        StatCard(Icons.Default.Storage, formatBytes(s.totalSizeBytes), "Size",
                            SyntaxAnnotation, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(Icons.Default.Functions, s.totalFunctions.toString(), "Functions",
                            SyntaxFunction, Modifier.weight(1f))
                        StatCard(Icons.Default.Category, s.totalClasses.toString(), "Classes",
                            SyntaxType, Modifier.weight(1f))
                        StatCard(Icons.Default.ChatBubbleOutline, s.commentLines.toString(), "Comments",
                            SyntaxComment, Modifier.weight(1f))
                    }

                    // ── File Breakdown ─────────────────────────────────────
                    Text("FILE BREAKDOWN", style = MaterialTheme.typography.labelSmall, color = IDEPrimary)
                    Card(colors = CardDefaults.cardColors(containerColor = IDESurface),
                        border = BorderStroke(1.dp, IDEOutline)) {
                        Column(modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LangBar("Kotlin", s.kotlinFiles, s.totalFiles, Color(0xFFCBA6F7))
                            LangBar("Java",   s.javaFiles,   s.totalFiles, Color(0xFFFAB387))
                            LangBar("XML",    s.xmlFiles,    s.totalFiles, Color(0xFF89DCEB))
                            if (s.otherFiles > 0)
                                LangBar("Other", s.otherFiles, s.totalFiles, IDEOutline)
                        }
                    }

                    // ── Code Quality ───────────────────────────────────────
                    Text("CODE QUALITY", style = MaterialTheme.typography.labelSmall, color = IDEPrimary)
                    Card(colors = CardDefaults.cardColors(containerColor = IDESurface),
                        border = BorderStroke(1.dp, IDEOutline)) {
                        Column(modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val codeRatio = if (s.totalLines > 0)
                                (s.codeLines.toFloat() / s.totalLines * 100).toInt() else 0
                            val commentRatio = if (s.totalLines > 0)
                                (s.commentLines.toFloat() / s.totalLines * 100).toInt() else 0

                            QualityRow("Code lines",      "${s.codeLines} (${codeRatio}%)",    IDESecondary)
                            QualityRow("Comment lines",   "${s.commentLines} (${commentRatio}%)", SyntaxComment)
                            QualityRow("Blank lines",     "${s.blankLines}",                   IDEOutline)
                            QualityRow("Largest file",    "${s.largestFile} (${s.largestFileLines} lines)", IDEPrimary)
                            if (s.lastModified > 0) {
                                val df = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                QualityRow("Last modified", df.format(Date(s.lastModified)),   IDEOnSurface)
                            }
                        }
                    }

                    // ── Top Files by Size ──────────────────────────────────
                    if (s.topFiles.isNotEmpty()) {
                        Text("LARGEST FILES", style = MaterialTheme.typography.labelSmall, color = IDEPrimary)
                        Card(colors = CardDefaults.cardColors(containerColor = IDESurface),
                            border = BorderStroke(1.dp, IDEOutline)) {
                            Column(modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                s.topFiles.take(8).forEachIndexed { i, (name, lines) ->
                                    Row(modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("${i + 1}.", color = IDEOutline, fontSize = 11.sp,
                                            modifier = Modifier.width(20.dp))
                                        Text(name, modifier = Modifier.weight(1f), fontSize = 12.sp,
                                            color = IDEOnBackground, fontFamily = FontFamily.Monospace,
                                            maxLines = 1)
                                        Text("$lines ln", fontSize = 11.sp, color = IDEOnSurface,
                                            fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = IDESurface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(20.dp), tint = color)
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = IDEOnBackground)
            Text(label, fontSize = 10.sp, color = IDEOnSurface)
        }
    }
}

@Composable
private fun LangBar(name: String, count: Int, total: Int, color: Color) {
    val ratio = if (total > 0) count.toFloat() / total else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.width(50.dp), fontSize = 12.sp, color = IDEOnBackground)
        Box(modifier = Modifier.weight(1f).height(14.dp)
            .background(IDESurfaceVariant, RoundedCornerShape(4.dp))) {
            Box(modifier = Modifier.fillMaxHeight()
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .background(color, RoundedCornerShape(4.dp)))
        }
        Spacer(Modifier.width(8.dp))
        Text("$count", fontSize = 11.sp, color = IDEOnSurface,
            modifier = Modifier.width(30.dp), fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun QualityRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = IDEOnSurface, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace)
    }
}

private fun analyzeProject(root: File): ProjectStats {
    var ktFiles = 0; var javaFiles = 0; var xmlFiles = 0; var other = 0
    var totalLines = 0; var codeLines = 0; var commentLines = 0; var blankLines = 0
    var totalFunctions = 0; var totalClasses = 0; var totalSize = 0L
    var largestName = ""; var largestLines = 0
    var lastMod = 0L; var lastModFile = ""
    val fileSizes = mutableListOf<Pair<String, Int>>()

    root.walkTopDown()
        .filter { it.isFile && !it.path.contains("/build/") && !it.path.contains("/.gradle/") }
        .forEach { file ->
            totalSize += file.length()
            if (file.lastModified() > lastMod) { lastMod = file.lastModified(); lastModFile = file.name }

            val ext = file.extension.lowercase()
            when (ext) {
                "kt", "kts" -> ktFiles++
                "java"      -> javaFiles++
                "xml"       -> xmlFiles++
                else        -> { other++; return@forEach }
            }

            try {
                val code = file.readText()
                val lang = when (ext) {
                    "kt", "kts" -> com.mobileide.app.data.Language.KOTLIN
                    "java"      -> com.mobileide.app.data.Language.JAVA
                    else        -> com.mobileide.app.data.Language.PLAIN
                }
                val s = CodeFormatter.analyze(code, lang)
                totalLines     += s.totalLines
                codeLines      += s.codeLines
                commentLines   += s.commentLines
                blankLines     += s.blankLines
                totalFunctions += s.functions
                totalClasses   += s.classes
                if (s.totalLines > largestLines) {
                    largestLines = s.totalLines
                    largestName = file.name
                }
                fileSizes += Pair(file.name, s.totalLines)
            } catch (_: Exception) {}
        }

    return ProjectStats(
        totalFiles      = ktFiles + javaFiles + xmlFiles + other,
        kotlinFiles     = ktFiles,
        javaFiles       = javaFiles,
        xmlFiles        = xmlFiles,
        otherFiles      = other,
        totalLines      = totalLines,
        codeLines       = codeLines,
        commentLines    = commentLines,
        blankLines      = blankLines,
        totalFunctions  = totalFunctions,
        totalClasses    = totalClasses,
        totalSizeBytes  = totalSize,
        largestFile     = largestName,
        largestFileLines = largestLines,
        mostRecentFile  = lastModFile,
        lastModified    = lastMod,
        topFiles        = fileSizes.sortedByDescending { it.second }
    )
}

private fun formatNum(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000f)
    n >= 1_000     -> "%.1fK".format(n / 1_000f)
    else           -> n.toString()
}

private fun formatBytes(b: Long): String = when {
    b >= 1_048_576 -> "%.1f MB".format(b / 1_048_576f)
    b >= 1_024     -> "%.1f KB".format(b / 1_024f)
    else           -> "$b B"
}
