package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import androidx.compose.ui.text.TextRange
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination

class ReplaceCommand : EditorCommand() {
    override val id: String = "editor.replace"

    override val title: String = "replace"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        tab.editorState.apply {
            editor.getSelectedText()?.let {
                searchKeyword = searchKeyword.copy(text = it, selection = TextRange(it.length))
            }
            isSearching = true
            isReplaceShown = true
        }
    }

    fun isEnabled(context: EditorNonActionContext): Boolean {
        return tab.editorState.editable
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.find_replace)

    }
