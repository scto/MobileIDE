package com.scto.mobile.ide.commands.global





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.components.codeSearchDialog
import com.scto.mobile.ide.icons.Icon











class SearchCodeCommand : GlobalCommand() {
    override val id: String = "global.search_code"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.search_code.getString()

    override fun action(context: ActionContext) {
        codeSearchDialog = true
    }

    override fun isEnabled(): Boolean {
        return commandContext.drawerViewModel.currentDrawerTab != null
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.search)

    override val defaultKeybinds: KeyCombination =
        KeyCombination(keyCode = KeyEvent.KEYCODE_F, ctrl = true, shift = true)
}
