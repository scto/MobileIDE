package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SaveCommand : EditorCommand() {
    override val id: String = "editor.save"

    override val title: String = "save"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        DefaultScope.launch(Dispatchers.IO) { tab.save() }
    }

    fun isEnabled(context: EditorNonActionContext): Boolean {
        return !tab.isReadOnly && (tab.editorState.isDirty || Settings.auto_save)
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.save)

    }
