package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import com.scto.mobile.ide.commands.EditorCommand

class UpperCaseCommand : EditorCommand() {
    override val id: String = "editor.uppercase"

    override val title: String = "transform_uppercase"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        val editor = context.editor
        if (editor.isTextSelected) {
            val selectionStart = editor.cursorRange.startIndex
            val selectionEnd = editor.cursorRange.endIndex
            val selectionText = editor.text.substring(selectionStart, selectionEnd)
            editor.text.replace(selectionStart, selectionEnd, selectionText.uppercase())
        }
    }

    fun isEnabled(context: EditorNonActionContext): Boolean {
        return tab.editorState.editable
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.letters)
}
