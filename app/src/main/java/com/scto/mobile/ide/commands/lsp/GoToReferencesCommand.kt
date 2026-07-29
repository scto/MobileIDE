package com.scto.mobile.ide.commands.lsp

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.lsp.editor.LspEditor
import com.scto.mobile.ide.commands.*
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.rosemoe.sora.lsp.editor.requestReferencesAt

class GoToReferencesCommand : LspCommand() {
    override val id: String = "lsp.go_to_references"
    override val title: String = "go_to_references"

    override suspend fun executeLspCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor, lspEditor: LspEditor) {
        try {
            val refs = lspEditor.requestReferencesAt(editor.cursor.leftLine, editor.cursor.leftColumn)
            withContext(Dispatchers.Main) {
                val msg = if (refs.isEmpty()) "No references found" else "Found ${refs.size} references (UI not implemented)"
                Toast.makeText(context.androidContext, msg, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
