package io.kiquar.plugin.zig.runner

import android.content.Context
import android.app.Activity
import android.content.res.Resources
import com.scto.mobile.ide.core.common.files.FileObject
import com.scto.mobile.ide.core.common.icons.Icon
import com.scto.mobile.ide.runner.Runner
import com.scto.mobile.ide.core.common.files.BuiltinFileType
import com.scto.mobile.ide.exec.launchTerminal
import com.scto.mobile.ide.exec.TerminalCommand
import com.scto.mobile.ide.MainActivity

class ZigRunner(
    val icon: Icon? = BuiltinFileType.ZIG.icon,
    val supportedExtensions: List<String> = listOf("zig"),
) : Runner() {

    override val id = "zig.run"
    override val label = "Run Zig"

    override fun getIcon(context: Context) = icon

    override fun matcher(fileObject: FileObject): Boolean {
        return supportedExtensions.contains(fileObject.getExtension())
    } 

    override suspend fun run(activity: Activity, fileObject: FileObject) {
        val workingDir = fileObject.getParentFile()?.getAbsolutePath()
        launchTerminal(
            activity = activity,
            terminalCommand = TerminalCommand(
                exe = "\$HOME/.local/zig/zig",
                args = arrayOf("run", fileObject.getName()),
                id = id,
                workingDir = workingDir,
            ),
        )
    }

    override suspend fun isRunning() = false

    override suspend fun stop() {}
}