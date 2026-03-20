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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

data class Shortcut(val keys: String, val description: String, val category: String)

val SHORTCUTS = listOf(
    // Editor
    Shortcut("Ctrl+S",      "Save current file",                  "Editor"),
    Shortcut("Ctrl+Shift+S","Save all files",                     "Editor"),
    Shortcut("Ctrl+Z",      "Undo",                               "Editor"),
    Shortcut("Ctrl+Y",      "Redo",                               "Editor"),
    Shortcut("Ctrl+C",      "Copy",                               "Editor"),
    Shortcut("Ctrl+X",      "Cut",                                "Editor"),
    Shortcut("Ctrl+V",      "Paste",                              "Editor"),
    Shortcut("Ctrl+A",      "Select all",                         "Editor"),
    Shortcut("Ctrl+F",      "Find in file",                       "Editor"),
    Shortcut("Ctrl+H",      "Find and replace",                   "Editor"),
    Shortcut("Ctrl+D",      "Duplicate line",                     "Editor"),
    Shortcut("Ctrl+/",      "Toggle line comment",                "Editor"),
    Shortcut("Tab",         "Indent / Auto-complete confirm",     "Editor"),
    Shortcut("Shift+Tab",   "Unindent",                           "Editor"),
    Shortcut("Ctrl+]",      "Indent line",                        "Editor"),
    Shortcut("Ctrl+[",      "Unindent line",                      "Editor"),
    Shortcut("Ctrl+G",      "Go to line number",                  "Editor"),
    Shortcut("Ctrl+L",      "Select entire line",                 "Editor"),
    // Navigation
    Shortcut("Ctrl+P",      "Quick open file",                    "Navigation"),
    Shortcut("Ctrl+Shift+F","Project-wide search",                "Navigation"),
    Shortcut("Ctrl+T",      "Show file outline",                  "Navigation"),
    Shortcut("Ctrl+W",      "Close current tab",                  "Navigation"),
    Shortcut("Ctrl+Tab",    "Switch to next tab",                 "Navigation"),
    Shortcut("Ctrl+1..9",   "Jump to tab by number",              "Navigation"),
    Shortcut("Ctrl+B",      "Go to definition",                   "Navigation"),
    // Build
    Shortcut("Ctrl+F9",     "Build project",                      "Build"),
    Shortcut("Shift+F10",   "Run project",                        "Build"),
    Shortcut("Ctrl+F2",     "Stop build",                         "Build"),
    Shortcut("F8",          "Next build error",                   "Build"),
    // Smart Keyboard rows
    Shortcut("Row 1",       "Brackets: { } ( ) [ ] < >",         "Smart Keyboard"),
    Shortcut("Row 2",       "Operators: = != == -> ?. !! ?:",     "Smart Keyboard"),
    Shortcut("Row 3",       "Snippets: fun val var if for when",   "Smart Keyboard"),
    // Terminal
    Shortcut("↑ / ↓",       "Navigate command history",          "Terminal"),
    Shortcut("Ctrl+C",      "Cancel running command",             "Terminal"),
    Shortcut("Ctrl+L",      "Clear terminal output",              "Terminal"),
    Shortcut("Tab",         "Auto-complete path/command",         "Terminal"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardHelpScreen(vm: IDEViewModel) {
    val categories = remember { SHORTCUTS.map { it.category }.distinct() }
    var selectedCat by remember { mutableStateOf(categories.first()) }

    val filtered = remember(selectedCat) { SHORTCUTS.filter { it.category == selectedCat } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyboard Shortcuts", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { vm.navigate(Screen.SETTINGS) }) {
                    Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Category tabs
            ScrollableTabRow(selectedTabIndex = categories.indexOf(selectedCat),
                containerColor = IDESurface, contentColor = IDEPrimary,
                edgePadding = 12.dp) {
                categories.forEach { cat ->
                    Tab(selected = selectedCat == cat, onClick = { selectedCat = cat },
                        text = { Text(cat, fontSize = 12.sp) })
                }
            }

            HorizontalDivider(color = IDEOutline)

            LazyColumn(modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { shortcut ->
                    ShortcutRow(shortcut)
                }
            }
        }
    }
}

@Composable
private fun ShortcutRow(shortcut: Shortcut) {
    Row(modifier = Modifier.fillMaxWidth()
        .background(IDESurface, RoundedCornerShape(8.dp))
        .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(6.dp),
            color = IDESurfaceVariant,
            border = BorderStroke(1.dp, IDEOutline)) {
            Text(shortcut.keys,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, color = IDEPrimary)
        }
        Spacer(Modifier.width(16.dp))
        Text(shortcut.description, color = IDEOnBackground, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}
