package com.scto.mobile.ide.commands.global





import android.content.Intent
import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.activities.settings.SettingsActivity
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon











class SettingsCommand : GlobalCommand() {
    override val id: String = "global.settings"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.settings.getString()

    override fun action(context: ActionContext) {
        val activity = context.currentActivity
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.settings)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_COMMA, ctrl = true)
}
