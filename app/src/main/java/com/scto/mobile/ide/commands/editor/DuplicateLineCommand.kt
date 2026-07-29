package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination

class DuplicateLineCommand : EditorCommand() {
    override val id: String = "editor.duplicate_line"

    override val title: String = "duplicate_line"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        editor.duplicateLine()
    }

    fun isEnabled(context: EditorNonActionContext): Boolean {
        return tab.editorState.editable
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.duplicate_line)

    }
