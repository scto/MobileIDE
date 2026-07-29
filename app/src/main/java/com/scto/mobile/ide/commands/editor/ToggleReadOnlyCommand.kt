package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination

class ToggleReadOnlyCommand : EditorCommand() {
    override val id: String = "editor.editable"

    override fun getLabel(): String {
        val editorTab = commandContext.mainViewModel.currentTab as? EditorTab
        return if (editorTab?.editorState?.editable == true) {
            strings.read_mode.getString()
        } else {
            strings.edit_mode.getString()
        }
    }

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        val editorState = tab.editorState
        tab.removeNotice("binary_file")
        editorState.editable = !editorState.editable
    }

    fun isEnabled(context: EditorNonActionContext): Boolean {
        return !tab.isReadOnly
    }

    override fun getIcon(): Icon {
        val editorTab = commandContext.mainViewModel.currentTab as? EditorTab
        return if (editorTab?.editorState?.editable == true) {
            Icon.ResourceIcon(drawables.lock)
        } else {
            Icon.ResourceIcon(drawables.edit)
        }
    }

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_E, ctrl = true)
}
