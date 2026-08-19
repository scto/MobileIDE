package com.scto.mobile.ide.commands.editor





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.EditorNonActionContext
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon











class DuplicateLineCommand : EditorCommand() {
    override val id: String = "editor.duplicate_line"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.duplicate_line.getString()

    override fun action(context: EditorActionContext) {
        context.editor.duplicateLine()
    }

    override fun isEnabled(context: EditorNonActionContext): Boolean {
        return context.editorTab.editorState.editable
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.duplicate_line)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_D, ctrl = true)
}
