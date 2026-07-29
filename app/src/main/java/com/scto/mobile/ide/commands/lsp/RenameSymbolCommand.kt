package com.scto.mobile.ide.commands.lsp

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.lsp.editor.LspEditor
import com.scto.mobile.ide.commands.*
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RenameSymbolCommand : LspCommand() {
    override val id: String = "lsp.rename_symbol"
    override val title: String = "rename_symbol"

    override suspend fun executeLspCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor, lspEditor: LspEditor) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context.androidContext, "Rename Symbol UI not implemented", Toast.LENGTH_SHORT).show()
        }
    }
}
