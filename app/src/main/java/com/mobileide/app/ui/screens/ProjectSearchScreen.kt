package com.mobileide.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SearchResult(
    val file: File,
    val lineNumber: Int,
    val lineContent: String,
    val matchStart: Int,
    val matchEnd: Int
)

data class FileSearchGroup(
    val file: File,
    val results: List<SearchResult>,
    val isExpanded: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSearchScreen(vm: IDEViewModel) {
    val project by vm.currentProject.collectAsState()
    var query by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var wholeWord by remember { mutableStateOf(false) }
    var useRegex by remember { mutableStateOf(false) }
    var includeExts by remember { mutableStateOf("kt,java,xml,kts,json,md") }
    var groups by remember { mutableStateOf<List<FileSearchGroup>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var totalMatches by remember { mutableStateOf(0) }
    var showOptions by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun runSearch() {
        val root = project?.path ?: return
        if (query.isBlank()) return
        isSearching = true
        groups = emptyList()

        scope.launch(Dispatchers.IO) {
            val exts = includeExts.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            val regexOptions = if (!caseSensitive) setOf(RegexOption.IGNORE_CASE) else emptySet()
            val pattern = try {
                if (useRegex) Regex(query, regexOptions)
                else {
                    val escaped = Regex.escape(query)
                    val wrapped = if (wholeWord) "\\b$escaped\\b" else escaped
                    Regex(wrapped, regexOptions)
                }
            } catch (e: Exception) { null }

            if (pattern == null) {
                withContext(Dispatchers.Main) { isSearching = false }
                return@launch
            }

            val newGroups = mutableListOf<FileSearchGroup>()
            File(root).walkTopDown()
                .filter { it.isFile && (exts.isEmpty() || it.extension.lowercase() in exts) }
                .filter { !it.path.contains("/build/") && !it.path.contains("/.gradle/") }
                .sortedBy { it.name }
                .forEach { file ->
                    val matches = mutableListOf<SearchResult>()
                    try {
                        file.readLines().forEachIndexed { idx, line ->
                            pattern.findAll(line).forEach { match ->
                                matches += SearchResult(
                                    file        = file,
                                    lineNumber   = idx + 1,
                                    lineContent  = line.trim(),
                                    matchStart   = match.range.first,
                                    matchEnd     = match.range.last + 1
                                )
                            }
                        }
                    } catch (_: Exception) {}
                    if (matches.isNotEmpty()) newGroups += FileSearchGroup(file, matches)
                }

            val total = newGroups.sumOf { it.results.size }
            withContext(Dispatchers.Main) {
                groups = newGroups
                totalMatches = total
                isSearching = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project Search", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.EDITOR) }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showOptions = !showOptions }) {
                        Icon(Icons.Default.Tune, null,
                            tint = if (showOptions) IDEPrimary else IDEOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Search Input ───────────────────────────────────────────────
            Surface(color = IDESurface) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search in project…", fontSize = 13.sp) },
                            leadingIcon = {
                                if (isSearching)
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else
                                    Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = ""; groups = emptyList(); totalMatches = 0 }) {
                                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp,
                                color = IDEOnBackground),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                                focusedContainerColor = IDEBackground, unfocusedContainerColor = IDEBackground,
                                cursorColor = IDEPrimary
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { runSearch() })
                        )
                        Button(onClick = { runSearch() }, enabled = query.isNotBlank() && !isSearching) {
                            Icon(Icons.Default.Search, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Find")
                        }
                    }

                    // Toggle options
                    AnimatedVisibility(visible = showOptions) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                SearchToggle("Aa", "Case sensitive", caseSensitive) { caseSensitive = it }
                                SearchToggle("\\b", "Whole word", wholeWord) { wholeWord = it }
                                SearchToggle(".*", "Regex", useRegex) { useRegex = it }
                            }
                            OutlinedTextField(
                                value = includeExts,
                                onValueChange = { includeExts = it },
                                label = { Text("File extensions (comma-separated)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                                    focusedContainerColor = IDEBackground, unfocusedContainerColor = IDEBackground
                                )
                            )
                        }
                    }
                }
            }

            // ── Results Stats ──────────────────────────────────────────────
            if (totalMatches > 0) {
                Surface(color = IDESurfaceVariant) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = IDESecondary)
                        Spacer(Modifier.width(6.dp))
                        Text("$totalMatches match${if (totalMatches != 1) "es" else ""} in ${groups.size} file${if (groups.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium, color = IDEOnBackground)
                    }
                }
            }

            HorizontalDivider(color = IDEOutline)

            // ── Results List ───────────────────────────────────────────────
            if (groups.isEmpty() && !isSearching && query.isNotEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, Modifier.size(48.dp), tint = IDEOutline)
                        Spacer(Modifier.height(8.dp))
                        Text("No results for \"$query\"", color = IDEOnSurface)
                    }
                }
            } else {
                var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    groups.forEach { group ->
                        val isExp = group.file.absolutePath !in expandedGroups // default expanded

                        item(key = group.file.absolutePath) {
                            // File header
                            Surface(
                                color = IDESurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedGroups = if (isExp)
                                            expandedGroups + group.file.absolutePath
                                        else
                                            expandedGroups - group.file.absolutePath
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isExp) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                        null, Modifier.size(16.dp), tint = IDEPrimary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    val relPath = group.file.absolutePath
                                        .removePrefix(project?.path ?: "")
                                        .trimStart('/')
                                    Text(relPath, color = IDEPrimary,
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.weight(1f), maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                    Surface(shape = RoundedCornerShape(8.dp),
                                        color = IDEPrimary.copy(alpha = 0.15f)) {
                                        Text("${group.results.size}", fontSize = 10.sp, color = IDEPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }

                        if (isExp) {
                            items(group.results, key = { "${it.file.absolutePath}:${it.lineNumber}:${it.matchStart}" }) { result ->
                                SearchResultRow(
                                    result = result,
                                    query = query,
                                    caseSensitive = caseSensitive,
                                    onClick = {
                                        vm.openFile(result.file)
                                        vm.navigate(Screen.EDITOR)
                                    }
                                )
                            }
                        }

                        item { HorizontalDivider(color = IDEOutline.copy(alpha = 0.5f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: SearchResult,
    query: String,
    caseSensitive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(IDEBackground)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            result.lineNumber.toString(),
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = IDEOutline),
            modifier = Modifier.width(32.dp)
        )
        Spacer(Modifier.width(8.dp))

        // Highlight match in line
        val annotated = buildAnnotatedString {
            val line = result.lineContent
            val flags = if (!caseSensitive) setOf(RegexOption.IGNORE_CASE) else emptySet()
            val pattern = try { Regex(Regex.escape(query), flags) } catch (_: Exception) { null }

            if (pattern != null) {
                var last = 0
                pattern.findAll(line).forEach { match ->
                    append(line.substring(last, match.range.first))
                    withStyle(SpanStyle(
                        color = IDEBackground,
                        background = IDESecondary,
                        fontWeight = FontWeight.Bold
                    )) { append(match.value) }
                    last = match.range.last + 1
                }
                append(line.substring(last))
            } else {
                append(line)
            }
        }

        Text(
            annotated,
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                lineHeight = 18.sp, color = IDEOnBackground),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SearchToggle(symbol: String, tooltip: String, active: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        onClick = { onToggle(!active) },
        shape = RoundedCornerShape(6.dp),
        color = if (active) IDEPrimary.copy(alpha = 0.15f) else IDESurfaceVariant,
        border = BorderStroke(1.dp, if (active) IDEPrimary else IDEOutline),
        modifier = Modifier.size(36.dp, 32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, color = if (active) IDEPrimary else IDEOnSurface))
        }
    }
}
