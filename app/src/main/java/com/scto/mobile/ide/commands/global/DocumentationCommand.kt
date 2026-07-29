package com.scto.mobile.ide.commands.global

import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Menu_book
import com.scto.mobile.ide.icons.XedIcons
import com.scto.mobile.ide.utils.openUrl

class DocumentationCommand : GlobalCommand() {
    override val id: String = "global.documentation"

    override val title: String = "docs"

    override val icon: Any? = null // Icon.VectorIcon(XedIcons.Menu_book)

    override suspend fun execute(context: CommandContext) {
        val url = "https://xed-editor.github.io/Xed-Docs/"
        (context as? MobileIDECommandContext)?.androidContext as? android.app.Activity.openUrl(url)
    }

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_F1)
}
