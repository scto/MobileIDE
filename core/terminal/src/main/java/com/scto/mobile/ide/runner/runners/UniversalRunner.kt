package com.scto.mobile.ide.runner.runners

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Environment
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.exec.TerminalCommand
import com.scto.mobile.ide.exec.launchTerminal
import com.scto.mobile.ide.file.FileObject
import com.scto.mobile.ide.file.FileWrapper
import com.scto.mobile.ide.file.child
import com.scto.mobile.ide.file.localBinDir
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.resources.drawables
import com.scto.mobile.ide.resources.getString
import com.scto.mobile.ide.resources.strings
import com.scto.mobile.ide.runner.Runner
import com.scto.mobile.ide.exec.setupAssetFile
import com.scto.mobile.ide.utils.dialogRes
import kotlinx.coroutines.launch

object UniversalRunner : Runner() {

    override val id = "universal"
    override val label = strings.universal_runner.getString()
    override val description = strings.universal_runner_desc.getString()

    override fun matcher(fileObject: FileObject): Boolean {
        return Regex(
                ".*\\.(py|js|ts|java|kt|rs|rb|php|c|cpp|cc|cxx|cs|sh|bash|zsh|fish|pl|lua|r|R|hs|f90|f95|f03|f08|pas|tcl|elm|fsx|fs)$"
            )
            .matches(fileObject.getName())
    }

    @SuppressLint("SdCardPath")
    override suspend fun run(activity: Activity, fileObject: FileObject) {
        setupAssetFile("universal_runner")

        if (fileObject !is FileWrapper) {
            dialogRes(title = strings.attention.getString(), msg = strings.non_native_filetype.getString(), onOk = {})
            return
        }

        val path = fileObject.getAbsolutePath()
        if (
            path.startsWith("/sdcard") ||
                path.startsWith("/storage/") ||
                path.startsWith(Environment.getExternalStorageDirectory().absolutePath)
        ) {
            dialogRes(
                title = strings.attention.getString(),
                msg = strings.sdcard_filetype.getString(),
                okRes = strings.continue_action,
                onCancel = {},
                onOk = { DefaultScope.launch { launchUniversalRunner(activity, fileObject) } },
            )
            return
        }

        launchUniversalRunner(activity, fileObject)
    }

    suspend fun launchUniversalRunner(activity: Activity, fileObject: FileObject) {
        launchTerminal(
            activity = activity,
            terminalCommand =
                TerminalCommand(
                    sandbox = true,
                    exe = "/bin/bash",
                    args = arrayOf(localBinDir().child("universal_runner").absolutePath, fileObject.getAbsolutePath()),
                    id = strings.universal_runner.getString(),
                    terminatePreviousSession = true,
                    workingDir = fileObject.getParentFile()?.getAbsolutePath() ?: "/",
                ),
        )
    }

    override fun getIcon(context: Context): Icon {
        return Icon.ResourceIcon(drawables.run)
    }

    override suspend fun isRunning(): Boolean {
        return false
    }

    override suspend fun stop() {}
}
