package com.scto.mobile.ide.terminal

import com.scto.mobile.ide.exec.setupAssetFile
import com.scto.mobile.ide.core.common.files.child
import com.scto.mobile.ide.core.common.files.createFileIfNot
import com.scto.mobile.ide.core.common.files.localBinDir
import com.scto.mobile.ide.core.common.files.localDir
import com.scto.mobile.ide.core.common.files.sandboxDir
import com.scto.mobile.ide.core.terminal.libcommons.application

fun setupTerminalFiles() {
    if (sandboxDir().exists().not() || localBinDir().exists().not()) return

    with(localDir().child("stat")) {
        if (exists().not()) {
            createFileIfNot()
            writeText(stat)
        }
    }

    with(localDir().child("vmstat")) {
        if (exists().not()) {
            createFileIfNot()
            writeText(vmstat)
        }
    }

    with(localBinDir().child("termux-x11")) {
        if (exists().not()) {
            createFileIfNot()
            writeText(application!!.assets.open("terminal/termux-x11.sh").bufferedReader().use { it.readText() })
        }
    }

    val internalFiles = listOf("init", "sandbox", "setup", "utils")
    internalFiles.forEach { setupAssetFile(it) }

    application!!.assets.list("terminal/lsp")?.forEach { setupLspFile(it.removeSuffix(".sh")) }
}

fun setupLspFile(fileName: String) = setupAssetFile("lsp/$fileName")
