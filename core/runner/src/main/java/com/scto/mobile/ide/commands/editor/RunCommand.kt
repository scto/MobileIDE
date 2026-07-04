package com.scto.mobile.ide.commands.editor

import android.view.KeyEvent
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.commands.CommandProvider
import com.scto.mobile.ide.commands.EditorActionContext
import com.scto.mobile.ide.commands.EditorCommand
import com.scto.mobile.ide.commands.EditorNonActionContext
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.resources.drawables
import com.scto.mobile.ide.resources.getString
import com.scto.mobile.ide.resources.strings
import com.scto.mobile.ide.runner.RunnerManager
import com.scto.mobile.ide.settings.Settings
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import com.scto.mobile.ide.runner.RunnerUI

@OptIn(DelicateCoroutinesApi::class)
class RunCommand : EditorCommand() {
    override val id: String = "editor.run"

    override fun getLabel(): String = strings.run.getString()

    override fun action(editorActionContext: EditorActionContext) {
        val editorTab = editorActionContext.editorTab
        val activity = editorActionContext.currentActivity
        CommandProvider.SaveCommand.action(editorActionContext)
        DefaultScope.launch {
            Settings.runs += 1
            RunnerManager.run(
                activity = activity,
                fileObject = editorTab.file,
                onMultipleRunners = {
                    RunnerUI.runnersToShow = it
                    RunnerUI.showRunnerDialog = true
                },
            )
        }
    }

    override fun isSupported(editorNonActionContext: EditorNonActionContext): Boolean {
        return RunnerManager.isRunnable(editorNonActionContext.editorTab.file)
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(drawables.run)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_F5)
}
