package com.scto.mobile.ide.commands.global

import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SaveAllCommand : GlobalCommand() {
    override val id: String = "global.save_all"

    override val title: String = "save_all"

    override suspend fun execute(context: CommandContext) {
        commandContext.mainViewModel.editorTabs.forEach {
            DefaultScope.launch(Dispatchers.IO) { it.save() }
        }
    }

    fun isEnabled(): Boolean {
        return commandContext.mainViewModel.editorTabs.any { it.editorState.isDirty } || Settings.auto_save
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.save)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_S, ctrl = true, alt = true)
}
