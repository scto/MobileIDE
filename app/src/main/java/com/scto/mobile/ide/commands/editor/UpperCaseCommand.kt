package com.scto.mobile.ide.commands.editor
import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpperCaseCommand : EditorCommand() {
    override val id: String = "editor.uppercase"
    override val title: String = "uppercase"
    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context.androidContext, "UpperCaseCommand not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }
}
