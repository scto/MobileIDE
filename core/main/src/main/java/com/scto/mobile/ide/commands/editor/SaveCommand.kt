package com.scto.mobile.ide.commands.editor





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.EditorNonActionContext
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch











class SaveCommand : EditorCommand() {
    override val id: String = "editor.save"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.save.getString()

    override fun action(context: EditorActionContext) {
        DefaultScope.launch(Dispatchers.IO) { context.editorTab.save() }
    }

    override fun isEnabled(context: EditorNonActionContext): Boolean {
        return !context.editorTab.isReadOnly && (context.editorTab.editorState.isDirty || Settings.auto_save)
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.save)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_S, ctrl = true)
}
