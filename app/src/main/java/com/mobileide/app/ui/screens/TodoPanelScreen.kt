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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.data.TodoItem
import com.mobileide.app.data.TodoTag
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoPanelScreen(vm: IDEViewModel) {
    val todos by vm.todos.collectAsState()
    val project by vm.currentProject.collectAsState()
    var selectedTag by remember { mutableStateOf<TodoTag?>(null) }
    var query by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(SortBy.FILE) }

    LaunchedEffect(Unit) { vm.scanTodos() }

    val filtered = remember(todos, selectedTag, query, sortBy) {
        var list = todos
        if (selectedTag != null) list = list.filter { it.tag == selectedTag }
        if (query.isNotEmpty()) list = list.filter {
            it.text.contains(query, true) || it.file.name.contains(query, true)
        }
        when (sortBy) {
            SortBy.FILE -> list.sortedWith(compareBy({ it.file.name }, { it.line }))
            SortBy.TAG  -> list.sortedBy { it.tag.ordinal }
            SortBy.LINE -> list.sortedBy { it.line }
        }
    }

    val grouped = filtered.groupBy { it.file.absolutePath }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TODOs (${todos.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { vm.navigate(Screen.EDITOR) }) {
                    Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { vm.scanTodos() }) {
                        Icon(Icons.Default.Refresh, null, tint = IDEPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Filter bar
            Surface(color = IDESurface) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Search TODOs…", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                            focusedContainerColor = IDEBackground, unfocusedContainerColor = IDEBackground)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        FilterChip(selected = selectedTag == null, onClick = { selectedTag = null },
                            label = { Text("All (${todos.size})", fontSize = 11.sp) })
                        TodoTag.values().forEach { tag ->
                            val count = todos.count { it.tag == tag }
                            if (count > 0) {
                                FilterChip(selected = selectedTag == tag,
                                    onClick = { selectedTag = if (selectedTag == tag) null else tag },
                                    label = { Text("${tag.label} ($count)", fontSize = 11.sp) })
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sort:", color = IDEOnSurface, fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterVertically))
                        SortBy.values().forEach { sort ->
                            FilterChip(selected = sortBy == sort, onClick = { sortBy = sort },
                                label = { Text(sort.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) })
                        }
                    }
                }
            }

            HorizontalDivider(color = IDEOutline)

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(48.dp), tint = IDESecondary)
                        Spacer(Modifier.height(12.dp))
                        Text(if (todos.isEmpty()) "No TODOs found 🎉" else "No matches", color = IDEOnBackground, fontWeight = FontWeight.SemiBold)
                        if (todos.isEmpty()) Text("Your code is clean!", color = IDEOnSurface, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    grouped.forEach { (filePath, items) ->
                        val file = File(filePath)
                        val relPath = project?.let { filePath.removePrefix(it.path).trimStart('/') } ?: file.name

                        item(key = filePath + "_header") {
                            Surface(color = IDESurface, modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.InsertDriveFile, null, Modifier.size(14.dp), tint = IDEPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(relPath, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                        color = IDEPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Surface(shape = RoundedCornerShape(8.dp),
                                        color = IDEPrimary.copy(alpha = 0.15f)) {
                                        Text("${items.size}", fontSize = 10.sp, color = IDEPrimary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }

                        items(items, key = { "${filePath}_${it.line}_${it.tag}" }) { todo ->
                            TodoRow(todo) { vm.openFile(todo.file); vm.navigate(Screen.EDITOR) }
                        }
                        item(key = filePath + "_div") { HorizontalDivider(color = IDEOutline.copy(alpha = 0.5f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoRow(item: TodoItem, onClick: () -> Unit) {
    val (bg, fg) = todoColors(item.tag)
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        .background(IDEBackground).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top) {
        Surface(shape = RoundedCornerShape(4.dp), color = fg.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, fg.copy(alpha = 0.4f))) {
            Text(item.tag.label, fontSize = 9.sp, color = fg, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.text, fontSize = 13.sp, color = IDEOnBackground, maxLines = 2)
        }
        Text("L${item.line}", fontSize = 10.sp, color = IDEOutline, fontFamily = FontFamily.Monospace)
    }
}

private fun todoColors(tag: TodoTag): Pair<Color, Color> = when (tag) {
    TodoTag.TODO  -> Color.Transparent to Color(0xFF82AAFF)
    TodoTag.FIXME -> Color.Transparent to Color(0xFFFF9CAC)
    TodoTag.HACK  -> Color.Transparent to Color(0xFFFAB387)
    TodoTag.NOTE  -> Color.Transparent to Color(0xFFC3E88D)
    TodoTag.BUG   -> Color.Transparent to Color(0xFFF38BA8)
    TodoTag.WARN  -> Color.Transparent to Color(0xFFCBA6F7)
}

enum class SortBy { FILE, TAG, LINE }
