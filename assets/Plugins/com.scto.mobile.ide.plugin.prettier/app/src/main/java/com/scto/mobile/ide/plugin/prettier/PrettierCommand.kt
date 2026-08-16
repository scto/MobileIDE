package com.scto.mobile.ide.plugin.prettier

import com.scto.mobile.ide.plugin.prettier.utils.getPrettierIcon
import com.rk.commands.EditorActionContext
import com.rk.commands.EditorCommand
import com.rk.editor.Editor
import com.rk.extension.ExtensionContext
import com.rk.tabs.editor.EditorTab
import io.github.rosemoe.sora.text.TextRange

class PrettierCommand(
    private val context: ExtensionContext,
    private val onFormat: (EditorTab, Editor, TextRange?) -> Unit,
) : EditorCommand() {
    override val id = "editor.prettier"

    override fun getLabel() = "Format with Prettier"

    override fun getIcon() = getPrettierIcon(context)

    override fun action(editorActionContext: EditorActionContext) {
        val range =
            editorActionContext.editor.cursorRange.takeIf {
                editorActionContext.editor.isTextSelected
            }
        onFormat(editorActionContext.editorTab, editorActionContext.editor, range)
    }
}
