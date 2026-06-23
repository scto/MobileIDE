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

package com.scto.mobile.ide.ui.settings.debugOptions

import androidx.compose.runtime.mutableStateListOf
import com.rk.resources.getString
import com.rk.resources.strings

enum class LogLevel(val label: String, val value: Int) {
    ERROR(strings.error.getString(), 1),
    WARN(strings.warning.getString(), 2),
    INFO(strings.info.getString(), 3),
    DEBUG(strings.debug.getString(), 5),
}

data class LogEntry(val level: LogLevel, val message: String, val timestamp: Long = System.currentTimeMillis())

object LogCollector {
    val logs = mutableStateListOf<LogEntry>()

    fun reportDebug(message: String) {
        logs.add(LogEntry(LogLevel.DEBUG, message))
    }

    fun reportInfo(message: String) {
        logs.add(LogEntry(LogLevel.INFO, message))
    }

    fun reportWarn(message: String) {
        logs.add(LogEntry(LogLevel.WARN, message))
    }

    fun reportError(message: String) {
        logs.add(LogEntry(LogLevel.ERROR, message))
    }

    fun clearLogs() {
        logs.clear()
    }
}
