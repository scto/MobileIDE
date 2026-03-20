package com.mobileide.app.editor.intelligent

import com.mobileide.app.editor.Editor

/**
 * Automatically inserts a closing tag when `>` is typed inside an HTML/HTMX
 * file, and normalises self-closing tags when `/` is typed.
 *
 * Examples:
 * - Typing `>` after `<div ` → inserts `</div>` and moves cursor inside.
 * - Typing `/` after `<br ` → inserts `>` to complete `<br />`.
 * - Self-closing tags (`<br>`, `<input>`, …) are left untouched.
 * - Does not trigger inside attribute strings.
 */
object AutoCloseTag : IntelligentFeature() {

    override val id: String = "html.auto_close_tag"
    override val supportedExtensions: List<String> = listOf("html", "htm", "xhtml", "htmx", "php")
    override val triggerCharacters: List<Char> = listOf('>', '/')

    private val OPEN_TAG_REGEX = Regex(
        "<([_a-zA-Z][a-zA-Z0-9:\\-_.]*)(?:\\s+[^<>]*?[^\\s/<>=]+?)*?\\s?(/|>)$"
    )

    private val VOID_ELEMENTS = setOf(
        "area", "base", "br", "col", "command", "embed",
        "hr", "img", "input", "keygen", "link", "meta",
        "param", "source", "track", "wbr"
    )

    override fun handleInsertChar(triggerCharacter: Char, editor: Editor) {
        if (editor.cursor.isSelected) return

        val lineIdx  = editor.cursor.leftLine
        val colIdx   = editor.cursor.leftColumn
        val line     = editor.text.getLine(lineIdx).toString()
        val toCursor = line.take(colIdx)

        val match    = OPEN_TAG_REGEX.find(toCursor) ?: return
        val tagName  = match.groupValues[1].lowercase()
        val endChar  = match.groupValues[2]

        // Skip if cursor is inside an unclosed string
        if (!hasEvenQuotes(toCursor)) return

        when (endChar) {
            ">" -> {
                if (tagName in VOID_ELEMENTS) return
                // Insert closing tag; cursor stays between open and close tag
                editor.text.insert(lineIdx, colIdx, "</$tagName>")
                editor.setSelection(lineIdx, colIdx)
            }
            "/" -> {
                // Normalise: add a space before the slash if missing, then close
                if (colIdx >= line.length) return
                if (colIdx >= 2 && toCursor[colIdx - 2] != ' ') {
                    editor.text.insert(lineIdx, colIdx - 1, " ")
                }
                editor.text.insert(editor.cursor.leftLine, editor.cursor.leftColumn, ">")
            }
        }
    }

    /** Returns true when all quote-like characters in [s] appear in matched pairs. */
    private fun hasEvenQuotes(s: String): Boolean =
        s.count { it == '\'' } % 2 == 0 &&
        s.count { it == '"'  } % 2 == 0 &&
        s.count { it == '`'  } % 2 == 0

    override fun isEnabled(): Boolean = true
}
