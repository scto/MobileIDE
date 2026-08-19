package com.scto.mobile.ide.commands.lsp





import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.commands.LspActionContext
import com.scto.mobile.ide.commands.LspCommand
import com.scto.mobile.ide.commands.LspNonActionContext
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.lsp.formatDocumentRange











class FormatSelectionCommand : LspCommand() {
    override val id: String = "lsp.format_selection"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.format_selection.getString()

    override fun action(context: LspActionContext) {
        formatDocumentRange(DefaultScope, context.editorTab)
    }

    override fun isEnabled(context: LspNonActionContext): Boolean {
        return context.editorTab.editorState.editable
    }

    override fun isSupported(context: LspNonActionContext): Boolean {
        return context.lspConnector.isRangeFormattingSupported()
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.auto_fix)
}
