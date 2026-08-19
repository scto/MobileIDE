package com.scto.mobile.ide.commands.editor





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon











class SelectAllCommand : EditorCommand() {
    override val id: String = "editor.select_all"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.select_all.getString()

    override fun action(context: EditorActionContext) {
        context.editor.selectAll()
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.select_all)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_A, ctrl = true)
}
