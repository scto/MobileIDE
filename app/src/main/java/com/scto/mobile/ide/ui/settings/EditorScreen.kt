package com.scto.mobile.ide.ui.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.scto.mobile.ide.R
import com.scto.mobile.ide.ui.editor.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(navController: NavController, editorViewModel: EditorViewModel? = null) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("MobileIDE_Editor_Settings", Context.MODE_PRIVATE) }

    // Sprachserver
    var insertFinalNewline by remember { mutableStateOf(prefs.getBoolean("editor_insert_final_newline", true)) }
    var trimTrailingWhitespace by remember { mutableStateOf(prefs.getBoolean("editor_trim_trailing_whitespace", true)) }

    // Formatierung
    var formatOnSave by remember { mutableStateOf(prefs.getBoolean("editor_format_on_save", false)) }

    // Intelligente Funktionen
    var autoCloseTags by remember { mutableStateOf(prefs.getBoolean("editor_auto_close_tags", true)) }
    var continueListPrefix by remember { mutableStateOf(prefs.getBoolean("editor_continue_list_prefix", true)) }

    // Inhalt
    var wordWrap by remember { mutableStateOf(prefs.getBoolean("editor_word_wrap", false)) }
    var wrapTxtFiles by remember { mutableStateOf(prefs.getBoolean("editor_wrap_txt_files", true)) }
    var readOnlyMode by remember { mutableStateOf(prefs.getBoolean("editor_read_only_mode", false)) }

    // Editor
    var disableSoftKeyboard by remember { mutableStateOf(prefs.getBoolean("editor_disable_soft_keyboard", true)) }
    var lineSpacingMultiplier by remember { mutableFloatStateOf(prefs.getFloat("editor_line_spacing_multiplier", 1.0f)) }
    var cursorAnimation by remember { mutableStateOf(prefs.getBoolean("editor_cursor_animation", true)) }
    var showMinimap by remember { mutableStateOf(prefs.getBoolean("editor_show_minimap", true)) }
    var showLineNumbers by remember { mutableStateOf(prefs.getBoolean("editor_show_line_numbers", true)) }

    // Erweiterte Einstellungen
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("editor_font_size", 14f)) }
    var tabWidth by remember { mutableIntStateOf(prefs.getInt("editor_tab_width", 4)) }
    var showInvisibles by remember { mutableStateOf(prefs.getBoolean("editor_show_invisibles", false)) }
    var codeFolding by remember { mutableStateOf(prefs.getBoolean("editor_code_folding", true)) }
    var aiEnabled by remember { mutableStateOf(prefs.getBoolean("editor_ai_enabled", true)) }
    var editorType by remember { mutableStateOf(prefs.getString("editor_type", "treesitter") ?: "treesitter") }

    var showFormattersDialog by remember { mutableStateOf(false) }

    LaunchedEffect(
        insertFinalNewline,
        trimTrailingWhitespace,
        formatOnSave,
        autoCloseTags,
        continueListPrefix,
        wordWrap,
        wrapTxtFiles,
        readOnlyMode,
        disableSoftKeyboard,
        lineSpacingMultiplier,
        cursorAnimation,
        showMinimap,
        showLineNumbers,
        fontSize,
        tabWidth,
        showInvisibles,
        codeFolding,
        aiEnabled,
        editorType,
    ) {
        prefs.edit {
            putBoolean("editor_insert_final_newline", insertFinalNewline)
            putBoolean("editor_trim_trailing_whitespace", trimTrailingWhitespace)
            putBoolean("editor_format_on_save", formatOnSave)
            putBoolean("editor_auto_close_tags", autoCloseTags)
            putBoolean("editor_continue_list_prefix", continueListPrefix)
            putBoolean("editor_word_wrap", wordWrap)
            putBoolean("editor_wrap_txt_files", wrapTxtFiles)
            putBoolean("editor_read_only_mode", readOnlyMode)
            putBoolean("editor_disable_soft_keyboard", disableSoftKeyboard)
            putFloat("editor_line_spacing_multiplier", lineSpacingMultiplier)
            putBoolean("editor_cursor_animation", cursorAnimation)
            putBoolean("editor_show_minimap", showMinimap)
            putBoolean("editor_show_line_numbers", showLineNumbers)
            putFloat("editor_font_size", fontSize)
            putInt("editor_tab_width", tabWidth)
            putBoolean("editor_show_invisibles", showInvisibles)
            putBoolean("editor_code_folding", codeFolding)
            putBoolean("editor_ai_enabled", aiEnabled)
            putString("editor_type", editorType)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Sprachserver
            EditorSectionHeader(title = "Sprachserver")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    NavigationSettingRow(
                        title = "Sprachserver verwalten",
                        description = "Sprachserver installieren, verbinden und konfigurieren",
                        onClick = { navController.navigate("settings/lsp") },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingRow(
                        title = "Finalen Zeilenumbruch einfügen",
                        description = "Stelle sicher, dass die Datei beim Formatieren mit einem Zeilenumbruch endet",
                        checked = insertFinalNewline,
                        onCheckedChange = { insertFinalNewline = it },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingRow(
                        title = "Nachfolgende Leerzeichen entfernen",
                        description = "Bei der Formatierung nachfolgende Leerzeichen vor Zeilenumbrüchen entfernen",
                        checked = trimTrailingWhitespace,
                        onCheckedChange = { trimTrailingWhitespace = it },
                    )
                }
            }

            // 2. Formatierung
            EditorSectionHeader(title = "Formatierung")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    NavigationSettingRow(
                        title = "Formatierer verwalten",
                        description = "Formatierer priorisieren und konfigurieren",
                        onClick = { showFormattersDialog = true },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingRow(
                        title = "Beim Speichern formatieren",
                        description = "Die Datei beim Speichern automatisch formatieren",
                        checked = formatOnSave,
                        onCheckedChange = { formatOnSave = it },
                    )
                }
            }

            // 3. Intelligente Funktionen
            EditorSectionHeader(title = "Intelligente Funktionen")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    SwitchSettingRow(
                        title = "Tags automatisch schließen",
                        description = "HTML-Tags automatisch schließen",
                        checked = autoCloseTags,
                        onCheckedChange = { autoCloseTags = it },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingRow(
                        title = "Aufzählung fortsetzen",
                        description = "Listen und Zitate in Markdown automatisch fortsetzen",
                        checked = continueListPrefix,
                        onCheckedChange = { continueListPrefix = it },
                    )
                }
            }

            // 4. Inhalt
            EditorSectionHeader(title = "Inhalt")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    SwitchSettingRow(
                        title = "Zeilenumbruch",
                        description = "Zeilenumbruch in allen Editoren aktivieren",
                        checked = wordWrap,
                        onCheckedChange = { wordWrap = it },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingRow(
                        title = "TXT-Dateien umbrechen",
                        description = "TXT-Dateien automatisch umbrechen",
                        checked = wrapTxtFiles,
                        onCheckedChange = { wrapTxtFiles = it },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingRow(
                        title = "Lesemodus",
                        description = "Lesemodus standardmäßig aktivieren",
                        checked = readOnlyMode,
                        onCheckedChange = { readOnlyMode = it },
                    )
                }
            }

            // 5. Editor
            EditorSectionHeader(title = "Editor")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    SwitchSettingRow(
                        title = "Virtuelle Tastatur deaktivieren",
                        description = "Virtuelle Tastatur deaktivieren, wenn eine Hardware-Tastatur verfügbar ist",
                        checked = disableSoftKeyboard,
                        onCheckedChange = { disableSoftKeyboard = it },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    NavigationSettingRow(
                        title = "Zeilenabstand",
                        description = "Höhenmultiplikator für jede Zeile im Editor (aktuell: %.1fx)".format(lineSpacingMultiplier),
                        onClick = {
                            lineSpacingMultiplier = if (lineSpacingMultiplier >= 1.5f) 1.0f else lineSpacingMultiplier + 0.1f
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingRow(
                        title = "Cursor-Animation",
                        description = "Flüssige Cursor-Animationen aktivieren",
                        checked = cursorAnimation,
                        onCheckedChange = { cursorAnimation = it },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingRow(
                        title = "Minimap anzeigen",
                        description = "Eine Minimap auf der rechten Seite des Bildschirms anzeigen",
                        checked = showMinimap,
                        onCheckedChange = { showMinimap = it },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingRow(
                        title = "Zeilennummern anzeigen",
                        description = "Zeilennummern im Editor anzeigen",
                        checked = showLineNumbers,
                        onCheckedChange = { showLineNumbers = it },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showFormattersDialog) {
        AlertDialog(
            onDismissRequest = { showFormattersDialog = false },
            title = { Text("Formatierer verwalten", fontWeight = FontWeight.Bold) },
            text = {
                Text("Integrierte Formatierer sind für Prettier, Clang-Format, shfmt und Google-Java-Format aktiv.")
            },
            confirmButton = {
                TextButton(onClick = { showFormattersDialog = false }) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
fun EditorSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun SwitchSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun NavigationSettingRow(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
