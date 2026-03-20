package com.mobileide.app.editor.intelligent

import android.view.KeyEvent
import com.mobileide.app.editor.Editor
import io.github.rosemoe.sora.event.EditorKeyEvent

/**
 * Markdown intelligent-continuation feature.
 *
 * **Enter** behaviour:
 * - Inside a `> ` blockquote → inserts a new `> ` line (or removes the
 *   blockquote marker if the current line is an empty quote).
 * - Inside an unordered list (`- `, `* `, `+ `, optionally with `[ ]`/`[x]`
 *   checkboxes) → inserts a new list item (resets checkbox to `[ ]`).
 * - Inside an ordered list (`1. `, `2) `, …) → inserts the next numbered
 *   item and adjusts trailing space to keep text aligned.
 * - On an empty list item (marker only, no text) → removes the marker.
 *
 * **Tab / Shift+Tab** behaviour:
 * - While the cursor is on a list-item line → indent / un-indent.
 *
 * Enabled/disabled by [isEnabled] (currently always true; wire to a
 * `SharedPreferences` / `DataStore` flag when a settings key is added).
 */
object BulletContinuation : IntelligentFeature() {

    override val id: String = "md.bullet_continuation"
    override val supportedExtensions: List<String> = listOf("md", "markdown")

    // ── Regexes ──────────────────────────────────────────────────────────────

    private val QUOTE_REGEX            = Regex("^> ")
    private val LIST_WHITESPACE_REGEX  = Regex("^\\s*([-+*]|[0-9]+[.)]) +(\\[[ x]] +)?")
    private val LIST_EMPTY_REGEX       = Regex("^([-+*]|[0-9]+[.)])( +\\[[ x]])?$")
    private val UL_LIST_REGEX          = Regex("^((\\s*[-+*] +)(\\[[ x]] +)?)")
    private val OL_LIST_REGEX          = Regex("^(\\s*)([0-9]+)([.)])( +)((\\[[ x]] +)?)")

    // ── Event handling ────────────────────────────────────────────────────────

    override fun handleKeyEvent(event: EditorKeyEvent, editor: Editor) {
        if (event.action != KeyEvent.ACTION_DOWN) return

        when {
            event.keyCode == KeyEvent.KEYCODE_ENTER && event.modifiers == 0 ->
                onEnter(editor) { event.intercept() }

            event.keyCode == KeyEvent.KEYCODE_TAB
                && !event.isCtrlPressed && !event.isAltPressed ->
                onTab(editor, event.isShiftPressed) { event.intercept() }
        }
    }

    // ── Enter ─────────────────────────────────────────────────────────────────

    private fun onEnter(editor: Editor, consume: () -> Unit) {
        if (editor.isTextSelected) return

        val lineIdx  = editor.cursor.leftLine
        val colIdx   = editor.cursor.leftColumn
        val line     = editor.text.getLine(lineIdx).toString()
        val toCursor = line.take(colIdx)

        // Blockquote
        QUOTE_REGEX.find(line)?.let {
            if (line.trim() == ">") {
                editor.text.delete(lineIdx, 0, lineIdx, line.length)
            } else {
                editor.text.insert(lineIdx, colIdx, "\n> ")
            }
            consume(); return
        }

        // Empty list item → remove marker
        if (LIST_EMPTY_REGEX.matchEntire(line.trim()) != null) {
            editor.text.delete(lineIdx, 0, lineIdx, line.length)
            consume(); return
        }

        // Unordered list
        UL_LIST_REGEX.find(toCursor)?.let { m ->
            val prefix = m.groupValues[1].replace("[x]", "[ ]")
            editor.text.insert(lineIdx, colIdx, "\n$prefix")
            consume(); return
        }

        // Ordered list
        OL_LIST_REGEX.find(toCursor)?.let { m ->
            val leading  = m.groupValues[1]
            val prev     = m.groupValues[2]
            val delim    = m.groupValues[3]
            val trailing = m.groupValues[4]
            val checkbox = m.groupValues[5].replace("[x]", "[ ]")
            val next     = (prev.toInt() + 1).toString()
            val spaceDiff = prev.length - next.length
            val spaces   = " ".repeat((trailing.length + spaceDiff).coerceAtLeast(1))
            editor.text.insert(lineIdx, colIdx, "\n$leading$next$delim$spaces$checkbox")
            consume(); return
        }
    }

    // ── Tab ───────────────────────────────────────────────────────────────────

    private fun onTab(editor: Editor, shift: Boolean, consume: () -> Unit) {
        if (editor.cursor.leftLine != editor.cursor.rightLine) return

        val line     = editor.text.getLine(editor.cursor.leftLine).toString()
        val toCursor = line.take(editor.cursor.leftColumn)

        LIST_WHITESPACE_REGEX.find(line)?.let { m ->
            if (toCursor.endsWith(m.value) || editor.isTextSelected) {
                if (!shift) editor.indentLines(false) else editor.unindentSelection()
                consume()
            }
        }
    }

    override fun isEnabled(): Boolean = true
}
