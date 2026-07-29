package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination

class SelectAllCommand : EditorCommand() {
    override val id: String = "editor.select_all"

    override val title: String = "select_all"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        editor.selectAll()
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.select_all)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_A, ctrl = true)
}
