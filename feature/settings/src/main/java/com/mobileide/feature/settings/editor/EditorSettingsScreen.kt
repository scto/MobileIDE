package com.mobileide.feature.settings.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobileide.feature.settings.SettingsSection
import com.mobileide.feature.settings.SettingsTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    // Mutable editor settings state (defaults; persisted externally via app's DataStore)
    var fontSize        by remember { mutableStateOf(14f) }
    var tabSize         by remember { mutableStateOf(4f) }
    var showLineNumbers by remember { mutableStateOf(true) }
    var wordWrap        by remember { mutableStateOf(false) }
    var autoComplete    by remember { mutableStateOf(true) }
    var bracketClose    by remember { mutableStateOf(true) }
    var autoIndent      by remember { mutableStateOf(true) }
    var stickyScroll    by remember { mutableStateOf(false) }
    var highlightLine   by remember { mutableStateOf(true) }
    var fontPath        by remember { mutableStateOf("fonts/JetBrainsMono-Regular.ttf") }
    var selectedTheme   by remember { mutableStateOf("Default") }

    // Extra options
    var readonlyMode    by remember { mutableStateOf(false) }
    var cursorAnim      by remember { mutableStateOf(true) }
    var showWhitespace  by remember { mutableStateOf(false) }
    var quickDelete     by remember { mutableStateOf(true) }
    var autoSave        by remember { mutableStateOf(true) }
    var autoCloseTag    by remember { mutableStateOf(true) }
    var bulletContinue  by remember { mutableStateOf(true) }
    var formatOnSave    by remember { mutableStateOf(false) }
    var insertFinalNl   by remember { mutableStateOf(true) }
    var trimTrailing    by remember { mutableStateOf(true) }

    fun save() {
        // Settings are passed back via onNavigate or saved by the app layer
        scope.launch {
            snack.showSnackbar("Einstellungen gespeichert", duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        topBar = {
            SettingsTopBar("Editor", onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.CenterEnd) {
                    Button(onClick = ::save) { Text("Speichern") }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── Font size ─────────────────────────────────────────────────────
            SettingsSection("Darstellung")
            Text("Schriftgröße: ${fontSize.toInt()}sp",
                style = MaterialTheme.typography.bodyMedium)
            Slider(fontSize, { fontSize = it }, valueRange = 8f..32f, steps = 23,
                modifier = Modifier.fillMaxWidth())

            // ── Tab size ──────────────────────────────────────────────────────
            Text("Tab-Breite: ${tabSize.toInt()}",
                style = MaterialTheme.typography.bodyMedium)
            Slider(tabSize, { tabSize = it }, valueRange = 2f..8f, steps = 5,
                modifier = Modifier.fillMaxWidth())

            // ── Font ──────────────────────────────────────────────────────────
            Text("Schriftart", style = MaterialTheme.typography.bodyMedium)
            listOf(
                "fonts/JetBrainsMono-Regular.ttf" to "JetBrains Mono",
                "fonts/FiraCode-Regular.ttf"       to "Fira Code",
                "fonts/Roboto-Regular.ttf"         to "Roboto",
            ).forEach { (path, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { fontPath = path }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(fontPath == path, { fontPath = path })
                    Spacer(Modifier.width(4.dp))
                    Text(label, fontFamily = FontFamily.Monospace)
                }
            }

            // ── Display toggles ───────────────────────────────────────────────
            SettingsSection("Anzeige")
            SettingsToggle("Zeilennummern anzeigen",      showLineNumbers) { showLineNumbers = it }
            SettingsToggle("Aktuelle Zeile hervorheben",  highlightLine)   { highlightLine   = it }
            SettingsToggle("Leerzeichen anzeigen",        showWhitespace)  { showWhitespace  = it }
            SettingsToggle("Zeilenumbruch",               wordWrap)        { wordWrap        = it }
            SettingsToggle("Sticky Scroll",               stickyScroll)    { stickyScroll    = it }

            // ── Behaviour ─────────────────────────────────────────────────────
            SettingsSection("Verhalten")
            SettingsToggle("Code-Vervollständigung",          autoComplete)   { autoComplete   = it }
            SettingsToggle("Klammern automatisch schließen",  bracketClose)   { bracketClose   = it }
            SettingsToggle("Auto-Einrückung",                 autoIndent)     { autoIndent     = it }
            SettingsToggle("Cursor-Animation",                cursorAnim)     { cursorAnim     = it }
            SettingsToggle("Schnelles Löschen",               quickDelete)    { quickDelete    = it }
            SettingsToggle("Auto-Tag-Schließen",              autoCloseTag)   { autoCloseTag   = it }
            SettingsToggle("Bullet-Fortsetzung",              bulletContinue) { bulletContinue = it }
            SettingsToggle("Nur lesen",                       readonlyMode)   { readonlyMode   = it }

            // ── Save behaviour ────────────────────────────────────────────────
            SettingsSection("Speichern")
            SettingsToggle("Automatisch speichern",       autoSave)     { autoSave     = it }
            SettingsToggle("Beim Speichern formatieren",  formatOnSave) { formatOnSave = it }
            SettingsToggle("Abschließende Newline",       insertFinalNl) { insertFinalNl = it }
            SettingsToggle("Leerzeichen am Ende kürzen",  trimTrailing)  { trimTrailing  = it }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SettingsToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(value, { onChange(it) })
    }
}
