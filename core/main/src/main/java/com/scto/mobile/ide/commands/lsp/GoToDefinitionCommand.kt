package com.scto.mobile.ide.commands.lsp





import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.commands.LspActionContext
import com.scto.mobile.ide.commands.LspCommand
import com.scto.mobile.ide.commands.LspNonActionContext
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.lsp.goToDefinition











class GoToDefinitionCommand : LspCommand() {
    override val id: String = "lsp.go_to_definition"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.go_to_definition.getString()

    override fun action(context: LspActionContext) {
        goToDefinition(
            scope = DefaultScope,
            context = context.currentActivity,
            viewModel = commandContext.mainViewModel,
            editorTab = context.editorTab,
        )
    }

    override fun isSupported(context: LspNonActionContext): Boolean {
        return context.lspConnector.isGoToDefinitionSupported()
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.jump_to_element)
}
