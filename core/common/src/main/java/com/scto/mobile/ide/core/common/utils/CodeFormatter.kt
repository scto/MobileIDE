/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.scto.mobile.ide.core.common.utils

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

object CodeFormatter {

    // Added indentSize parameter, defaults to 2 based on signature (or 4 depending on preference)
    fun format(code: String, extension: String, indentSize: Int = 2): String {
        if (code.isBlank()) return ""

        return try {
            when (extension.lowercase()) {
                "html",
                "htm" -> formatHtml(code, indentSize)
                "json" -> formatJson(code, indentSize)
                "css" -> formatCss(code, indentSize)
                // Use a generic C-Style formatter for JS, Java, Kotlin, and Gradle
                "js",
                "java",
                "kt",
                "kts",
                "gradle" -> formatCStyle(code, indentSize)
                // Specific formatters for whitespace-sensitive or structured text
                "md",
                "markdown" -> formatMarkdown(code)
                "yaml",
                "yml",
                "toml" -> formatConfig(code)
                else -> code
            }
        } catch (e: Exception) {
            e.printStackTrace()
            code
        }
    }

    private fun formatHtml(code: String, indentSize: Int): String {
        val doc = Jsoup.parse(code)
        doc.outputSettings().indentAmount(indentSize).prettyPrint(true)
        return doc.html()
    }

    private fun formatJson(code: String, indentSize: Int): String {
        val trimmed = code.trim()
        return if (trimmed.startsWith("[")) {
            JSONArray(trimmed).toString(indentSize)
        } else {
            JSONObject(trimmed).toString(indentSize)
        }
    }

    private fun formatCss(code: String, indentSize: Int): String {
        // Simple CSS processing, temporarily skipping dynamic indentation calculations
        // Build indent string
        val indent = " ".repeat(indentSize)
        return code
            .replace(Regex("\\s*\\{\\s*"), " {\n$indent")
            .replace(Regex("\\s*;\\s*"), ";\n$indent")
            .replace(Regex("\\s*\\}\\s*"), "\n}\n")
            .replace(Regex("(?m)^\\s+$"), "")
            .replace(Regex("\\n\\s*\\n"), "\n")
            .trim()
    }

    /**
     * A generic lightweight formatter for C-style languages (JS, Java, Kotlin, Groovy/Gradle). It properly tracks {},
     * [], and () to calculate indentation, ignoring contents inside strings.
     */
    private fun formatCStyle(code: String, indentSize: Int): String {
        val lines = code.split('\n')
        val result = StringBuilder()
        var indentLevel = 0
        val indentStr = " ".repeat(indentSize)

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                result.append("\n")
                continue
            }

            // Decrease indent if the line starts with a closing brace, bracket, or parenthesis
            if (trimmed.startsWith("}") || trimmed.startsWith("]") || trimmed.startsWith(")")) {
                indentLevel = maxOf(0, indentLevel - 1)
            }

            // Apply indentation
            repeat(indentLevel) { result.append(indentStr) }
            result.append(trimmed).append("\n")

            // Calculate indentation level for the *next* line based on characters in the *current* line
            var opens = 0
            var closes = 0
            var inString = false
            var inChar = false
            var escapeNext = false

            for (char in trimmed) {
                if (escapeNext) {
                    escapeNext = false
                    continue
                }
                when (char) {
                    '\\' -> escapeNext = true
                    '"' -> if (!inChar) inString = !inString
                    '\'' -> if (!inString) inChar = !inChar
                    '{',
                    '[',
                    '(' -> if (!inString && !inChar) opens++
                    '}',
                    ']',
                    ')' -> if (!inString && !inChar) closes++
                }
            }

            // Do not double-count the first closing character since we already adjusted indentLevel for it
            if (trimmed.startsWith("}") || trimmed.startsWith("]") || trimmed.startsWith(")")) {
                closes--
            }

            indentLevel = maxOf(0, indentLevel + opens - closes)
        }
        return result.toString().trim()
    }

    /**
     * Formats Markdown by trimming trailing whitespaces and standardizing line breaks. Markdown relies heavily on
     * specific whitespaces (e.g., 2 spaces for line break), so we only clean up trailing empty spaces at the very end
     * of lines.
     */
    private fun formatMarkdown(code: String): String {
        return code.lines().joinToString("\n") { it.trimEnd() }.trim() + "\n"
    }

    /**
     * Formats YAML and TOML. YAML is extremely sensitive to indentation, so modifying leading spaces without a full AST
     * is dangerous. This function performs a safe cleanup (removing trailing spaces and fixing line endings).
     */
    private fun formatConfig(code: String): String {
        return code.lines().joinToString("\n") { it.trimEnd() }.trim() + "\n"
    }
}
