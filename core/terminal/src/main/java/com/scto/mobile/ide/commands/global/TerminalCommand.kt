package com.scto.mobile.ide.commands.global

import android.content.Intent
import com.scto.mobile.ide.activities.terminal.Terminal
import com.rk.commands.BaseCommand
import com.rk.commands.CommandContext
import com.scto.mobile.ide.core.common.icons.Icon
import com.scto.mobile.ide.core.terminal.resources.drawables
import com.scto.mobile.ide.core.terminal.resources.getString
import com.scto.mobile.ide.core.terminal.resources.strings

class TerminalCommand : BaseCommand() {
    override val id: String = "global.terminal"

    override val title: String
        get() = strings.terminal.getString()

    override val description: String
        get() = strings.terminal.getString()

    override val icon: Icon
        get() = Icon.ResourceIcon(drawables.terminal)

    override suspend fun execute(context: CommandContext) {
        val intent = Intent(com.scto.mobile.ide.utils.application!!, Terminal::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        com.scto.mobile.ide.utils.application!!.startActivity(intent)
    }
}
