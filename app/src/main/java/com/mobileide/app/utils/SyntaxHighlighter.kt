package com.mobileide.app.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mobileide.app.data.Language
import com.mobileide.app.ui.theme.ActiveTheme

object SyntaxHighlighter {

    private val KOTLIN_KEYWORDS = setOf(
        "fun","val","var","class","object","interface","enum","sealed","data","abstract",
        "open","override","private","public","protected","internal","companion","suspend",
        "inline","reified","crossinline","noinline","if","else","when","for","while","do",
        "return","break","continue","throw","try","catch","finally","import","package",
        "as","is","in","null","true","false","this","super","by","typealias","init",
        "constructor","get","set","it","lateinit","lazy","const","annotation","actual","expect"
    )

    private val JAVA_KEYWORDS = setOf(
        "public","private","protected","static","final","abstract","class","interface",
        "extends","implements","new","return","void","int","long","double","float",
        "boolean","char","byte","short","if","else","for","while","do","switch","case",
        "break","continue","throw","throws","try","catch","finally","import","package",
        "null","true","false","this","super","instanceof","synchronized","enum"
    )

    fun highlight(code: String, language: Language): AnnotatedString {
        return when (language) {
            Language.KOTLIN, Language.GRADLE -> highlightKotlin(code)
            Language.JAVA                    -> highlightJava(code)
            Language.XML                     -> highlightXml(code)
            Language.JSON                    -> highlightJson(code)
            else                             -> AnnotatedString(code)
        }
    }

    // ── Kotlin ────────────────────────────────────────────────────────────────
    private fun highlightKotlin(code: String): AnnotatedString = buildAnnotatedString {
        append(code)
        val tokens = tokenize(code, KOTLIN_KEYWORDS)
        tokens.forEach { (s, e, color, bold, italic) ->
            addStyle(SpanStyle(color = color,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle  = if (italic) FontStyle.Italic else FontStyle.Normal), s, e)
        }
    }

    private fun highlightJava(code: String): AnnotatedString = buildAnnotatedString {
        append(code)
        tokenize(code, JAVA_KEYWORDS).forEach { (s, e, color, bold, italic) ->
            addStyle(SpanStyle(color = color,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle  = if (italic) FontStyle.Italic else FontStyle.Normal), s, e)
        }
    }

    private data class Token(val start: Int, val end: Int, val color: Color,
        val bold: Boolean = false, val italic: Boolean = false)

    private fun tokenize(code: String, keywords: Set<String>): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < code.length) {
            when {
                // Triple-quoted strings
                code.startsWith("\"\"\"", i) -> {
                    val end = code.indexOf("\"\"\"", i + 3).let { if (it < 0) code.length else it + 3 }
                    tokens += Token(i, end, ActiveTheme.syntaxString); i = end
                }
                // Line comment
                code.startsWith("//", i) -> {
                    val end = code.indexOf('\n', i).let { if (it < 0) code.length else it }
                    tokens += Token(i, end, ActiveTheme.syntaxComment, italic = true); i = end
                }
                // Block comment
                code.startsWith("/*", i) -> {
                    val end = code.indexOf("*/", i + 2).let { if (it < 0) code.length else it + 2 }
                    tokens += Token(i, end, ActiveTheme.syntaxComment, italic = true); i = end
                }
                // String literal
                code[i] == '"' -> {
                    var j = i + 1
                    while (j < code.length && code[j] != '"' && code[j] != '\n') {
                        if (code[j] == '\\') j++
                        j++
                    }
                    tokens += Token(i, minOf(j + 1, code.length), ActiveTheme.syntaxString); i = j + 1
                }
                // Char literal
                code[i] == '\'' -> {
                    val end = code.indexOf('\'', i + 1).let { if (it < 0) i + 1 else it + 1 }
                    tokens += Token(i, minOf(end, code.length), ActiveTheme.syntaxString); i = end
                }
                // Annotation
                code[i] == '@' && i + 1 < code.length && code[i + 1].isLetter() -> {
                    var j = i + 1
                    while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                    tokens += Token(i, j, ActiveTheme.syntaxAnnotation); i = j
                }
                // Number
                code[i].isDigit() || (code[i] == '-' && i + 1 < code.length && code[i+1].isDigit()
                        && (i == 0 || !code[i-1].isLetterOrDigit())) -> {
                    var j = i + 1
                    while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '.' || code[j] == '_')) j++
                    tokens += Token(i, j, ActiveTheme.syntaxNumber); i = j
                }
                // Word
                code[i].isLetter() || code[i] == '_' -> {
                    var j = i
                    while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                    val word = code.substring(i, j)
                    val color = when {
                        word in keywords           -> ActiveTheme.syntaxKeyword
                        j < code.length && code[j] == '(' -> ActiveTheme.syntaxFunction
                        word.isNotEmpty() && word[0].isUpperCase() -> ActiveTheme.syntaxType
                        else -> ActiveTheme.syntaxPlain
                    }
                    tokens += Token(i, j, color, bold = word in keywords); i = j
                }
                else -> i++
            }
        }
        return tokens
    }

    // ── XML ───────────────────────────────────────────────────────────────────
    private fun highlightXml(code: String): AnnotatedString = buildAnnotatedString {
        append(code)
        var i = 0
        while (i < code.length) {
            when {
                code.startsWith("<!--", i) -> {
                    val end = code.indexOf("-->", i).let { if (it < 0) code.length else it + 3 }
                    addStyle(SpanStyle(color = ActiveTheme.syntaxComment, fontStyle = FontStyle.Italic), i, end); i = end
                }
                code.startsWith("<?", i) -> {
                    val end = code.indexOf("?>", i).let { if (it < 0) code.length else it + 2 }
                    addStyle(SpanStyle(color = ActiveTheme.syntaxAnnotation), i, end); i = end
                }
                code[i] == '<' -> {
                    var j = i + 1
                    if (j < code.length && code[j] == '/') j++
                    while (j < code.length && code[j] != ' ' && code[j] != '>' && code[j] != '\n' && code[j] != '/') j++
                    addStyle(SpanStyle(color = ActiveTheme.syntaxKeyword, fontWeight = FontWeight.Bold), i, j); i = j
                }
                code[i] == '"' -> {
                    var j = i + 1
                    while (j < code.length && code[j] != '"') j++
                    addStyle(SpanStyle(color = ActiveTheme.syntaxString), i, minOf(j + 1, code.length)); i = j + 1
                }
                code[i].isLetter() && i > 0 && code[i-1] == ' ' -> {
                    var j = i
                    while (j < code.length && code[j] != '=' && code[j] != ' ' && code[j] != '>') j++
                    addStyle(SpanStyle(color = ActiveTheme.syntaxFunction), i, j); i = j
                }
                else -> i++
            }
        }
    }

    // ── JSON ──────────────────────────────────────────────────────────────────
    private fun highlightJson(code: String): AnnotatedString = buildAnnotatedString {
        append(code)
        var i = 0; var isKey = true
        while (i < code.length) {
            when {
                code[i] == '"' -> {
                    var j = i + 1
                    while (j < code.length && code[j] != '"') { if (code[j] == '\\') j++; j++ }
                    val color = if (isKey) ActiveTheme.syntaxFunction else ActiveTheme.syntaxString
                    addStyle(SpanStyle(color = color), i, minOf(j + 1, code.length))
                    isKey = false; i = j + 1
                }
                code[i] == ':' -> { isKey = false; i++ }
                code[i] == ',' || code[i] == '{' || code[i] == '[' -> { isKey = true; i++ }
                code[i].isDigit() || code[i] == '-' -> {
                    var j = i
                    while (j < code.length && (code[j].isDigit() || code[j] in ".-eE+")) j++
                    addStyle(SpanStyle(color = ActiveTheme.syntaxNumber), i, j); i = j
                }
                code.startsWith("true", i) || code.startsWith("false", i) || code.startsWith("null", i) -> {
                    val end = i + when { code.startsWith("false", i) -> 5; else -> 4 }
                    addStyle(SpanStyle(color = ActiveTheme.syntaxKeyword, fontWeight = FontWeight.Bold), i, minOf(end, code.length)); i = end
                }
                else -> i++
            }
        }
    }

    // ── Outline extraction ────────────────────────────────────────────────────
    fun extractOutline(code: String): List<com.mobileide.app.data.OutlineSymbol> {
        val symbols = mutableListOf<com.mobileide.app.data.OutlineSymbol>()
        val patterns = listOf(
            Regex("""^(class|data class|sealed class|abstract class)\s+(\w+)""", RegexOption.MULTILINE) to com.mobileide.app.data.SymbolKind.CLASS,
            Regex("""^(object)\s+(\w+)""", RegexOption.MULTILINE) to com.mobileide.app.data.SymbolKind.OBJECT,
            Regex("""^(interface)\s+(\w+)""", RegexOption.MULTILINE) to com.mobileide.app.data.SymbolKind.INTERFACE,
            Regex("""^(enum class)\s+(\w+)""", RegexOption.MULTILINE) to com.mobileide.app.data.SymbolKind.ENUM,
            Regex("""^\s*fun\s+(\w+)\s*\(""", RegexOption.MULTILINE) to com.mobileide.app.data.SymbolKind.FUNCTION,
            Regex("""^\s*val\s+(\w+)""", RegexOption.MULTILINE) to com.mobileide.app.data.SymbolKind.PROPERTY,
            Regex("""^\s*var\s+(\w+)""", RegexOption.MULTILINE) to com.mobileide.app.data.SymbolKind.VARIABLE,
        )
        patterns.forEach { (regex, kind) ->
            regex.findAll(code).forEach { match ->
                val line = code.substring(0, match.range.first).count { it == '\n' } + 1
                val name = match.groupValues.lastOrNull { it.isNotEmpty() && it != match.groupValues[0] } ?: ""
                if (name.isNotEmpty())
                    symbols += com.mobileide.app.data.OutlineSymbol(name, kind, line)
            }
        }
        return symbols.sortedBy { it.line }
    }
}
