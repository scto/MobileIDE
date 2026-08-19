package com.scto.mobile.ide.commands.editor





import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.EditorNonActionContext
import com.scto.mobile.ide.icons.Icon











class LowerCaseCommand : EditorCommand() {
    override val id: String = "editor.lowercase"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.transform_lowercase.getString()

    override fun action(context: EditorActionContext) {
        val editor = context.editor
        if (editor.isTextSelected) {
            val selectionStart = editor.cursorRange.startIndex
            val selectionEnd = editor.cursorRange.endIndex
            val selectionText = editor.text.substring(selectionStart, selectionEnd)
            editor.text.replace(selectionStart, selectionEnd, selectionText.lowercase())
        }
    }

    override fun isEnabled(context: EditorNonActionContext): Boolean {
        return context.editorTab.editorState.editable
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.letters)
}
