package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.view.KeyEvent
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.events.EditorTabEvent
import com.scto.mobile.ide.events.Events
import com.scto.mobile.ide.utils.dialogRes
import kotlinx.coroutines.launch

class RefreshCommand : EditorCommand() {
    override val id: String = "editor.refresh"

    override val title: String = "refresh"

    override suspend fun executeEditorCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor) {
        val currentTab = context.editorTab
        if (currentTab.editorState.isDirty) {
            dialogRes(
                activity = (context as? MobileIDECommandContext)?.androidContext as? android.app.Activity,
                title = strings.attention.getString(),
                msg = strings.ask_refresh.getString(),
                okRes = strings.refresh,
                onCancel = {},
                onOk = {
                    currentTab.refresh()
                    publishEvent(currentTab)
                },
            )
        } else {
            currentTab.refresh()
            publishEvent(currentTab)
        }
    }

    private fun publishEvent(currentTab: EditorTab) {
        DefaultScope.launch {
            Events.publish(EditorTabEvent.Refreshed(currentTab))
        }
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.refresh)

    override val defaultKeybinds: KeyCombination =
        KeyCombination(keyCode = KeyEvent.KEYCODE_R, ctrl = true, shift = true)
}
