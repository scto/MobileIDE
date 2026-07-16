package com.scto.mobile.ide.commands.global

import android.content.Intent
import android.view.KeyEvent
import com.scto.mobile.ide.activities.terminal.Terminal
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination
// import com.scto.mobile.ide.feature.FeatureRegistry // REMOVED
import com.scto.mobile.ide.core.common.icons.Icon
import com.scto.mobile.ide.core.terminal.resources.drawables
import com.scto.mobile.ide.core.terminal.resources.getString
import com.scto.mobile.ide.core.terminal.resources.strings

class TerminalCommand : GlobalCommand() {
    override val id: String = "global.terminal"

    override fun getLabel(): String = strings.terminal.getString()

    override fun action(actionContext: ActionContext) {
        val activity = actionContext.currentActivity
        val intent = Intent(activity, Terminal::class.java)
        activity.startActivity(intent)
    }

    override fun isSupported(): Boolean = FeatureRegistry.isEnabled("feature_terminal")

    override fun getIcon(): Icon = Icon.ResourceIcon(drawables.terminal)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_J, ctrl = true)
}
