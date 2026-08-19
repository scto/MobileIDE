package com.scto.mobile.ide.commands.lsp





import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.tabs.editor.EditorTab











class FormatDocumentCommand : EditorCommand() {
    override val id: String = "editor.format_document"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.format_document.getString()

    override fun action(context: EditorActionContext) {
        context.editorTab.registerTask(EditorTab.FORMAT_DOCUMENT_TASK_ID)
        context.editor.formatCodeAsync()
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.auto_fix)
}
