package com.scto.mobile.ide.commands.global





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon











class CommandPaletteCommand : GlobalCommand() {
    override val id: String = "global.command_palette"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.command_palette.getString()

    override fun action(context: ActionContext) {
        commandContext.mainViewModel.showCommandPalette()
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.command_palette)

    override val defaultKeybinds: KeyCombination =
        KeyCombination(keyCode = KeyEvent.KEYCODE_P, ctrl = true, shift = true)
}
