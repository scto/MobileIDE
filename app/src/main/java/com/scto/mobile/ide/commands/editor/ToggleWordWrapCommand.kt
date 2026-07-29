package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination

class ToggleWordWrapCommand : EditorCommand() {
    override val id: String = "editor.toggle_word_wrap"

    override val title: String = "toggle_word_wrap"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        val editor = context.editor
        editor.setWordwrap(!editor.isWordwrap, true, true)
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.edit_note)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_Z, alt = true)
}
