package com.scto.mobile.ide.commands.global





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch











class SaveAllCommand : GlobalCommand() {
    override val id: String = "global.save_all"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.save_all.getString()

    override fun action(context: ActionContext) {
        commandContext.mainViewModel.editorTabs.forEach {
            DefaultScope.launch(Dispatchers.IO) { it.save() }
        }
    }

    override fun isEnabled(): Boolean {
        return commandContext.mainViewModel.editorTabs.any { it.editorState.isDirty } || Settings.auto_save
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.save)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_S, ctrl = true, alt = true)
}
