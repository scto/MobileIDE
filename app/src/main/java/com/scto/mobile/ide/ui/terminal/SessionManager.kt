/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  Thomas Schmid  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.scto.mobile.ide.ui.terminal

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.scto.mobile.ide.core.common.utils.LogCatcher
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

data class SessionWrapper(val session: TerminalSession, val title: String)

object SessionManager {
    val sessions = mutableStateListOf<SessionWrapper>()
    var currentSessionIndex by mutableIntStateOf(0)

    val currentSession: TerminalSession?
        get() =
            if (sessions.isNotEmpty() && currentSessionIndex in sessions.indices) {
                sessions[currentSessionIndex].session
            } else null

    fun addNewSession(context: Context, initCommand: String? = null, tabTitle: String? = null) {
        val client =
            object : TerminalSessionClient {
                override fun onTextChanged(changedSession: TerminalSession) {}

                override fun onTitleChanged(changedSession: TerminalSession) {}

                override fun onSessionFinished(finishedSession: TerminalSession) {
                    val wrapper = sessions.find { it.session == finishedSession }
                    if (wrapper != null) {
                        if (finishedSession.exitStatus == 0) {
                            removeSession(wrapper)
                        }
                    }
                }

                override fun onCopyTextToClipboard(session: TerminalSession, text: String) {}

                override fun onPasteTextFromClipboard(session: TerminalSession?) {}

                override fun onBell(session: TerminalSession) {}

                override fun onColorsChanged(session: TerminalSession) {}

                override fun onTerminalCursorStateChange(state: Boolean) {}

                override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}

                override fun getTerminalCursorStyle(): Int = 0

                override fun logError(tag: String, message: String) {}

                override fun logWarn(tag: String, message: String) {}

                override fun logInfo(tag: String, message: String) {}

                override fun logDebug(tag: String, message: String) {}

                override fun logVerbose(tag: String, message: String) {}

                override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {
                    e.printStackTrace()
                }

                override fun logStackTrace(tag: String, e: Exception) {
                    e.printStackTrace()
                }
            }

        LogCatcher.i("SessionManager", "addNewSession: creating new DistroManager session with command $initCommand.")
        val session = DistroManager.createSession(context, client, initCommand = initCommand)
        val title = tabTitle ?: "Term ${sessions.size + 1}"
        sessions.add(SessionWrapper(session, title))
        currentSessionIndex = sessions.lastIndex
        LogCatcher.i("SessionManager", "addNewSession completed. Title=$title, index=$currentSessionIndex")
        TerminalService.startService(context)
    }

    fun removeSession(wrapper: SessionWrapper) {
        LogCatcher.i("SessionManager", "removeSession wrapper.title=${wrapper.title}")
        wrapper.session.finishIfRunning()
        sessions.remove(wrapper)
        if (currentSessionIndex >= sessions.size) {
            currentSessionIndex = (sessions.size - 1).coerceAtLeast(0)
        }
        if (sessions.isEmpty()) {
            if (com.scto.mobile.ide.core.terminal.settings.Settings.terminal_close_behavior == "new_session") {
                val ctx = com.scto.mobile.ide.utils.application
                if (ctx != null) {
                    addNewSession(ctx)
                }
            } else {
                LogCatcher.i("SessionManager", "All sessions removed. Stopping TerminalService.")
                val ctx = com.scto.mobile.ide.utils.application
                if (ctx != null) {
                    TerminalService.stopService(ctx)
                }
            }
        }
    }

    fun switchTo(index: Int) {
        LogCatcher.i("SessionManager", "switchTo index=$index (currentSessionIndex was $currentSessionIndex)")
        if (index in sessions.indices) {
            currentSessionIndex = index
        }
    }
}
