package com.scto.mobile.ide.commands.editor





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon











class JumpToLineCommand : EditorCommand() {
    override val id: String = "editor.jump_to_line"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.jump_to_line.getString()

    override fun action(context: EditorActionContext) {
        context.editorTab.editorState.apply {
            showJumpToLineDialog = true
            val line = context.editor.cursor.leftLine + 1
            val column = context.editor.cursor.leftColumn + 1
            jumpToLineValue = "$line:$column"
        }
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.arrow_outward)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_G, ctrl = true)
}
