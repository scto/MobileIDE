package com.scto.mobile.ide.features.runner.runners

import android.app.Activity
import android.content.Context
import com.scto.mobile.ide.TerminalLauncher
import com.scto.mobile.ide.features.runner.ProjectRunner
import com.scto.mobile.ide.core.common.files.FileObject
import com.scto.mobile.ide.core.common.icons.Icon
import com.scto.mobile.ide.mobileide.MobileIDEManager
import com.scto.mobile.ide.core.terminal.resources.R.drawable as drawables
import com.scto.mobile.ide.core.terminal.resources.getString
import com.scto.mobile.ide.core.terminal.resources.R.string as strings
import kotlinx.coroutines.runBlocking

object MobileIDEProjectRunner : ProjectRunner() {
    override val id: String = "mobileide_project_runner"
    override val label: String = strings.project_runner.getString()

    override val description = strings.project_runner_desc.getString()

    override fun getIcon(context: Context): Icon {
        return Icon.ResourceIcon(drawables.run)
    }

    override fun matcher(projectRoot: FileObject): Boolean {
        return runBlocking { MobileIDEManager.getRunScript(projectRoot) != null }
    }

    override suspend fun run(activity: Activity, projectRoot: FileObject) {
        val runScript = MobileIDEManager.getRunScript(projectRoot) ?: return

        TerminalLauncher.launch(
            activity = activity,
            exe = "/bin/bash",
            args = arrayOf(runScript.getAbsolutePath()),
            id = strings.project_runner.getString(),
            workingDir = projectRoot.getAbsolutePath(),
        )
    }

    override suspend fun isRunning(): Boolean = false

    override suspend fun stop() {}
}
