package com.mobileide.app.utils

import com.mobileide.app.data.Language
import com.mobileide.app.data.TodoItem
import com.mobileide.app.data.TodoTag
import java.io.File

object CodeFormatter {

    fun format(code: String, language: Language, tabSize: Int = 4): String = when (language) {
        Language.KOTLIN, Language.GRADLE -> formatKotlinJava(code, tabSize)
        Language.JAVA                    -> formatKotlinJava(code, tabSize)
        Language.XML                     -> formatXml(code, tabSize)
        Language.JSON                    -> formatJson(code, tabSize)
        else                             -> code
    }

    private fun formatKotlinJava(code: String, tabSize: Int): String {
        val indent = " ".repeat(tabSize)
        val sb = StringBuilder()
        var depth = 0
        for (rawLine in code.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) { sb.appendLine(); continue }
            val leadClose = line.takeWhile { it == '}' || it == ')' || it == ']' }
                .count { it == '}' || it == ')' || it == ']' }
            depth = maxOf(0, depth - leadClose)
            sb.append(indent.repeat(depth))
            sb.appendLine(line)
            val opens  = line.count { it == '{' || it == '(' || it == '[' }
            val closes = line.count { it == '}' || it == ')' || it == ']' }
            depth = maxOf(0, depth + opens - closes + leadClose)
        }
        return sb.toString().trimEnd()
            .replace(Regex("\n{3,}"), "\n\n")
            .lines().joinToString("\n") { it.trimEnd() }
    }

    private fun formatXml(code: String, tabSize: Int): String {
        val indent = " ".repeat(tabSize)
        val sb = StringBuilder()
        var depth = 0
        val tagRe = Regex("""(<[^>]+>|[^<]+)""")
        tagRe.findAll(code).map { it.value.trim() }.filter { it.isNotEmpty() }.forEach { token ->
            when {
                token.startsWith("</")  -> { depth = maxOf(0, depth - 1); sb.appendLine("${indent.repeat(depth)}$token") }
                token.endsWith("/>")    -> sb.appendLine("${indent.repeat(depth)}$token")
                token.startsWith("<?")  -> sb.appendLine(token)
                token.startsWith("<!--")-> sb.appendLine("${indent.repeat(depth)}$token")
                token.startsWith("<")   -> { sb.appendLine("${indent.repeat(depth)}$token"); depth++ }
                token.isNotBlank()      -> sb.appendLine("${indent.repeat(depth)}$token")
            }
        }
        return sb.toString().trimEnd()
    }

    fun formatJson(code: String, tabSize: Int = 4): String {
        val indent = " ".repeat(tabSize)
        val sb = StringBuilder()
        var depth = 0; var inStr = false; var escape = false; var i = 0
        while (i < code.length) {
            val c = code[i]
            when {
                escape           -> { sb.append(c); escape = false }
                c == '\\' && inStr -> { sb.append(c); escape = true }
                c == '"'         -> { sb.append(c); inStr = !inStr }
                inStr            -> sb.append(c)
                c == '{' || c == '[' -> { sb.append(c).appendLine(); depth++; sb.append(indent.repeat(depth)) }
                c == '}' || c == ']' -> { sb.appendLine(); depth = maxOf(0, depth - 1); sb.append(indent.repeat(depth)).append(c) }
                c == ','         -> { sb.append(c).appendLine(); sb.append(indent.repeat(depth)) }
                c == ':'         -> sb.append(": ")
                c == ' ' || c == '\n' || c == '\r' || c == '\t' -> {}
                else             -> sb.append(c)
            }; i++
        }
        return sb.toString()
    }

    fun organizeImports(code: String): String {
        val lines   = code.lines()
        val imports = lines.filter { it.trimStart().startsWith("import ") }.sorted().distinct()
        val other   = lines.filter { !it.trimStart().startsWith("import ") }.toMutableList()
        val pkgLine = other.indexOfFirst { it.trimStart().startsWith("package ") }
        if (pkgLine != -1) {
            while (pkgLine + 1 < other.size && other[pkgLine + 1].isBlank()) other.removeAt(pkgLine + 1)
            other.add(pkgLine + 1, "")
            imports.forEachIndexed { idx, imp -> other.add(pkgLine + 2 + idx, imp) }
        }
        return other.joinToString("\n")
    }

    data class CodeStats(
        val totalLines: Int, val codeLines: Int, val commentLines: Int, val blankLines: Int,
        val functions: Int, val classes: Int, val imports: Int,
        val longestLine: Int, val avgLineLength: Double
    )

    fun analyze(code: String, language: Language): CodeStats {
        val lines = code.lines()
        var comment = 0; var blank = 0; var code_ = 0; var inBlock = false
        lines.forEach { line ->
            val t = line.trim()
            when {
                t.isEmpty()          -> blank++
                inBlock              -> { comment++; if (t.contains("*/")) inBlock = false }
                t.startsWith("/*")   -> { comment++; if (!t.contains("*/")) inBlock = true }
                t.startsWith("//") || t.startsWith("*") -> comment++
                else                 -> code_++
            }
        }
        return CodeStats(
            totalLines    = lines.size, codeLines = code_, commentLines = comment, blankLines = blank,
            functions     = Regex("""fun\s+\w+\s*[(<]""").findAll(code).count(),
            classes       = Regex("""(class|object|interface)\s+\w+""").findAll(code).count(),
            imports       = lines.count { it.trimStart().startsWith("import ") },
            longestLine   = lines.maxOfOrNull { it.length } ?: 0,
            avgLineLength = if (lines.isEmpty()) 0.0 else lines.sumOf { it.length }.toDouble() / lines.size
        )
    }

    // ── TODO scanner ─────────────────────────────────────────────────────────
    fun findTodos(root: File): List<TodoItem> {
        val items = mutableListOf<TodoItem>()
        val tagRe = Regex("""(TODO|FIXME|HACK|NOTE|BUG|WARN)[:\s](.+)""", RegexOption.IGNORE_CASE)
        root.walkTopDown()
            .filter { it.isFile && !it.path.contains("/build/") && !it.path.contains("/.gradle/") }
            .filter { it.extension in listOf("kt","java","xml","kts") }
            .forEach { file ->
                try {
                    file.useLines { lines ->
                        lines.forEachIndexed { idx, line ->
                            tagRe.find(line)?.let { match ->
                                val tag = TodoTag.entries.firstOrNull {
                                    it.label.equals(match.groupValues[1], ignoreCase = true)
                                } ?: TodoTag.TODO
                                items += TodoItem(file, idx + 1, match.groupValues[2].trim(), tag)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        return items
    }
}
