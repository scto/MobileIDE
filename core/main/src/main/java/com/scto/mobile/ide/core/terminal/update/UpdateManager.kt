package com.scto.mobile.ide.core.terminal.update

import com.scto.mobile.ide.core.terminal.libcommons.application
import com.scto.mobile.ide.core.terminal.libcommons.child
import com.scto.mobile.ide.core.terminal.libcommons.createFileIfNot
import com.scto.mobile.ide.core.terminal.libcommons.localBinDir
import java.io.File

class UpdateManager {
    fun onUpdate(){
        val scripts = listOf("init-host", "init", "sandbox", "setup", "termux-x11", "universal_runner", "utils")
        for (script in scripts) {
            val file = localBinDir().child(script)
            if (file.exists()) {
                file.delete()
            }
            file.createFileIfNot()
            file.writeText(application!!.assets.open("terminal/$script.sh").bufferedReader().use { it.readText() })
        }
    }
}