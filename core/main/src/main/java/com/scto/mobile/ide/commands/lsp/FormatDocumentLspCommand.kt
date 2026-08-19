package com.scto.mobile.ide.commands.lsp





import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.LspActionContext
import com.scto.mobile.ide.commands.LspCommand
import com.scto.mobile.ide.commands.LspNonActionContext
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.lsp.formatDocumentSuspend
import kotlinx.coroutines.launch











class FormatDocumentLspCommand : LspCommand() {
    override val id: String = "lsp.format_document"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.format_document_lsp.getString()

    override fun action(context: LspActionContext) {
        context.editorTab.scope.launch {
            formatDocumentSuspend(context.editorTab)
        }
    }

    override fun isEnabled(context: LspNonActionContext): Boolean {
        return context.editorTab.editorState.editable
    }

    override fun isSupported(context: LspNonActionContext): Boolean {
        return context.lspConnector.isFormattingSupported()
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.auto_fix)
}
