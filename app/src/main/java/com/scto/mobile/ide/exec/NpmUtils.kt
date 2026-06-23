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

object NpmUtils {
    suspend fun getInstalledVersion(packageName: String): String? {
        val result =
            ShellUtils.runUbuntu(
                command = arrayOf("npm", "list", "-g", "--prefix", "/usr", packageName, "--depth=0", "--json"),
                timeoutSeconds = 5L,
            )
        if (result.timedOut || result.exitCode != 0) return null

        return runCatching {
                val obj = JSONObject(result.output)
                obj.getJSONObject("dependencies").getJSONObject(packageName).getString("version")
            }
            .getOrNull()
    }

    suspend fun getLatestVersion(packageName: String): String? {
        val result =
            ShellUtils.runUbuntu(command = arrayOf("npm", "view", packageName, "version"), timeoutSeconds = 20L)
        if (result.timedOut || result.exitCode != 0) return null
        return result.output
    }

    suspend fun hasUpdate(packageName: String): Boolean {
        val currentVersion = getInstalledVersion(packageName)?.toVersionOrNull(false) ?: return false
        val latestVersion = getLatestVersion(packageName)?.toVersionOrNull(false) ?: return false
        return currentVersion < latestVersion
    }
}
