package com.mobileide.app.ui.screens

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class DiffLine {
    data class Added    (val line: String, val newNum: Int) : DiffLine()
    data class Removed  (val line: String, val oldNum: Int) : DiffLine()
    data class Context  (val line: String, val oldNum: Int, val newNum: Int) : DiffLine()
    data class Hunk     (val header: String) : DiffLine()
    data class FileHeader(val text: String) : DiffLine()
}

data class DiffFile(
    val oldPath: String, val newPath: String,
    val lines: List<DiffLine>,
    val additions: Int, val deletions: Int
)

object DiffParser {
    fun parse(raw: String): List<DiffFile> {
        val files = mutableListOf<DiffFile>()
        var curOld = ""; var curNew = ""; var curLines = mutableListOf<DiffLine>()
        var adds = 0; var dels = 0; var oldN = 0; var newN = 0

        raw.lines().forEach { line ->
            when {
                line.startsWith("diff ") -> {
                    if (curLines.isNotEmpty()) {
                        files += DiffFile(curOld, curNew, curLines.toList(), adds, dels)
                        curLines = mutableListOf(); adds = 0; dels = 0
                    }
                    curLines.add(DiffLine.FileHeader(line))
                }
                line.startsWith("--- ") -> curOld = line.removePrefix("--- ").trimStart('a','/')
                line.startsWith("+++ ") -> curNew = line.removePrefix("+++ ").trimStart('b','/')
                line.startsWith("@@ ") -> {
                    val m = Regex("""@@ -(\d+)(?:,\d+)? \+(\d+)""").find(line)
                    oldN = m?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    newN = m?.groupValues?.get(2)?.toIntOrNull() ?: 0
                    curLines.add(DiffLine.Hunk(line))
                }
                line.startsWith("+") && !line.startsWith("+++") -> {
                    curLines.add(DiffLine.Added(line.drop(1), newN++)); adds++
                }
                line.startsWith("-") && !line.startsWith("---") -> {
                    curLines.add(DiffLine.Removed(line.drop(1), oldN++)); dels++
                }
                line.startsWith("Binary") -> curLines.add(DiffLine.Context("[Binary file]", 0, 0))
                line.startsWith("\\") -> {}
                else -> if (oldN > 0 || newN > 0) curLines.add(DiffLine.Context(line, oldN++, newN++))
            }
        }
        if (curLines.isNotEmpty()) files += DiffFile(curOld, curNew, curLines.toList(), adds, dels)
        return files
    }
}

enum class DiffMode(val label: String) {
    UNSTAGED("Working Tree"), STAGED("Staged"), HEAD("vs HEAD"), LAST("Last Commit")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffViewerScreen(vm: IDEViewModel) {
    val project by vm.currentProject.collectAsState()
    var diffFiles by remember { mutableStateOf<List<DiffFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var diffMode  by remember { mutableStateOf(DiffMode.UNSTAGED) }
    var expandedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    fun loadDiff() {
        val path = project?.path ?: return
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val args = when (diffMode) {
                DiffMode.STAGED   -> listOf("git", "-C", path, "diff", "--cached")
                DiffMode.UNSTAGED -> listOf("git", "-C", path, "diff")
                DiffMode.HEAD     -> listOf("git", "-C", path, "diff", "HEAD~1", "HEAD")
                DiffMode.LAST     -> listOf("git", "-C", path, "show", "--format=", "HEAD")
            }
            val result = try {
                ProcessBuilder(args).redirectErrorStream(true)
                    .start().inputStream.bufferedReader().readText()
            } catch (e: Exception) { "Error: ${e.message}" }
            val parsed = DiffParser.parse(result)
            diffFiles = parsed
            expandedFiles = parsed.map { it.newPath }.toSet()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadDiff() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diff Viewer", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { vm.navigate(Screen.GIT) }) {
                    Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { loadDiff() }) {
                    Icon(Icons.Default.Refresh, null, tint = IDEPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Surface(color = IDESurface) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DiffMode.values().forEach { mode ->
                        FilterChip(selected = diffMode == mode,
                            onClick = { diffMode = mode; loadDiff() },
                            label = { Text(mode.label, fontSize = 11.sp) })
                    }
                }
            }
            if (diffFiles.isNotEmpty()) {
                val totalAdds = diffFiles.sumOf { it.additions }
                val totalDels = diffFiles.sumOf { it.deletions }
                Surface(color = IDESurfaceVariant) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${diffFiles.size} files", style = MaterialTheme.typography.labelSmall, color = IDEOnBackground)
                        Text("+$totalAdds", style = MaterialTheme.typography.labelSmall, color = IDESecondary)
                        Text("-$totalDels", style = MaterialTheme.typography.labelSmall, color = IDETertiary)
                    }
                }
            }
            HorizontalDivider(color = IDEOutline)
            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = IDEPrimary) }
            } else if (diffFiles.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(48.dp), tint = IDESecondary)
                        Spacer(Modifier.height(12.dp))
                        Text("No changes", fontWeight = FontWeight.SemiBold)
                        Text("Working tree is clean", color = IDEOnSurface, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    diffFiles.forEach { file ->
                        val isExp = file.newPath in expandedFiles
                        item(key = file.newPath + "_h") {
                            DiffFileHeader(file, isExp) {
                                expandedFiles = if (isExp) expandedFiles - file.newPath
                                else expandedFiles + file.newPath
                            }
                        }
                        if (isExp) {
                            items(file.lines, key = { "${file.newPath}_${it.hashCode()}_${ System.identityHashCode(it)}" }) { line ->
                                DiffLineRow(line)
                            }
                        }
                        item(key = file.newPath + "_d") { HorizontalDivider(color = IDEOutline) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffFileHeader(file: DiffFile, isExpanded: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(IDESurface)
        .clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
            null, Modifier.size(16.dp), tint = IDEPrimary)
        Spacer(Modifier.width(8.dp))
        Text(file.newPath.ifEmpty { file.oldPath }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            fontFamily = FontFamily.Monospace, color = IDEOnBackground, modifier = Modifier.weight(1f))
        if (file.additions > 0) {
            Surface(shape = RoundedCornerShape(4.dp), color = IDESecondary.copy(alpha = 0.15f)) {
                Text("+${file.additions}", fontSize = 10.sp, color = IDESecondary,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.width(4.dp))
        }
        if (file.deletions > 0) {
            Surface(shape = RoundedCornerShape(4.dp), color = IDETertiary.copy(alpha = 0.15f)) {
                Text("-${file.deletions}", fontSize = 10.sp, color = IDETertiary,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun DiffLineRow(line: DiffLine) {
    val bg: Color
    val fg: Color
    val prefix: String
    val lineNum: String
    val content: String
    when (line) {
        is DiffLine.Added     -> { bg = IDESecondary.copy(0.08f); fg = IDESecondary; prefix = "+"; lineNum = line.newNum.toString(); content = line.line }
        is DiffLine.Removed   -> { bg = IDETertiary.copy(0.08f);  fg = IDETertiary;  prefix = "-"; lineNum = line.oldNum.toString(); content = line.line }
        is DiffLine.Hunk      -> { bg = IDEPrimary.copy(0.08f);   fg = IDEPrimary;   prefix = " "; lineNum = "";                    content = line.header }
        is DiffLine.FileHeader -> { bg = IDESurface;              fg = IDEOnSurface; prefix = " "; lineNum = "";                    content = line.text }
        is DiffLine.Context   -> { bg = IDEBackground;            fg = IDEOnSurface.copy(0.7f); prefix = " "; lineNum = line.oldNum.toString(); content = line.line }
    }
    Row(modifier = Modifier.fillMaxWidth().background(bg).padding(0.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(lineNum, modifier = Modifier.width(36.dp).padding(end = 4.dp),
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = IDEOutline), maxLines = 1)
        Text(prefix, modifier = Modifier.width(14.dp),
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg))
        Text(content,
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(end = 8.dp),
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp, color = fg))
    }
}
