package com.mobileide.feature.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Full-screen feature editor with:
 * - Tab bar
 * - MobileIDE theme integration (via [FeatureSoraEditor])
 * - Search/Replace panel
 * - Quick toolbar with special-character row
 * - Status bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureEditorScreen(
    onBack: () -> Unit,
    vm: FeatureEditorViewModel = viewModel(),
) {
    val tabs            by vm.tabs.collectAsState()
    val activeTabId     by vm.activeTabId.collectAsState()
    val settings        by vm.settings.collectAsState()
    val showSearch      by vm.showSearchPanel.collectAsState()
    val showLanguage    by vm.showLanguageDialog.collectAsState()
    val showGotoLine    by vm.showGotoLineDialog.collectAsState()
    val statusMessage   by vm.statusMessage.collectAsState()
    val isLoading       by vm.isLoading.collectAsState()
    val searchResults   by vm.searchResults.collectAsState()

    val activeTab = tabs.firstOrNull { it.id == activeTabId }

    val openFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.openFileFromUri(it) }
    }
    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/*")) { uri ->
        uri?.let { vm.saveActiveTab(it) }
    }

    Scaffold(
        topBar = {
            Column {
                // ── Main toolbar ────────────────────────────────────────────
                TopAppBar(
                    title = {
                        Text(
                            activeTab?.fileName ?: "Feature Editor",
                            style        = MaterialTheme.typography.titleMedium,
                            fontWeight   = FontWeight.SemiBold,
                            maxLines     = 1,
                            overflow     = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Zurück")
                        }
                    },
                    actions = {
                        if (activeTab?.isModified == true) {
                            IconButton(onClick = {
                                if (activeTab.filePath != null) vm.saveActiveTab()
                                else saveFileLauncher.launch(activeTab.fileName)
                            }) {
                                Icon(Icons.Default.Save, "Speichern",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        // File menu
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "Menü")
                            }
                            DropdownMenu(showMenu, { showMenu = false }) {
                                DropdownMenuItem({ Text("Neue Datei") }, {
                                    vm.createNewTab(); showMenu = false
                                }, leadingIcon = { Icon(Icons.Default.Add, null) })
                                DropdownMenuItem({ Text("Öffnen…") }, {
                                    openFileLauncher.launch(arrayOf("text/*", "*/*")); showMenu = false
                                }, leadingIcon = { Icon(Icons.Default.FolderOpen, null) })
                                DropdownMenuItem({ Text("Speichern unter…") }, {
                                    saveFileLauncher.launch(activeTab?.fileName ?: "untitled.txt"); showMenu = false
                                }, leadingIcon = { Icon(Icons.Default.SaveAs, null) })
                                HorizontalDivider()
                                DropdownMenuItem({ Text("Suchen / Ersetzen") }, {
                                    vm.showSearch(); showMenu = false
                                }, leadingIcon = { Icon(Icons.Default.Search, null) })
                                DropdownMenuItem({ Text("Gehe zu Zeile") }, {
                                    vm.showGotoLine(); showMenu = false
                                }, leadingIcon = { Icon(Icons.Default.MyLocation, null) })
                                DropdownMenuItem({ Text("Code formatieren") }, {
                                    vm.formatCode(); showMenu = false
                                }, leadingIcon = { Icon(Icons.Default.FormatAlignLeft, null) })
                                DropdownMenuItem({ Text("Sprache wählen") }, {
                                    vm.showLanguageSelector(); showMenu = false
                                }, leadingIcon = { Icon(Icons.Default.Code, null) })
                                HorizontalDivider()
                                DropdownMenuItem(
                                    { Text(if (settings.wordWrap) "Zeilenumbruch ✓" else "Zeilenumbruch") },
                                    { vm.toggleWordWrap(); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.WrapText, null) }
                                )
                                DropdownMenuItem(
                                    { Text(if (settings.showLineNumbers) "Zeilennummern ✓" else "Zeilennummern") },
                                    { vm.toggleLineNumbers(); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatListNumbered, null) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // ── Quick toolbar + special-chars row ───────────────────────
                Surface(
                    color         = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val canUndo = activeTabId?.let { vm.canUndo(it) } ?: false
                        val canRedo = activeTabId?.let { vm.canRedo(it) } ?: false
                        SmallToolbarBtn(Icons.Default.Undo, "Undo", canUndo) { activeTabId?.let { vm.undo(it) } }
                        SmallToolbarBtn(Icons.Default.Redo, "Redo", canRedo) { activeTabId?.let { vm.redo(it) } }
                        VerticalDivider(Modifier.height(20.dp).padding(horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant)
                        SmallToolbarBtn(Icons.Default.Search, "Suchen") { vm.showSearch() }
                        SmallToolbarBtn(Icons.Default.Save, "Speichern") {
                            if (activeTab?.filePath != null) vm.saveActiveTab()
                            else saveFileLauncher.launch(activeTab?.fileName ?: "untitled.txt")
                        }
                        VerticalDivider(Modifier.height(20.dp).padding(horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant)
                        // Special chars
                        listOf("{","}","(",")","[","]","<",">",";",":","=","+","-","*",
                            "/","\\","|","&","!","?","\"","'","`","#","@","$","%","^","~","_",".").forEach { ch ->
                            TextButton(
                                onClick = { /* inject char via editor */ },
                                modifier = Modifier.defaultMinSize(minWidth = 30.dp, minHeight = 30.dp),
                                contentPadding = PaddingValues(horizontal = 3.dp, vertical = 1.dp),
                            ) {
                                Text(ch, color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Status bar
            Surface(
                color         = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val (lines, words, chars) = vm.getWordCount()
                    if (statusMessage != null) {
                        Text(statusMessage!!, color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp, maxLines = 1)
                    } else {
                        Text("Z.${(activeTab?.cursorLine ?: 0)+1} S.${(activeTab?.cursorColumn ?: 0)+1}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (searchResults.isNotEmpty())
                            Text("${searchResults.size} Treffer",
                                color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp)
                        Text("$lines Z | $words W | $chars Z",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        activeTab?.let {
                            Text(it.language.displayName,
                                color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tab bar
            if (tabs.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tabs.forEach { tab ->
                            FeatureTabItem(
                                tab      = tab,
                                isActive = tab.id == activeTabId,
                                onSelect = { vm.setActiveTab(tab.id) },
                                onClose  = { vm.closeTab(tab.id) },
                            )
                        }
                        IconButton(onClick = { vm.createNewTab() }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Add, "Neuer Tab",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Search panel
            AnimatedVisibility(showSearch, enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()) {
                FeatureSearchPanel(vm)
            }

            // Editor
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (activeTab != null) {
                    FeatureSoraEditor(
                        tab              = activeTab,
                        settings         = settings,
                        onContentChanged = { vm.updateTabContent(activeTab.id, it) },
                        onCursorChanged  = { l, c -> vm.updateCursorPosition(activeTab.id, l, c) },
                        modifier         = Modifier.fillMaxSize(),
                    )
                } else {
                    // Empty state
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Code, null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Text("Feature Editor", style = MaterialTheme.typography.titleLarge)
                            Text("Keine Datei geöffnet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button({ vm.createNewTab() }) { Text("Neue Datei") }
                                OutlinedButton({ openFileLauncher.launch(arrayOf("text/*","*/*")) }) {
                                    Text("Öffnen")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialoge ────────────────────────────────────────────────────────────────
    if (showLanguage) {
        LanguageSelectorDialog(
            current    = activeTab?.language ?: FeatureEditorLanguage.PLAIN_TEXT,
            onSelected = { lang -> activeTabId?.let { vm.setLanguage(it, lang) }; vm.hideLanguageSelector() },
            onDismiss  = { vm.hideLanguageSelector() },
        )
    }

    if (showGotoLine) {
        GotoLineDialog(
            maxLine   = activeTab?.content?.lines()?.size ?: 1,
            onGoto    = { vm.hideGotoLine() },
            onDismiss = { vm.hideGotoLine() },
        )
    }
}

// ── Tab Item ──────────────────────────────────────────────────────────────────

@Composable
private fun FeatureTabItem(
    tab: FeatureEditorTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val bg   = if (isActive) MaterialTheme.colorScheme.surface else Color.Transparent
    val fg   = if (isActive) MaterialTheme.colorScheme.onSurface
               else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color    = bg,
        modifier = Modifier.clickable(onClick = onSelect).defaultMinSize(minWidth = 90.dp).widthIn(max = 150.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (tab.isModified) {
                Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary))
            }
            Text(tab.fileName, color = fg, fontSize = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose, modifier = Modifier.size(16.dp)) {
                Icon(Icons.Default.Close, "Schließen",
                    modifier = Modifier.size(11.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Search Panel ──────────────────────────────────────────────────────────────

@Composable
private fun FeatureSearchPanel(vm: FeatureEditorViewModel) {
    val opts    by vm.searchOptions.collectAsState()
    val results by vm.searchResults.collectAsState()
    val showRep by vm.showReplacePanel.collectAsState()

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 4.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = opts.query, onValueChange = { vm.updateSearchOptions(opts.copy(query = it)) },
                    placeholder = { Text("Suchen…") }, modifier = Modifier.weight(1f), singleLine = true,
                )
                FilterChip(opts.caseSensitive, { vm.updateSearchOptions(opts.copy(caseSensitive = !opts.caseSensitive)) },
                    { Text("Aa", fontSize = 11.sp) })
                FilterChip(opts.useRegex, { vm.updateSearchOptions(opts.copy(useRegex = !opts.useRegex)) },
                    { Text(".*", fontSize = 11.sp) })
                if (opts.query.isNotEmpty())
                    Text("${results.size}×", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                IconButton({ vm.toggleReplace() }) {
                    Icon(if (showRep) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Ersetzen")
                }
                IconButton({ vm.hideSearch() }) { Icon(Icons.Default.Close, "Schließen") }
            }
            AnimatedVisibility(showRep) {
                Row(modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = opts.replacement, onValueChange = { vm.updateSearchOptions(opts.copy(replacement = it)) },
                        placeholder = { Text("Ersetzen durch…") }, modifier = Modifier.weight(1f), singleLine = true,
                    )
                    Button({ vm.replaceNext() }) { Text("Nächstes", fontSize = 12.sp) }
                    Button({ vm.replaceAll() }, colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error)) { Text("Alle", fontSize = 12.sp) }
                }
            }
            if (results.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 100.dp).padding(top = 4.dp)) {
                    items(results.take(30)) { r ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Z.${r.line+1}:${r.column+1}",
                                color = MaterialTheme.colorScheme.primary, fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace)
                            Text(r.preview, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ── Language Selector ─────────────────────────────────────────────────────────

@Composable
private fun LanguageSelectorDialog(
    current: FeatureEditorLanguage,
    onSelected: (FeatureEditorLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = FeatureEditorLanguage.values().filter {
        it.displayName.contains(query, ignoreCase = true) ||
        it.fileExtensions.any { e -> e.contains(query, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sprache auswählen") },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, placeholder = { Text("Suchen…") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(filtered) { lang ->
                        ListItem(
                            headlineContent = { Text(lang.displayName) },
                            supportingContent = { Text(lang.fileExtensions.joinToString(", ") { ".$it" },
                                fontSize = 11.sp) },
                            trailingContent = { if (lang == current) Icon(Icons.Default.Check, null,
                                tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable { onSelected(lang) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Abbrechen") } },
    )
}

// ── Goto Line Dialog ──────────────────────────────────────────────────────────

@Composable
private fun GotoLineDialog(maxLine: Int, onGoto: (Int) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gehe zu Zeile") },
        text = {
            OutlinedTextField(input, { input = it; error = null },
                placeholder = { Text("1–$maxLine") }, singleLine = true, isError = error != null,
                supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } })
        },
        confirmButton = {
            Button({
                val l = input.toIntOrNull()
                when {
                    l == null       -> error = "Gültige Zahl eingeben"
                    l < 1 || l > maxLine -> error = "1–$maxLine"
                    else -> onGoto(l - 1)
                }
            }) { Text("Gehe zu") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Abbrechen") } },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SmallToolbarBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cd: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(onClick, modifier = Modifier.size(34.dp), enabled = enabled) {
        Icon(icon, cd, modifier = Modifier.size(17.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
    }
}
