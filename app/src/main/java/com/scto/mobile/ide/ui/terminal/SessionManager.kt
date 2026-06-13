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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wrapper class: Solves the issue where mTitle inside TerminalSession is invisible. We maintain the Session object and
 * its title ourselves.
 */
data class SessionWrapper(val session: TerminalSession, val title: String)

object SessionManager {
    // Use Compose's mutableStateListOf to ensure the UI can listen to list changes
    val sessions = mutableStateListOf<SessionWrapper>()

    // Currently selected session index
    var currentSessionIndex by mutableIntStateOf(0)

    // Get the currently active Session object (for UI use).
    // Returns null if the list is empty or the index is out of bounds.
    val currentSession: TerminalSession?
        get() =
            if (sessions.isNotEmpty() && currentSessionIndex in sessions.indices) {
                sessions[currentSessionIndex].session
            } else null

    /** Create a new session and add it to the list */
    suspend fun addNewSession(context: Context) {
        // Define the callback interface for Session
        val client =
            object : TerminalSessionClient {
                override fun onTextChanged(changedSession: TerminalSession) {
                    // When text changes, the TerminalView in the UI layer automatically redraws
                }

                override fun onTitleChanged(changedSession: TerminalSession) {
                    // If you want to support dynamic titles (e.g., displaying the current directory),
                    // you can update the wrapper's title here. Leaving it blank for now, using a static title.
                }

                override fun onSessionFinished(finishedSession: TerminalSession) {
                    // When the Shell exits (e.g., user inputs 'exit'), automatically remove this session
                    // 1. Find the wrapper containing this session in the list
                    val wrapper = sessions.find { it.session == finishedSession }
                    // 2. Remove it
                    if (wrapper != null) {
                        removeSession(wrapper)
                    }
                }

                // --- Below are other interfaces that must be implemented but are not needed temporarily ---
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

        // Call AlpineManager to create the core session
        val session = withContext(Dispatchers.IO) { AlpineManager.createSession(context, client) }

        withContext(Dispatchers.Main) {
            // Generate title (e.g.: Term 1, Term 2)
            val title = "Term ${sessions.size + 1}"

            // Wrap and add to the list
            sessions.add(SessionWrapper(session, title))

            // Automatically switch to the newly created session
            currentSessionIndex = sessions.lastIndex
        }
    }

    /** Remove the specified session */
    fun removeSession(wrapper: SessionWrapper) {
        // 1. Ensure the underlying session stops
        wrapper.session.finishIfRunning()

        // 2. Remove from the list
        sessions.remove(wrapper)

        // 3. Fix the current index to prevent out of bounds
        if (currentSessionIndex >= sessions.size) {
            currentSessionIndex = (sessions.size - 1).coerceAtLeast(0)
        }
    }

    /** Switch to the session at the specified index */
    fun switchTo(index: Int) {
        if (index in sessions.indices) {
            currentSessionIndex = index
        }
    }
}
