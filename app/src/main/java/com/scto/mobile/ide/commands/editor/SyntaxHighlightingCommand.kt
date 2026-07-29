package com.scto.mobile.ide.commands.editor
import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyntaxHighlightingCommand : EditorCommand() {
    override val id: String = "editor.syntaxhighlighting"
    override val title: String = "syntaxhighlighting"
    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context.androidContext, "SyntaxHighlightingCommand not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }
}
