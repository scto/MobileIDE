package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination

class CutCommand : EditorCommand() {
    override val id: String = "editor.cut"

    override val title: String = "cut"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        editor.cutText()
    }

    fun isEnabled(context: EditorNonActionContext): Boolean {
        return tab.editorState.editable
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.cut)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_X, ctrl = true)
}
