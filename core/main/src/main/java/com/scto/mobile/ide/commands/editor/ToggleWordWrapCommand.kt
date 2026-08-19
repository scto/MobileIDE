package com.scto.mobile.ide.commands.editor





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon











class ToggleWordWrapCommand : EditorCommand() {
    override val id: String = "editor.toggle_word_wrap"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.toggle_word_wrap.getString()

    override fun action(context: EditorActionContext) {
        val editor = context.editor
        editor.setWordwrap(!editor.isWordwrap, true, true)
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.edit_note)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_Z, alt = true)
}
