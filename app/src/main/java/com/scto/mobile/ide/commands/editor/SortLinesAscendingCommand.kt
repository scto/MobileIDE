package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import com.scto.mobile.ide.commands.EditorCommand

class SortLinesAscendingCommand : EditorCommand() {
    override val id = "editor.sort_lines_ascending"

    override fun getLabel() = strings.sort_lines_ascending.getString()

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        val editor = context.editor

        val cursor = editor.cursor

        var startLine: Int
        var endLine: Int
        if (!cursor.isSelected) {
            startLine = 0
            endLine = editor.text.lineCount - 1
        } else {
            startLine = minOf(cursor.leftLine, cursor.rightLine)
            endLine = maxOf(cursor.leftLine, cursor.rightLine)
        }
        val endLineColumn = editor.text.getColumnCount(endLine)

        val lines = editor.text.subContent(startLine, 0, endLine, endLineColumn).lines()
        val ascendingLines = lines.sorted().joinToString("\n")

        editor.text.replace(startLine, 0, endLine, endLineColumn, ascendingLines)
    }

    fun isEnabled(context: EditorNonActionContext): Boolean {
        return tab.editorState.editable
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.sort_by_alphabet)
}
