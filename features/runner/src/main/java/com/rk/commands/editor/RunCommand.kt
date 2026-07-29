package com.scto.mobile.ide.commands.editor

import android.view.KeyEvent
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.Command
import com.scto.mobile.ide.commands.CommandProvider
import com.scto.mobile.ide.commands.KeyCombination
import com.scto.mobile.ide.features.runner.RunnerManager
import com.scto.mobile.ide.features.runner.RunnerUI
import com.scto.mobile.ide.filetree.FileTreeTab
import com.scto.mobile.ide.core.common.icons.Icon
import com.scto.mobile.ide.core.terminal.resources.R.drawable as drawables
import com.scto.mobile.ide.core.terminal.resources.getString
import com.scto.mobile.ide.core.terminal.resources.R.string as strings
import com.scto.mobile.ide.tabs.editor.EditorTab
import kotlinx.coroutines.DelicateCoroutinesApi

@OptIn(DelicateCoroutinesApi::class)
object RunCommand : Command() {
    override val id: String = "editor.run"

    override fun getLabel(): String = strings.run.getString()

    override fun action(context: ActionContext) {
        launchRunner(context, forceSelection = false)
    }

    override fun onLongClick(context: ActionContext): Boolean {
        launchRunner(context, forceSelection = true)
        return true
    }

    private fun launchRunner(context: ActionContext, forceSelection: Boolean) {
        val mainViewModel = commandContext.mainViewModel
        val drawerViewModel = commandContext.drawerViewModel

        val currentTab = mainViewModel.currentTab as? EditorTab
        val currentDrawerTab = drawerViewModel.currentDrawerTab as? FileTreeTab
        val projectRoot = currentTab?.projectRoot ?: currentDrawerTab?.root
        val fileObject = currentTab?.file

        RunnerManager.run(
            activity = context.currentActivity,
            fileObject = fileObject,
            projectRoot = projectRoot,
            forceSelection = forceSelection,
            beforeRun = {
                if (currentTab != null) {
                    CommandProvider.SaveCommand.action(context)
                }
            },
            onMultipleRunners = {
                RunnerUI.runnersToShow = it
                RunnerUI.showRunnerDialog = true
            },
        )
    }

    override fun isSupported(): Boolean {
        val mainViewModel = commandContext.mainViewModel
        val drawerViewModel = commandContext.drawerViewModel

        val currentTab = mainViewModel.currentTab as? EditorTab
        val currentDrawerTab = drawerViewModel.currentDrawerTab as? FileTreeTab
        val projectRoot = currentTab?.projectRoot ?: currentDrawerTab?.root
        val fileObject = currentTab?.file

        return RunnerManager.isRunnable(fileObject, projectRoot)
    }

    override fun getIcon(): Icon = Icon.ResourceIcon(drawables.run)

    override val defaultKeybinds: KeyCombination = KeyCombination(keyCode = KeyEvent.KEYCODE_F5)
}
