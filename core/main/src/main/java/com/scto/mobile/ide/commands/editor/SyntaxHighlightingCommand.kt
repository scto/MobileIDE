package com.scto.mobile.ide.commands.editor





import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.commands.Command
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.file.FileTypeManager
import com.scto.mobile.ide.icons.Icon











class SyntaxHighlightingCommand : EditorCommand() {
    override val id: String = "editor.syntax_highlighting"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.highlighting.getString()

    override fun action(context: EditorActionContext) {}

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.edit_note)

    override val childCommands: List<Command> by lazy {
        FileTypeManager.allTypes()
            .filter { it.textmateScope != null }
            .map { fileType ->
                object : EditorCommand() {
                    override val id: String = "editor.syntax_highlighting.${fileType.name.lowercase()}"

                    override fun getLabel(): String = fileType.title

                    override fun action(context: EditorActionContext) {
                        context.editorTab.editorState.textmateScope = fileType.textmateScope!!
                    }

                    override fun getIcon(): Icon = fileType.getResolvedIcon()
                }
            }
    }

    override fun getChildSearchPlaceholder(): String = com.scto.mobile.ide.core.main.R.string.select_language.getString()
}
