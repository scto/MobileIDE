package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination

class SaveAsCommand : EditorCommand() {
    override val id: String = "editor.save_as"

    override val title: String = "save_as"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        tab.saveAs()
    }

    fun isEnabled(context: EditorNonActionContext): Boolean {
        return !tab.isReadOnly
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.save)

            KeyCombination(keyCode = KeyEvent.KEYCODE_S, ctrl = true, shift = true)
}
