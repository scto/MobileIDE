package com.scto.mobile.ide.commands.global





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.components.fileSearchDialog
import com.scto.mobile.ide.filetree.FileTreeTab
import com.scto.mobile.ide.icons.Icon











class SearchFileFolderCommand : GlobalCommand() {
    override val id: String = "global.search_file_folder"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.search_file_folder.getString()

    override fun action(context: ActionContext) {
        fileSearchDialog = true
    }

    override fun isEnabled(): Boolean {
        return commandContext.drawerViewModel.currentDrawerTab is FileTreeTab
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.search)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_P, ctrl = true)
}
