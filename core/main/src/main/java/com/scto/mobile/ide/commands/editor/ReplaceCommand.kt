package com.scto.mobile.ide.commands.editor





import android.view.KeyEvent
import androidx.compose.ui.text.TextRange
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.EditorNonActionContext
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon











class ReplaceCommand : EditorCommand() {
    override val id: String = "editor.replace"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.replace.getString()

    override fun action(context: EditorActionContext) {
        context.editorTab.editorState.apply {
            context.editor.getSelectedText()?.let {
                searchKeyword = searchKeyword.copy(text = it, selection = TextRange(it.length))
            }
            isSearching = true
            isReplaceShown = true
        }
    }

    override fun isEnabled(context: EditorNonActionContext): Boolean {
        return context.editorTab.editorState.editable
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.find_replace)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_H, ctrl = true)
}
