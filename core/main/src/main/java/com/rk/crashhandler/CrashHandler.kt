package com.rk.crashhandler

import android.content.Context
import android.os.Looper
import com.rk.libcommons.application
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

object CrashHandler : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        runCatching {
            logErrorOrExit(ex)
        }.onFailure {
            it.printStackTrace()
            exitProcess(1)
        }

        if (Looper.myLooper() != null) {
            while (true) {
                try {
                    Looper.loop()
                    return
                } catch (t: Throwable) {
                    Thread{
                        t.printStackTrace()
                        logErrorOrExit(t)
                    }.start()
                }
            }
        } else {
            exitProcess(1)
        }
    }
}

fun logErrorOrExit(throwable: Throwable){
    runCatching {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTraceString = sw.toString()
        val app = application
        if (app != null) {
            val prefs = app.getSharedPreferences("mobileide_prefs", Context.MODE_PRIVATE)
            val savedPath = prefs.getString("workspace_path", null)
            val workspacePath = if (savedPath.isNullOrBlank()) {
                "/storage/emulated/0/MobileIDEProjects"
            } else {
                savedPath
            }
            
            var logged = false
            runCatching {
                val projectsDir = File(workspacePath)
                if (!projectsDir.exists()) {
                    projectsDir.mkdirs()
                }
                val logFile = File(projectsDir, "crash.log")
                logFile.appendText("\n$stackTraceString\n")
                logged = true
            }
            
            if (!logged) {
                runCatching {
                    app.filesDir.child("crash.log").createFileIfNot().appendText("\n$stackTraceString\n")
                }
            }
        } else {
            System.err.println(stackTraceString)
        }
    }.onFailure { it.printStackTrace(); exitProcess(-1) }
}


