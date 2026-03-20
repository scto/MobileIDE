package com.mobileide.app.utils

import androidx.compose.ui.text.input.TextFieldValue

// ── Completion item ────────────────────────────────────────────────────────────
data class Completion(
    val label: String,
    val insertText: String = label,
    val kind: CompletionKind = CompletionKind.KEYWORD,
    val detail: String = "",
    val documentation: String = ""
)

enum class CompletionKind {
    KEYWORD, FUNCTION, CLASS, PROPERTY, SNIPPET, IMPORT, VARIABLE
}

// ── AutoComplete Engine ────────────────────────────────────────────────────────
object AutoCompleteEngine {

    // ── Static keyword pools ───────────────────────────────────────────────────
    private val KOTLIN_KEYWORDS = listOf(
        "fun", "val", "var", "class", "object", "interface", "enum", "sealed",
        "data", "abstract", "open", "override", "private", "public", "protected",
        "internal", "companion", "suspend", "inline", "reified", "crossinline",
        "when", "if", "else", "for", "while", "do", "return", "break", "continue",
        "throw", "try", "catch", "finally", "import", "package", "as", "is", "in",
        "null", "true", "false", "this", "super", "by", "typealias", "init",
        "constructor", "lateinit", "lazy", "const", "annotation", "actual", "expect"
    ).map { Completion(it, it, CompletionKind.KEYWORD) }

    // Compose-specific completions
    private val COMPOSE_COMPLETIONS = listOf(
        Completion("@Composable", "@Composable\n", CompletionKind.SNIPPET, "Annotation"),
        Completion("Column", "Column(\n    modifier = Modifier,\n    verticalArrangement = Arrangement.Top,\n    horizontalAlignment = Alignment.Start\n) {\n    \n}", CompletionKind.FUNCTION, "Layout"),
        Completion("Row", "Row(\n    modifier = Modifier,\n    horizontalArrangement = Arrangement.Start,\n    verticalAlignment = Alignment.Top\n) {\n    \n}", CompletionKind.FUNCTION, "Layout"),
        Completion("Box", "Box(\n    modifier = Modifier,\n    contentAlignment = Alignment.TopStart\n) {\n    \n}", CompletionKind.FUNCTION, "Layout"),
        Completion("Text", "Text(\n    text = \"\",\n    style = MaterialTheme.typography.bodyLarge\n)", CompletionKind.FUNCTION, "UI"),
        Completion("Button", "Button(\n    onClick = {  }\n) {\n    Text(\"\")\n}", CompletionKind.FUNCTION, "UI"),
        Completion("IconButton", "IconButton(onClick = {  }) {\n    Icon(Icons.Default.Add, contentDescription = null)\n}", CompletionKind.FUNCTION, "UI"),
        Completion("TextField", "TextField(\n    value = text,\n    onValueChange = { text = it },\n    label = { Text(\"Label\") }\n)", CompletionKind.FUNCTION, "UI"),
        Completion("OutlinedTextField", "OutlinedTextField(\n    value = text,\n    onValueChange = { text = it },\n    label = { Text(\"Label\") },\n    modifier = Modifier.fillMaxWidth()\n)", CompletionKind.FUNCTION, "UI"),
        Completion("LazyColumn", "LazyColumn(\n    modifier = Modifier.fillMaxSize(),\n    contentPadding = PaddingValues(16.dp),\n    verticalArrangement = Arrangement.spacedBy(8.dp)\n) {\n    items(list) { item ->\n        \n    }\n}", CompletionKind.FUNCTION, "List"),
        Completion("LazyRow", "LazyRow(\n    modifier = Modifier.fillMaxWidth(),\n    horizontalArrangement = Arrangement.spacedBy(8.dp)\n) {\n    items(list) { item ->\n        \n    }\n}", CompletionKind.FUNCTION, "List"),
        Completion("Card", "Card(\n    modifier = Modifier.fillMaxWidth(),\n    onClick = {  }\n) {\n    \n}", CompletionKind.FUNCTION, "UI"),
        Completion("Scaffold", "Scaffold(\n    topBar = {\n        TopAppBar(title = { Text(\"\") })\n    }\n) { padding ->\n    Column(modifier = Modifier.padding(padding)) {\n        \n    }\n}", CompletionKind.SNIPPET, "Layout"),
        Completion("remember", "remember { mutableStateOf() }", CompletionKind.FUNCTION, "State"),
        Completion("rememberSaveable", "rememberSaveable { mutableStateOf() }", CompletionKind.FUNCTION, "State"),
        Completion("collectAsState", "collectAsState()", CompletionKind.FUNCTION, "State"),
        Completion("LaunchedEffect", "LaunchedEffect(key1 = Unit) {\n    \n}", CompletionKind.FUNCTION, "Effect"),
        Completion("DisposableEffect", "DisposableEffect(key1 = Unit) {\n    onDispose {  }\n}", CompletionKind.FUNCTION, "Effect"),
        Completion("SideEffect", "SideEffect {\n    \n}", CompletionKind.FUNCTION, "Effect"),
        Completion("AnimatedVisibility", "AnimatedVisibility(visible = visible) {\n    \n}", CompletionKind.FUNCTION, "Animation"),
        Completion("Modifier.fillMaxSize", "Modifier.fillMaxSize()", CompletionKind.PROPERTY, "Modifier"),
        Completion("Modifier.fillMaxWidth", "Modifier.fillMaxWidth()", CompletionKind.PROPERTY, "Modifier"),
        Completion("Modifier.padding", "Modifier.padding(16.dp)", CompletionKind.PROPERTY, "Modifier"),
        Completion("Modifier.background", "Modifier.background(Color.Transparent)", CompletionKind.PROPERTY, "Modifier"),
        Completion("Modifier.clickable", "Modifier.clickable {  }", CompletionKind.PROPERTY, "Modifier"),
    )

    // ViewModel completions
    private val VIEWMODEL_COMPLETIONS = listOf(
        Completion("viewModelScope.launch", "viewModelScope.launch {\n    \n}", CompletionKind.FUNCTION, "Coroutine"),
        Completion("MutableStateFlow", "MutableStateFlow()", CompletionKind.CLASS, "Flow"),
        Completion("StateFlow", "StateFlow<>", CompletionKind.CLASS, "Flow"),
        Completion("MutableSharedFlow", "MutableSharedFlow<>()", CompletionKind.CLASS, "Flow"),
        Completion("withContext", "withContext(Dispatchers.IO) {\n    \n}", CompletionKind.FUNCTION, "Coroutine"),
        Completion("flow", "flow {\n    emit()\n}.flowOn(Dispatchers.IO)", CompletionKind.FUNCTION, "Flow"),
        Completion("combine", "combine(flow1, flow2) { a, b -> }.stateIn(viewModelScope, SharingStarted.Eagerly, null)", CompletionKind.FUNCTION, "Flow"),
    )

    // Android imports
    private val COMMON_IMPORTS = listOf(
        Completion("import androidx.compose.runtime.*", kind = CompletionKind.IMPORT),
        Completion("import androidx.compose.foundation.layout.*", kind = CompletionKind.IMPORT),
        Completion("import androidx.compose.material3.*", kind = CompletionKind.IMPORT),
        Completion("import androidx.compose.ui.Modifier", kind = CompletionKind.IMPORT),
        Completion("import androidx.compose.ui.unit.dp", kind = CompletionKind.IMPORT),
        Completion("import androidx.lifecycle.viewModelScope", kind = CompletionKind.IMPORT),
        Completion("import kotlinx.coroutines.flow.*", kind = CompletionKind.IMPORT),
        Completion("import kotlinx.coroutines.launch", kind = CompletionKind.IMPORT),
        Completion("import kotlinx.coroutines.Dispatchers", kind = CompletionKind.IMPORT),
    )

    private val ALL_STATIC = KOTLIN_KEYWORDS + COMPOSE_COMPLETIONS + VIEWMODEL_COMPLETIONS

    // ── Main entry: get completions for current cursor context ─────────────────
    fun getCompletions(textValue: TextFieldValue, fileSymbols: List<String> = emptyList()): List<Completion> {
        val prefix = getCurrentWordPrefix(textValue) ?: return emptyList()
        if (prefix.length < 2) return emptyList()

        // Deduplicated match list
        val results = mutableListOf<Completion>()

        // 1. File-local symbols (functions, classes, vals declared in same file)
        fileSymbols
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .take(5)
            .forEach { results += Completion(it, it, CompletionKind.VARIABLE, "local") }

        // 2. Static completions
        ALL_STATIC
            .filter { it.label.startsWith(prefix, ignoreCase = true) && results.none { r -> r.label == it.label } }
            .sortedWith(compareBy({ !it.label.startsWith(prefix) }, { it.label.length }))
            .take(10)
            .forEach { results += it }

        // 3. Import completions (when line starts with "import")
        val lineStart = textValue.text.lastIndexOf('\n', (textValue.selection.start - 1).coerceAtLeast(0))
        val currentLine = textValue.text.substring(lineStart + 1, textValue.selection.start.coerceAtMost(textValue.text.length))
        if (currentLine.trimStart().startsWith("import")) {
            COMMON_IMPORTS
                .filter { it.label.contains(prefix, ignoreCase = true) }
                .forEach { results += it }
        }

        return results.distinctBy { it.label }.take(12)
    }

    // ── Extract symbol declarations from file content ──────────────────────────
    fun extractSymbols(code: String): List<String> {
        val symbols = mutableListOf<String>()
        val patterns = listOf(
            Regex("""fun\s+(\w+)\s*[(<]"""),
            Regex("""class\s+(\w+)"""),
            Regex("""object\s+(\w+)"""),
            Regex("""val\s+(\w+)"""),
            Regex("""var\s+(\w+)"""),
            Regex("""data class\s+(\w+)"""),
            Regex("""sealed class\s+(\w+)"""),
            Regex("""enum class\s+(\w+)"""),
        )
        patterns.forEach { pattern ->
            pattern.findAll(code).forEach { match ->
                match.groupValues.getOrNull(1)?.let { if (it.length > 2) symbols += it }
            }
        }
        return symbols.distinct()
    }

    // ── Get word before cursor ─────────────────────────────────────────────────
    fun getCurrentWordPrefix(textValue: TextFieldValue): String? {
        val text   = textValue.text
        val cursor = textValue.selection.start.coerceIn(0, text.length)
        if (cursor == 0) return null
        var start = cursor - 1
        while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '_' || text[start - 1] == '.')) {
            start--
        }
        val word = text.substring(start, cursor)
        return if (word.length >= 2) word else null
    }

    // ── Apply completion ───────────────────────────────────────────────────────
    fun applyCompletion(textValue: TextFieldValue, completion: Completion): TextFieldValue {
        val text   = textValue.text
        val cursor = textValue.selection.start.coerceIn(0, text.length)
        // Find start of current word
        var wordStart = cursor - 1
        while (wordStart > 0 && (text[wordStart - 1].isLetterOrDigit() || text[wordStart - 1] == '_' || text[wordStart - 1] == '.')) {
            wordStart--
        }
        val newText   = text.substring(0, wordStart) + completion.insertText + text.substring(cursor)
        val newCursor = wordStart + completion.insertText.length
        return textValue.copy(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newCursor)
        )
    }
}
