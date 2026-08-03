package com.scto.mobile.ide.core.terminal.crashhandler

import android.util.Log
import com.scto.mobile.ide.core.terminal.libcommons.application
import com.scto.mobile.ide.core.terminal.libcommons.child
import com.scto.mobile.ide.core.terminal.libcommons.createFileIfNot
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

object CrashHandler : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        logError(ex)
        defaultHandler?.uncaughtException(thread, ex) ?: exitProcess(1)
    }

    fun logError(throwable: Throwable) {
        runCatching {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTraceString = sw.toString()
            Log.e("CrashHandler", "Uncaught Exception in thread ${Thread.currentThread().name}:\n$stackTraceString")

            application?.let { app ->
                val crashFile = app.filesDir.child("crash.log")
                crashFile.createFileIfNot().writeText(stackTraceString)
            }
        }.onFailure {
            it.printStackTrace()
        }
    }
}
