package com.scto.mobile.ide.commands.global

import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.components.codeSearchDialog

class SearchCodeCommand : GlobalCommand() {
    override val id: String = "global.search_code"

    override val title: String = "search_code"

    override suspend fun execute(context: CommandContext) {
        codeSearchDialog = true
    }

    fun isEnabled(): Boolean {
        return commandContext.drawerViewModel.currentDrawerTab != null
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.search)

    override val defaultKeybinds: KeyCombination =
        KeyCombination(keyCode = KeyEvent.KEYCODE_F, ctrl = true, shift = true)
}
