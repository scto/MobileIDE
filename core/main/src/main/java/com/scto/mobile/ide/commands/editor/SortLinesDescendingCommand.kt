package com.scto.mobile.ide.commands.editor





import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.EditorNonActionContext
import com.scto.mobile.ide.icons.Icon











class SortLinesDescendingCommand : EditorCommand() {
    override val id = "editor.sort_lines_descending"

    override fun getLabel() = com.scto.mobile.ide.core.main.R.string.sort_lines_descending.getString()

    override fun action(context: EditorActionContext) {
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
        val descendingLine = lines.sortedDescending().joinToString("\n")

        editor.text.replace(startLine, 0, endLine, endLineColumn, descendingLine)
    }

    override fun isEnabled(context: EditorNonActionContext): Boolean {
        return context.editorTab.editorState.editable
    }

    override fun getIcon() = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.sort_by_alphabet)
}
