package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination

class JumpToLineCommand : EditorCommand() {
    override val id: String = "editor.jump_to_line"

    override val title: String = "jump_to_line"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        tab.editorState.apply {
            showJumpToLineDialog = true
            jumpToLineValue = "${editor.cursor.leftLine}:${editor.cursor.leftColumn}"
        }
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.arrow_outward)

    }
