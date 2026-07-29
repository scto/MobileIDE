package com.scto.mobile.ide.commands.global

import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination

class CommandPaletteCommand : GlobalCommand() {
    override val id: String = "global.command_palette"

    override val title: String = "command_palette"

    override suspend fun execute(context: CommandContext) {
        commandContext.mainViewModel.showCommandPalette()
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.command_palette)

    override val defaultKeybinds: KeyCombination =
        KeyCombination(keyCode = KeyEvent.KEYCODE_P, ctrl = true, shift = true)
}
