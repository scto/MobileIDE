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

import io.github.z4kn4fein.semver.toVersionOrNull
import org.json.JSONObject

object PipxUtils {
    suspend fun getInstalledVersion(venvName: String): String? {
        val result =
            ShellUtils.runUbuntu(
                command = arrayOf("pipx", "runpip", venvName, "index", "versions", venvName, "--json"),
                timeoutSeconds = 20L,
            )
        if (result.timedOut || result.exitCode != 0) return null

        return runCatching {
                val obj = JSONObject(result.output)
                obj.getString("installed_version")
            }
            .getOrNull()
    }

    suspend fun getLatestVersion(venvName: String): String? {
        val result =
            ShellUtils.runUbuntu(
                command = arrayOf("pipx", "runpip", venvName, "index", "versions", venvName, "--json"),
                timeoutSeconds = 20L,
            )
        if (result.timedOut || result.exitCode != 0) return null

        return runCatching {
                val obj = JSONObject(result.output)
                obj.getString("latest")
            }
            .getOrNull()
    }

    suspend fun hasUpdate(venvName: String): Boolean {
        val result =
            ShellUtils.runUbuntu(
                command = arrayOf("pipx", "runpip", venvName, "index", "versions", venvName, "--json"),
                timeoutSeconds = 20L,
            )
        if (result.timedOut || result.exitCode != 0) return false

        return runCatching {
                val obj = JSONObject(result.output)
                val latest = obj.getString("latest")
                val installed = obj.getString("installed_version")

                val latestVersion = latest.toVersionOrNull(false) ?: return false
                val installedVersion = installed.toVersionOrNull(false) ?: return false
                installedVersion < latestVersion
            }
            .getOrDefault(false)
    }
}
