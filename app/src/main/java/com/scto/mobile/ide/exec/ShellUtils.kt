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

package com.scto.mobile.ide.exec

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShellUtils {
    data class Result(val exitCode: Int, val output: String, val error: String, val timedOut: Boolean)

    suspend fun run(vararg command: String, timeoutSeconds: Long? = null): Result =
        withContext(Dispatchers.IO) {
            val process = ProcessBuilder(*command).start()

            val output = StringBuilder()
            val error = StringBuilder()

            val outputThread = Thread {
                runCatching { process.inputStream.bufferedReader().forEachLine { output.appendLine(it) } }
            }
            val errorThread = Thread {
                runCatching { process.errorStream.bufferedReader().forEachLine { error.appendLine(it) } }
            }

            outputThread.start()
            errorThread.start()

            val timedOut =
                if (timeoutSeconds != null) {
                    !process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
                } else {
                    process.waitFor()
                    false
                }

            if (timedOut) {
                process.destroyForcibly()
            }

            outputThread.join()
            errorThread.join()

            Result(
                exitCode = if (timedOut) -1 else process.exitValue(),
                output = output.toString().trim(),
                error = error.toString().trim(),
                timedOut = timedOut,
            )
        }

    suspend fun runUbuntu(workingDir: String? = null, vararg command: String, timeoutSeconds: Long? = null): Result =
        withContext(Dispatchers.IO) {
            val process = ubuntuProcess(workingDir = workingDir, command = command.toList())

            val output = StringBuilder()
            val error = StringBuilder()

            val outputThread = Thread {
                runCatching { process.inputStream.bufferedReader().forEachLine { output.appendLine(it) } }
            }
            val errorThread = Thread {
                runCatching { process.errorStream.bufferedReader().forEachLine { error.appendLine(it) } }
            }

            outputThread.start()
            errorThread.start()

            val timedOut =
                if (timeoutSeconds != null) {
                    !process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
                } else {
                    process.waitFor()
                    false
                }

            if (timedOut) {
                process.destroyForcibly()
            }

            outputThread.join()
            errorThread.join()

            Result(
                exitCode = if (timedOut) -1 else process.exitValue(),
                output = output.toString().trim(),
                error = error.toString().trim(),
                timedOut = timedOut,
            )
        }
}
