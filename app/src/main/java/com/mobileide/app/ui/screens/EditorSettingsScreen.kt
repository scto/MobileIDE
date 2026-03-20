package com.mobileide.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.mobileide.app.editor.AVAILABLE_FONTS
import com.mobileide.app.ui.theme.*
import com.mobileide.app.utils.EditorSettings
import com.mobileide.app.utils.EditorTheme
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettingsScreen(vm: IDEViewModel) {
    val currentTheme   by vm.currentThemeName.collectAsState()
    val editorSettings by vm.editorSettings.collectAsState()
    val scope          = rememberCoroutineScope()
    val snackbarState  = remember { SnackbarHostState() }

    var fontSize         by remember(editorSettings.fontSize)             { mutableStateOf(editorSettings.fontSize) }
    var tabSize          by remember(editorSettings.tabSize)              { mutableStateOf(editorSettings.tabSize.toFloat()) }
    var lineNumbers      by remember(editorSettings.showLineNumbers)      { mutableStateOf(editorSettings.showLineNumbers) }
    var wordWrap         by remember(editorSettings.wordWrap)             { mutableStateOf(editorSettings.wordWrap) }
    var autoComplete     by remember(editorSettings.autoComplete)         { mutableStateOf(editorSettings.autoComplete) }
    var bracketAutoClose by remember(editorSettings.bracketAutoClose)     { mutableStateOf(editorSettings.bracketAutoClose) }
    var autoIndent       by remember(editorSettings.autoIndent)           { mutableStateOf(editorSettings.autoIndent) }
    var stickyScroll     by remember(editorSettings.stickyScroll)         { mutableStateOf(editorSettings.stickyScroll) }
    var highlightLine    by remember(editorSettings.highlightCurrentLine) { mutableStateOf(editorSettings.highlightCurrentLine) }
    var selectedTheme    by remember(currentTheme)                        { mutableStateOf(currentTheme) }
    var selectedFont     by remember(editorSettings.fontPath)             { mutableStateOf(editorSettings.fontPath) }

    fun save() {
        vm.saveEditorSettings(EditorSettings(
            fontSize             = fontSize,
            tabSize              = tabSize.toInt(),
            showLineNumbers      = lineNumbers,
            wordWrap             = wordWrap,
            autoComplete         = autoComplete,
            bracketAutoClose     = bracketAutoClose,
            autoIndent           = autoIndent,
            stickyScroll         = stickyScroll,
            highlightCurrentLine = highlightLine,
            fontPath             = selectedFont,
        ))
        vm.setTheme(selectedTheme)
        scope.launch {
            snackbarState.showSnackbar(
                message     = "Settings saved",
                duration    = SnackbarDuration.Short,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.SETTINGS) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { save() }) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Save",
                            color      = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState) { data ->
                Snackbar(
                    snackbarData     = data,
                    containerColor   = MaterialTheme.colorScheme.inverseSurface,
                    contentColor     = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor      = MaterialTheme.colorScheme.inversePrimary,
                    shape            = RoundedCornerShape(12.dp),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Editor Theme ──────────────────────────────────────────────────
            Section("Editor Theme") {
                EditorTheme.entries.forEach { theme ->
                    SimpleThemeCard(
                        name     = theme.displayName,
                        isDark   = theme.isDark,
                        isActive = selectedTheme == theme.displayName
                    ) { selectedTheme = theme.displayName }
                }
            }

            // ── Font ──────────────────────────────────────────────────────────
            Section("Editor Font") {
                AVAILABLE_FONTS.forEach { (name, path) ->
                    FontCard(
                        name     = name,
                        isActive = selectedFont == path
                    ) { selectedFont = path }
                }
            }

            // ── Text & Font ───────────────────────────────────────────────────
            Section("Text & Font") {
                SliderRow("Font Size", fontSize, 10f..28f, "${fontSize.toInt()} sp") { fontSize = it }
                SliderRow("Tab Size",  tabSize,  2f..8f,  "${tabSize.toInt()} spaces") { tabSize = it }
                FontPreview(fontSize)
            }

            // ── Behaviour ─────────────────────────────────────────────────────
            Section("Behaviour") {
                SwitchRow("Line Numbers",           lineNumbers)      { lineNumbers      = it }
                SwitchRow("Word Wrap",              wordWrap)         { wordWrap         = it }
                SwitchRow("Auto-Complete",          autoComplete)     { autoComplete     = it }
                SwitchRow("Auto-Close Brackets",    bracketAutoClose) { bracketAutoClose = it }
                SwitchRow("Auto-Indent",            autoIndent)       { autoIndent       = it }
                SwitchRow("Sticky Scope Header",    stickyScroll)     { stickyScroll     = it }
                SwitchRow("Highlight Current Line", highlightLine)    { highlightLine    = it }
            }

            // ── Sora Editor info ──────────────────────────────────────────────
            Section("Powered by Sora Editor") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sora Editor v0.23.4", fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("github.com/Rosemoe/sora-editor",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(4.dp))
                        listOf(
                            "TextMate grammar highlighting",
                            "47 bundled language grammars",
                            "Intelligent auto-complete popup",
                            "Bracket matching & auto-close",
                            "Auto-indent with language awareness",
                            "Find & replace",
                            "Multiple cursor & column selection",
                            "Sticky scope header",
                            "Hardware-accelerated GPU rendering"
                        ).forEach { f ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, null, Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text(f, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FontPreview(fontSize: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Preview", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp))
            Text("fun greet(name: String) {",
                fontSize = fontSize.sp, fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface)
            Text("    println(\"Hello, \$name!\")",
                fontSize = fontSize.sp, fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary)
            Text("}",
                fontSize = fontSize.sp, fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SimpleThemeCard(name: String, isDark: Boolean, isActive: Boolean, onClick: () -> Unit) {
    val border = if (isActive)
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    else
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        border = border
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp, 24.dp)
                    .background(
                        if (isDark) androidx.compose.ui.graphics.Color(0xFF282A36)
                        else androidx.compose.ui.graphics.Color(0xFFF5F5F5),
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(if (isDark) "Dark" else "Light",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                null, Modifier.size(18.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun FontCard(name: String, isActive: Boolean, onClick: () -> Unit) {
    val border = if (isActive)
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    else
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        border = border
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, fontFamily = FontFamily.Monospace,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface)
            Icon(
                if (isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                null, Modifier.size(18.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface)
            Switch(value, onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                ))
        }
    }
}

@Composable
private fun SliderRow(
    label: String, value: Float,
    range: ClosedFloatingPointRange<Float>, display: String,
    onChange: (Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row {
                Text(label, modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface)
                Text(display, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace)
            }
            Slider(value, onChange, valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ))
        }
    }
}
