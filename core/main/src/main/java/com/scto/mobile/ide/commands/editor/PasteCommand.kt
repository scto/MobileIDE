package com.scto.mobile.ide.commands.editor





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.EditorNonActionContext
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon











class PasteCommand : EditorCommand() {
    override val id: String = "editor.paste"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.paste.getString()

    override fun action(context: EditorActionContext) {
        context.editor.pasteText()
    }

    override fun isEnabled(context: EditorNonActionContext): Boolean {
        return context.editorTab.editorState.editable
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.paste)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_V, ctrl = true)
}
