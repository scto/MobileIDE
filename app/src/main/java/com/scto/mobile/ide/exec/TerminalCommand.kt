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

var pendingCommand: TerminalCommand? = null

data class TerminalCommand(
    val sandbox: Boolean = true,
    val exe: String,
    val args: Array<String> = arrayOf(),
    val id: String,
    val terminatePreviousSession: Boolean = true,
    val workingDir: String? = null,
    val env: Array<String> = arrayOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false

        other as TerminalCommand

        if (exe != other.exe) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = exe.hashCode()
        result = 31 * result + args.contentHashCode()
        return result
    }
}
