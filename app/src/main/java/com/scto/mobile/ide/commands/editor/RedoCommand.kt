package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination

class RedoCommand : EditorCommand() {
    override val id: String = "editor.redo"

    override val repeatOnHold: Boolean = true

    override val title: String = "redo"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        val editor = context.editor
        if (editor.canRedo()) editor.redo()
        tab.editorState.updateUndoRedo()
    }

    fun isEnabled(context: EditorNonActionContext): Boolean {
        val editorState = tab.editorState
        return editorState.editable && editorState.canRedo
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.redo)

    }
