package com.mobileide.app.ui.screens.settings.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mobileide.app.editor.AVAILABLE_FONTS
import com.mobileide.app.ui.screens.settings.*
import com.mobileide.app.utils.EditorSettings
import com.mobileide.app.utils.EditorTheme
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettingsScreen(vm: IDEViewModel) {
    val settings by vm.editorSettings.collectAsState()
    val theme    by vm.currentThemeName.collectAsState()
    val scope    = rememberCoroutineScope()
    val snack    = remember { SnackbarHostState() }

    // Local mutable copies
    var fontSize         by remember(settings) { mutableStateOf(settings.fontSize) }
    var tabSize          by remember(settings) { mutableStateOf(settings.tabSize.toFloat()) }
    var showLineNumbers  by remember(settings) { mutableStateOf(settings.showLineNumbers) }
    var wordWrap         by remember(settings) { mutableStateOf(settings.wordWrap) }
    var autoComplete     by remember(settings) { mutableStateOf(settings.autoComplete) }
    var bracketClose     by remember(settings) { mutableStateOf(settings.bracketAutoClose) }
    var autoIndent       by remember(settings) { mutableStateOf(settings.autoIndent) }
    var stickyScroll     by remember(settings) { mutableStateOf(settings.stickyScroll) }
    var highlightLine    by remember(settings) { mutableStateOf(settings.highlightCurrentLine) }
    var fontPath         by remember(settings) { mutableStateOf(settings.fontPath) }
    var selectedTheme    by remember(theme)    { mutableStateOf(theme) }

    // Extra editor options (persisted via DataStore in a future iteration)
    var readonlyMode     by remember { mutableStateOf(false) }
    var detectBinary     by remember { mutableStateOf(true) }
    var oomPredict       by remember { mutableStateOf(true) }
    var disableVirtKbd   by remember { mutableStateOf(true) }
    var cursorAnim       by remember { mutableStateOf(true) }
    var pinLineNumbers   by remember { mutableStateOf(false) }
    var showWhitespace   by remember { mutableStateOf(false) }
    var kbdSuggestions   by remember { mutableStateOf(false) }
    var quickDelete      by remember { mutableStateOf(true) }
    var completeOnEnter  by remember { mutableStateOf(true) }
    var tmAutocomplete   by remember { mutableStateOf(true) }
    var insertTabs       by remember { mutableStateOf(false) }
    var restoreSession   by remember { mutableStateOf(true) }
    var smoothTabSwitch  by remember { mutableStateOf(false) }
    var tabIcons         by remember { mutableStateOf(true) }
    var autoSave         by remember { mutableStateOf(true) }
    var autoCloseTag     by remember { mutableStateOf(true) }
    var bulletContinue   by remember { mutableStateOf(true) }
    var formatOnSave     by remember { mutableStateOf(false) }
    var insertFinalNl    by remember { mutableStateOf(true) }
    var trimTrailing     by remember { mutableStateOf(true) }
    var editorConfig     by remember { mutableStateOf(true) }
    var openCreatedFiles by remember { mutableStateOf(true) }
    var drawerClosed     by remember { mutableStateOf(false) }
    var hiddenFiles      by remember { mutableStateOf(true) }
    var compactFolders   by remember { mutableStateOf(true) }

    fun save() {
        vm.saveEditorSettings(EditorSettings(
            fontSize             = fontSize,
            tabSize              = tabSize.toInt(),
            showLineNumbers      = showLineNumbers,
            wordWrap             = wordWrap,
            autoComplete         = autoComplete,
            bracketAutoClose     = bracketClose,
            autoIndent           = autoIndent,
            stickyScroll         = stickyScroll,
            highlightCurrentLine = highlightLine,
            fontPath             = fontPath,
        ))
        vm.setTheme(selectedTheme)
        scope.launch { snack.showSnackbar("Einstellungen gespeichert", duration = SnackbarDuration.Short) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.SETTINGS) }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    TextButton(onClick = { save() }) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Speichern", color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = {
            SnackbarHost(snack) { data ->
                Snackbar(data, shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor   = MaterialTheme.colorScheme.inverseOnSurface)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Language server ───────────────────────────────────────────────
            SettingsSection("Sprachserver")
            SettingsGroup {
                SettingsNavItem(Icons.Default.Hub, "Sprachserver verwalten",
                    "Sprachserver installieren, verbinden und konfigurieren") {
                    vm.navigate(Screen.SETTINGS_LSP)
                }
                SettingsToggle("Beim Speichern formatieren",
                    "Die Datei beim Speichern automatisch formatieren", formatOnSave) { formatOnSave = it }
                SettingsToggle("Finalen Zeilenumbruch einfügen",
                    "Stelle sicher, dass die Datei beim Formatieren mit einem Zeilenumbruch endet",
                    insertFinalNl) { insertFinalNl = it }
                SettingsToggle("Nachfolgende Leerzeichen entfernen",
                    "Bei der Formatierung nachfolgende Leerzeichen vor Zeilenumbrüchen entfernen",
                    trimTrailing) { trimTrailing = it }
            }

            // ── Intelligent features ──────────────────────────────────────────
            SettingsSection("Intelligente Funktionen")
            SettingsGroup {
                SettingsToggle("Tags automatisch schließen",
                    "HTML-Tags automatisch schließen", autoCloseTag) { autoCloseTag = it }
                SettingsToggle("Aufzählung fortsetzen",
                    "Listen und Zitate in Markdown automatisch fortsetzen",
                    bulletContinue) { bulletContinue = it }
            }

            // ── Editor ────────────────────────────────────────────────────────
            SettingsSection("Editor")
            SettingsGroup {
                SettingsToggle("Lesemodus", "Lesemodus standardmäßig aktivieren",
                    readonlyMode) { readonlyMode = it }
                SettingsToggle("Binärdateien erkennen",
                    "Binärdateien standardmäßig im schreibgeschützten Modus öffnen",
                    detectBinary) { detectBinary = it }
                SettingsToggle("OOM-Vorhersage",
                    "Out-of-Memory-Dialog anzeigen", oomPredict) { oomPredict = it }
                SettingsToggle("Virtuelle Tastatur deaktivieren",
                    "Virtuelle Tastatur deaktivieren, wenn eine Hardware-Tastatur verfügbar ist",
                    disableVirtKbd) { disableVirtKbd = it }
                SettingsItem("Zeilenabstand", "Höhenmultiplikator für jede Zeile im Editor")
                SettingsToggle("Cursor-Animation", "Flüssige Cursor-Animationen aktivieren",
                    cursorAnim) { cursorAnim = it }
                SettingsToggle("Zeilennummern anzeigen", "Zeilennummern anzeigen",
                    showLineNumbers) { showLineNumbers = it }
                SettingsToggle("Zeilennummern anheften", "Zeilennummern anheften",
                    pinLineNumbers) { pinLineNumbers = it }
                SettingsToggle("Leerzeichen anzeigen",
                    "Leerzeichen, Tabs und Zeilenumbrüche als sichtbare Symbole anzeigen",
                    showWhitespace) { showWhitespace = it }
                SettingsToggle("Tastaturvorschläge anzeigen",
                    "Tastaturvorschläge anzeigen", kbdSuggestions) { kbdSuggestions = it }
                SettingsToggle("Sticky Scroll aktivieren",
                    "Die aktuelle Codeblock-Überschrift beim Scrollen sichtbar halten",
                    stickyScroll) { stickyScroll = it }
                SettingsToggle("Schnelllöschung aktivieren",
                    "Die gesamte Zeile automatisch löschen, wenn diese leer ist",
                    quickDelete) { quickDelete = it }
                SettingsNavItem(Icons.Default.FontDownload, "Editor-Schriftarten verwalten",
                    "Editor-Schriftarten verwalten") { }
                SettingsItem("Textgröße", "Textgröße festlegen")
                SettingsToggle("Bei Enter vervollständigen",
                    "Den ersten Vorschlag beim Drücken von Enter auf der Software-Tastatur auswählen",
                    completeOnEnter) { completeOnEnter = it }
                SettingsToggle("TextMate-Autovervollständigung",
                    "Vordefinierte Schlüsselwörter vorschlagen", tmAutocomplete) { tmAutocomplete = it }
                SettingsItem("Tabgröße", "Anzahl der für ein Tab-Zeichen eingefügten Leerzeichen")
                SettingsToggle("Tab-Zeichen einfügen",
                    "Einen echten Tab (\\t) statt Leerzeichen für Einrückungen verwenden",
                    insertTabs) { insertTabs = it }
            }

            // ── Actions ───────────────────────────────────────────────────────
            SettingsSection("Aktionen")
            SettingsGroup {
                SettingsNavItem(Icons.Default.Tune, "Toolbar-Aktionen",
                    "Aktionen auswählen, die in der App-Leiste angezeigt werden sollen") { }
                SettingsToggle("Zusatztasten", "Zusatztasten über der Tastatur anzeigen",
                    true) { }
                SettingsToggle("Zusatztasten-Hintergrund",
                    "Hintergrund für Zusatztasten anzeigen", true) { }
                SettingsToggle("Zusatztasten teilen",
                    "Zusatztastenfeld in zwei Zeilen aufteilen, um Befehle und Symbole zu trennen",
                    false) { }
                SettingsNavItem(Icons.Default.Edit, "Zusatztasten bearbeiten",
                    "Zusatztasten über der Tastatur anpassen") { }
            }

            // ── Drawer ────────────────────────────────────────────────────────
            SettingsSection("Drawer")
            SettingsGroup {
                SettingsToggle("Drawer geschlossen halten",
                    "Drawer beim Öffnen einer Datei geschlossen halten",
                    drawerClosed) { drawerClosed = it }
                SettingsItem("Sortiermodus", "Standard-Sortiermodus auswählen")
                SettingsToggle("Versteckte Dateien im Drawer anzeigen",
                    "Versteckte Dateien oder Ordner wie .git im Drawer anzeigen",
                    hiddenFiles) { hiddenFiles = it }
                SettingsNavItem(Icons.Default.FilterAlt, "Dateien im Drawer ausschließen",
                    "Dateien ändern, die vom Dateibaum ausgeschlossen werden sollen") { }
                SettingsToggle("Kompakte Ordner im Drawer",
                    "Ordner mit nur einem Unterordner im Drawer zu einem Ordner zusammenfassen",
                    compactFolders) { compactFolders = it }
                SettingsToggle("Versteckte Dateien in der Suche anzeigen",
                    "Versteckte Dateien oder Ordner in den Suchergebnissen anzeigen",
                    hiddenFiles) { }
                SettingsToggle("Projekte immer indizieren",
                    "Projekte standardmäßig automatisch indizieren. Dies kann eine erhebliche Menge an Gerätespeicher verbrauchen.",
                    true) { }
                SettingsNavItem(Icons.Default.FilterAlt, "Dateien in der Suche ausschließen",
                    "Dateien von der Indizierung ausschließen. Dies kann die Suchzeiten bei großen Projekten verbessern.") { }
                SettingsToggle("Erstellte Dateien öffnen",
                    "Neu erstellte Dateien automatisch öffnen",
                    openCreatedFiles) { openCreatedFiles = it }
            }

            // ── Misc ──────────────────────────────────────────────────────────
            SettingsSection("Sonstiges")
            SettingsGroup {
                SettingsToggle("Sitzung wiederherstellen",
                    "Vorherige Tabs wiederherstellen", restoreSession) { restoreSession = it }
                SettingsToggle("Sanfter Tabwechsel",
                    "Sanftes Wechseln zwischen Tabs", smoothTabSwitch) { smoothTabSwitch = it }
                SettingsToggle("Tab-Symbole anzeigen",
                    "Dateisymbole neben den Tab-Titeln anzeigen", tabIcons) { tabIcons = it }
                SettingsItem("Standardcodierung", "Editor-Standardcodierung")
                SettingsItem("Zeilenende", "Das Standard-Zeilenende ändern")
                SettingsToggle("Automatisch speichern",
                    "Datei automatisch speichern", autoSave) { autoSave = it }
                SettingsItem("Verzögerung beim automatischen Speichern",
                    "Verzögerung in Millisekunden zwischen automatischem Speichern")
                SettingsToggle("EditorConfig-Unterstützung aktivieren",
                    "Codestil-Regeln aus .editorconfig-Dateien automatisch anwenden",
                    editorConfig) { editorConfig = it }
            }
        }
    }
}
