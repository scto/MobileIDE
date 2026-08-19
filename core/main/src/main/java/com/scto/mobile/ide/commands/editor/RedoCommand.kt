package com.scto.mobile.ide.commands.editor





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.EditorNonActionContext
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon











class RedoCommand : EditorCommand() {
    override val id: String = "editor.redo"

    override val repeatOnHold: Boolean = true

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.redo.getString()

    override fun action(context: EditorActionContext) {
        val editor = context.editor
        if (editor.canRedo()) editor.redo()
        context.editorTab.editorState.updateUndoRedo()
    }

    override fun isEnabled(context: EditorNonActionContext): Boolean {
        val editorState = context.editorTab.editorState
        return editorState.editable && editorState.canRedo
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.redo)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_Y, ctrl = true)
}
