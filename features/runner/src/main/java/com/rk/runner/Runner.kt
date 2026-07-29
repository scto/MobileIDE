package com.scto.mobile.ide.features.runner

import android.app.Activity
import android.content.Context
import com.scto.mobile.ide.core.common.files.FileObject
import com.scto.mobile.ide.core.common.icons.Icon
import com.scto.mobile.ide.settings.Preference

abstract class Runner {
    abstract val id: String
    abstract val label: String
    open val description: String? = null
    open val onConfigure: (() -> Unit)? = null

    abstract fun getIcon(context: Context): Icon?

    abstract suspend fun isRunning(): Boolean

    abstract suspend fun stop()

    fun isEnabled(): Boolean {
        return Preference.getBoolean("runner_$id", true)
    }

    fun setEnabled(enabled: Boolean) {
        Preference.setBoolean("runner_$id", enabled)
    }
}

abstract class FileRunner : Runner() {
    abstract fun matcher(fileObject: FileObject): Boolean

    abstract suspend fun run(activity: Activity, fileObject: FileObject)
}

abstract class ProjectRunner : Runner() {
    abstract fun matcher(projectRoot: FileObject): Boolean

    abstract suspend fun run(activity: Activity, projectRoot: FileObject)
}
