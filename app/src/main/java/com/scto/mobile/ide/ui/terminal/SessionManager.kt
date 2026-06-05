/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.scto.mobile.ide.ui.terminal

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

/**
 * Wrapper class: Resolves the issue of mTitle being invisible inside TerminalSession. We maintain the Session object
 * and its title ourselves.
 */
data class SessionWrapper(val session: TerminalSession, val title: String)

object SessionManager {
    // Use Compose's mutableStateListOf to ensure UI listens to list changes
    val sessions = mutableStateListOf<SessionWrapper>()

    // Currently selected session index
    var currentSessionIndex by mutableIntStateOf(0)

    // Get currently active Session object (for UI use)
    // Return null if list is empty or index is out of bounds
    val currentSession: TerminalSession?
        get() =
            if (sessions.isNotEmpty() && currentSessionIndex in sessions.indices) {
                sessions[currentSessionIndex].session
            } else null

    /** Create new session and add to list */
    fun addNewSession(context: Context) {
        // Define Session callback interface
        val client =
            object : TerminalSessionClient {
                override fun onTextChanged(changedSession: TerminalSession) {
                    // When text changes, TerminalView on UI layer automatically redraws
                }

                override fun onTitleChanged(changedSession: TerminalSession) {
                    // If you want to support dynamic titles (e.g., show current directory),
                    // you can update wrapper's title here.
                    // Temporarily left blank to use static title.
                }

                override fun onSessionFinished(finishedSession: TerminalSession) {
                    val exitStatus = finishedSession.exitStatus
                    android.util.Log.i("SessionManager", "Terminal session finished: exitStatus=$exitStatus")
                    // When Shell exits (e.g., user enters 'exit'), automatically remove this session
                    // 1. Find the wrapper containing this session in the list
                    val wrapper = sessions.find { it.session == finishedSession }
                    // 2. Remove it
                    if (wrapper != null) {
                        removeSession(wrapper)
                    }
                }

                // --- Below are other interfaces that must be implemented but are temporarily not needed ---
                override fun onCopyTextToClipboard(session: TerminalSession, text: String) {}

                override fun onPasteTextFromClipboard(session: TerminalSession?) {}

                override fun onBell(session: TerminalSession) {}

                override fun onColorsChanged(session: TerminalSession) {}

                override fun onTerminalCursorStateChange(state: Boolean) {}

                override fun setTerminalShellPid(session: TerminalSession, pid: Int) {
                    android.util.Log.i("SessionManager", "Terminal session shell PID set: $pid")
                }

                override fun getTerminalCursorStyle(): Int = 0

                override fun logError(tag: String, message: String) {
                    android.util.Log.e(tag, message)
                }

                override fun logWarn(tag: String, message: String) {
                    android.util.Log.w(tag, message)
                }

                override fun logInfo(tag: String, message: String) {
                    android.util.Log.i(tag, message)
                }

                override fun logDebug(tag: String, message: String) {
                    android.util.Log.d(tag, message)
                }

                override fun logVerbose(tag: String, message: String) {
                    android.util.Log.v(tag, message)
                }

                override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {
                    android.util.Log.e(tag, message, e)
                }

                override fun logStackTrace(tag: String, e: Exception) {
                    android.util.Log.e(tag, "Terminal Exception", e)
                }
            }

        // Call AlpineManager to create core session
        val session = AlpineManager.createSession(context, client)

        // Generate title (e.g., Term 1, Term 2)
        val title = "Term ${sessions.size + 1}"

        // Wrap and add to list
        sessions.add(SessionWrapper(session, title))

        // Automatically switch to newly created session
        currentSessionIndex = sessions.lastIndex
    }

    /** Remove specified session */
    fun removeSession(wrapper: SessionWrapper) {
        // 1. Ensure underlying session stops
        wrapper.session.finishIfRunning()

        // 2. Remove from list
        sessions.remove(wrapper)

        // 3. Fix current index to prevent out of bounds
        if (currentSessionIndex >= sessions.size) {
            currentSessionIndex = (sessions.size - 1).coerceAtLeast(0)
        }
    }

    /** Switch to session at specified index */
    fun switchTo(index: Int) {
        if (index in sessions.indices) {
            currentSessionIndex = index
        }
    }
}
