package com.scto.mobile.ide.plugin.go.runner

import android.content.Context
import android.app.Activity
import android.content.res.Resources
import com.rk.file.FileObject
import com.rk.icons.Icon
import com.rk.runner.Runner
import com.rk.file.BuiltinFileType
import com.rk.exec.launchTerminal
import com.rk.exec.TerminalCommand
import com.rk.activities.main.MainActivity

class GoRunner(
    val icon: Icon? = BuiltinFileType.ZIG.icon,
    val supportedExtensions: List<String> = listOf("go"),
) : Runner() {

    override val id = "go.run"
    override val label = "Run Go"

    override fun getIcon(context: Context) = icon

    override fun matcher(fileObject: FileObject): Boolean {
        return supportedExtensions.contains(fileObject.getExtension())
    } 

    override suspend fun run(activity: Activity, fileObject: FileObject) {
        val workingDir = fileObject.getParentFile()?.getAbsolutePath()
        launchTerminal(
            activity = activity,
            terminalCommand = TerminalCommand(
                exe = "/bin/go",
                args = arrayOf("run", fileObject.getAbsolutePath()),
                id = id,
                workingDir = workingDir,
            ),
        )
    }

    override suspend fun isRunning() = false

    override suspend fun stop() {}
}