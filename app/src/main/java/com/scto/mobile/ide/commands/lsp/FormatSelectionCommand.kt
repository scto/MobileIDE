package com.scto.mobile.ide.commands.lsp

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.lsp.editor.LspEditor
import com.scto.mobile.ide.commands.*

class FormatSelectionCommand : LspCommand() {
    override val id: String = "lsp.format_selection"
    override val title: String = "format_selection"

    override suspend fun executeLspCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor, lspEditor: LspEditor) {
        editor.formatCodeAsync()
    }
}
