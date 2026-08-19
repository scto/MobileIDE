package com.scto.mobile.ide.commands.editor





import android.view.KeyEvent
import androidx.compose.ui.text.TextRange
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon











class SearchCommand : EditorCommand() {
    override val id: String = "editor.search"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.search.getString()

    override fun action(context: EditorActionContext) {
        context.editorTab.editorState.apply {
            context.editor.getSelectedText()?.let {
                searchKeyword = searchKeyword.copy(text = it, selection = TextRange(it.length))
            }
            isSearching = true
            isReplaceShown = false
        }
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.search)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_F, ctrl = true)
}
