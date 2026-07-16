package com.rk.runner
import com.rk.file.FileObject
import android.app.Activity
import com.rk.icons.Icon
import android.content.Context
abstract class Runner {
    abstract val id: String
    abstract val label: String
    abstract fun getIcon(context: Context): Icon?
    abstract fun matcher(fileObject: FileObject): Boolean
    abstract suspend fun run(activity: Activity, fileObject: FileObject)
    abstract suspend fun isRunning(): Boolean
    abstract suspend fun stop()
}
object RunnerManager {
    fun registerRunner(runner: Runner) {}
    fun unregisterRunner(runner: Runner) {}
}
