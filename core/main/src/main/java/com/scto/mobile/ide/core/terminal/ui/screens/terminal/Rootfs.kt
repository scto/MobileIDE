package com.scto.mobile.ide.core.terminal.ui.screens.terminal

import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import com.scto.mobile.ide.core.terminal.libcommons.application
import com.scto.mobile.ide.core.terminal.libcommons.ubuntuDir
import com.scto.mobile.ide.core.terminal.libcommons.child
import com.scto.mobile.ide.core.terminal.settings.Settings
import com.scto.mobile.ide.core.terminal.model.WorkingMode
import com.scto.mobile.ide.core.terminal.App
import java.io.File

object Rootfs {
    val reTerminal = application!!.filesDir

    init {
        if (reTerminal.exists().not()){
            reTerminal.mkdirs()
        }
    }

    var isDownloaded = mutableStateOf(isFilesDownloaded())

    fun requiresRootfs(workingMode: Int = Settings.working_Mode): Boolean {
        return workingMode != WorkingMode.ANDROID
    }

    private fun rootfsFileName(workingMode: Int): String {
        return when (workingMode) {
            WorkingMode.UBUNTU,
            WorkingMode.UBUNTU_ROOT -> "ubuntu.tar.gz"
            else -> "alpine.tar.gz"
        }
    }

    fun isFilesDownloaded(workingMode: Int = Settings.working_Mode): Boolean{
        if (!requiresRootfs(workingMode)) {
            return true
        }
        val baseReady = reTerminal.exists() &&
                reTerminal.child("proot").exists() &&
                reTerminal.child("libtalloc.so.2").exists()

        if (!baseReady) {
            return false
        }

        if (workingMode == WorkingMode.UBUNTU || workingMode == WorkingMode.UBUNTU_ROOT) {
            val ubuntuBase = ubuntuDir()
            val marker = ubuntuBase.parentFile!!.child(".termix-ubuntu-installed")
            val hasEtc = ubuntuBase.child("etc").exists() || ubuntuBase.child("root").child("etc").exists()
            return marker.exists() && hasEtc
        }

        return reTerminal.child(rootfsFileName(workingMode)).exists()
    }
}
