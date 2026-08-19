package com.scto.mobile.ide.commands.global





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.components.addDialog
import com.scto.mobile.ide.icons.Icon











class NewFileCommand : GlobalCommand() {
    override val id: String = "global.new_file"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.new_file.getString()

    override fun action(context: ActionContext) {
        addDialog = true
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.add)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_N, ctrl = true)
}
