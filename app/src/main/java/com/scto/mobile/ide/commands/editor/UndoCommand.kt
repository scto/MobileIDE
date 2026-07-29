package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination

class UndoCommand : EditorCommand() {
    override val id: String = "editor.undo"

    override val repeatOnHold: Boolean = true

    override val title: String = "undo"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        val editor = context.editor
        if (editor.canUndo()) editor.undo()
        tab.editorState.updateUndoRedo()
    }

    fun isEnabled(context: EditorNonActionContext): Boolean {
        val editorState = tab.editorState
        return editorState.editable && editorState.canUndo
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.undo)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_Z, ctrl = true)
}
