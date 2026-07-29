package com.scto.mobile.ide.commands.global

import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.components.addDialog

class NewFileCommand : GlobalCommand() {
    override val id: String = "global.new_file"

    override val title: String = "new_file"

    override suspend fun execute(context: CommandContext) {
        addDialog = true
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.add)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_N, ctrl = true)
}
