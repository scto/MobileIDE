package com.scto.mobile.ide.exec

import android.app.Activity
import android.content.Intent
import com.scto.mobile.ide.file.child
import com.scto.mobile.ide.file.createFileIfNot
import com.scto.mobile.ide.file.localDir
import com.scto.mobile.ide.file.sandboxDir
import com.scto.mobile.ide.file.sandboxHomeDir
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
        com.scto.mobile.ide.utils.toast("Terminal feature is not available in this build")
    }
}

fun setupAssetFile(fileName: String) {
    with(com.scto.mobile.ide.file.localBinDir().child(fileName)) {
        parentFile?.mkdir()
        if (exists().not()) {
            createFileIfNot()
            writeText(
                com.scto.mobile.ide.utils.application!!.assets.open("terminal/$fileName.sh").bufferedReader().use {
                    it.readText()
                }
            )
        }
    }
}
