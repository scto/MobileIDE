package com.scto.mobile.ide.exec

import android.app.Activity
import android.content.Intent
import com.scto.mobile.ide.core.common.files.child
import com.scto.mobile.ide.core.common.files.createFileIfNot
import com.scto.mobile.ide.core.common.files.localDir
import com.scto.mobile.ide.core.common.files.sandboxDir
import com.scto.mobile.ide.core.common.files.sandboxHomeDir
import com.scto.mobile.ide.core.common.files.localBinDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun isTerminalInstalled(): Boolean {
    val rootfs =
        sandboxDir().listFiles()?.filter {
            it.absolutePath != sandboxHomeDir().absolutePath &&
                it.absolutePath != sandboxDir().child("tmp").absolutePath
        } ?: emptyList()

    return localDir().child(".terminal_setup_ok_DO_NOT_REMOVE").exists() && rootfs.isNotEmpty()
}

suspend fun isTerminalWorking(): Boolean =
    withContext(Dispatchers.IO) {
        val process = ubuntuProcess(command = arrayOf("true"))
        return@withContext process.waitFor() == 0
    }

fun launchTerminal(activity: Activity, terminalCommand: TerminalCommand) {
    pendingCommand = terminalCommand
    try {
        val intent = Intent().setClassName(activity, "com.scto.mobile.ide.activities.terminal.Terminal")
        activity.startActivity(intent)
    } catch (e: Exception) {
        com.scto.mobile.ide.core.common.utils.toast("Terminal feature is not available in this build")
    }
}


fun setupAssetFile(fileName: String) {
    with(localBinDir().child(fileName)) {
        parentFile?.mkdir()
        createFileIfNot()
        writeText(
            com.scto.mobile.ide.utils.application!!.assets.open("terminal/$fileName.sh").bufferedReader().use {
                it.readText()
            }
        )
        setExecutable(true)
    }
}
