package com.scto.mobile.ide.commands.global





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.icons.Menu_book
import com.scto.mobile.ide.icons.XedIcons
import com.scto.mobile.ide.utils.openUrl











class DocumentationCommand : GlobalCommand() {
    override val id: String = "global.documentation"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.docs.getString()

    override fun getIcon(): Icon = Icon.VectorIcon(XedIcons.Menu_book)

    override fun action(context: ActionContext) {
        val url = "https://xed-editor.github.io/Xed-Docs/"
        context.currentActivity.openUrl(url)
    }

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_F1)
}
