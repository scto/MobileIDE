package com.scto.mobile.ide.features.git

import org.eclipse.jgit.lib.ProgressMonitor

interface ProgressCoordinator : ProgressMonitor {
    fun showDialog()

    fun hideDialog()

    fun cancel()
}
