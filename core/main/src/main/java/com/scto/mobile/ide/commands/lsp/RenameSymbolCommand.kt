package com.scto.mobile.ide.commands.lsp





import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.commands.LspActionContext
import com.scto.mobile.ide.commands.LspCommand
import com.scto.mobile.ide.commands.LspNonActionContext
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.lsp.renameSymbol











class RenameSymbolCommand : LspCommand() {
    override val id: String = "lsp.rename_symbol"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.rename_symbol.getString()

    override fun action(context: LspActionContext) {
        renameSymbol(DefaultScope, context.editorTab)
    }

    override fun isSupported(context: LspNonActionContext): Boolean {
        return context.lspConnector.isRenameSymbolSupported()
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.manage_search)
}
