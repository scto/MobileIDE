package com.scto.mobile.ide.commands.editor





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.EditorNonActionContext
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.tabs.editor.EditorTab











class ToggleReadOnlyCommand : EditorCommand() {
    override val id: String = "editor.editable"

    override fun getLabel(): String {
        val editorTab = commandContext.mainViewModel.currentTab as? EditorTab
        return if (editorTab?.editorState?.editable == true) {
            com.scto.mobile.ide.core.main.R.string.read_mode.getString()
        } else {
            com.scto.mobile.ide.core.main.R.string.edit_mode.getString()
        }
    }

    override fun action(context: EditorActionContext) {
        val editorState = context.editorTab.editorState
        context.editorTab.removeNotice("binary_file")
        editorState.editable = !editorState.editable
    }

    override fun isEnabled(context: EditorNonActionContext): Boolean {
        return !context.editorTab.isReadOnly
    }

    override fun getIcon(): Icon {
        val editorTab = commandContext.mainViewModel.currentTab as? EditorTab
        return if (editorTab?.editorState?.editable == true) {
            Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.lock)
        } else {
            Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.edit)
        }
    }

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_E, ctrl = true)
}
