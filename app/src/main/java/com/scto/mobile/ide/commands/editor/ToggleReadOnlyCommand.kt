package com.scto.mobile.ide.commands.editor
import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToggleReadOnlyCommand : EditorCommand() {
    override val id: String = "editor.togglereadonly"
    override val title: String = "togglereadonly"
    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context.androidContext, "ToggleReadOnlyCommand not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }
}
