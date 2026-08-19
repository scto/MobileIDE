package com.scto.mobile.ide.commands.editor





import android.view.KeyEvent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.events.EditorTabEvent
import com.scto.mobile.ide.events.Events
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.tabs.editor.EditorTab
import com.scto.mobile.ide.utils.dialogRes
import kotlinx.coroutines.launch











class RefreshCommand : EditorCommand() {
    override val id: String = "editor.refresh"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.refresh.getString()

    override fun action(context: EditorActionContext) {
        val currentTab = context.editorTab
        if (currentTab.editorState.isDirty) {
            dialogRes(
                activity = context.currentActivity,
                title = com.scto.mobile.ide.core.main.R.string.attention.getString(),
                msg = com.scto.mobile.ide.core.main.R.string.ask_refresh.getString(),
                okRes = com.scto.mobile.ide.core.main.R.string.refresh,
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

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.refresh)

    override val defaultKeybinds: KeyCombination =
        KeyCombination(keyCode = KeyEvent.KEYCODE_R, ctrl = true, shift = true)
}
