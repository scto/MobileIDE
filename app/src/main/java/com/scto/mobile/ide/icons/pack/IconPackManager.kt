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

package com.scto.mobile.ide.icons.pack

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.pm.PackageInfoCompat
import com.scto.mobile.ide.core.terminal.resources.getString
import com.scto.mobile.ide.core.terminal.resources.strings
import com.scto.mobile.ide.core.terminal.settings.Settings

import com.scto.mobile.ide.core.common.utils.dialogRes
import com.scto.mobile.ide.core.common.utils.child
import com.scto.mobile.ide.core.common.utils.createDirIfNot
import com.scto.mobile.ide.utils.application
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

fun JSONObject.toMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val keys = this.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        map[key] = this.getString(key)
    }
    return map
}

fun parseIconPackManifest(jsonStr: String): IconPackManifest {
    val obj = JSONObject(jsonStr)
    val iconsObj = obj.getJSONObject("icons")

    val folderNames =
        if (iconsObj.has("folderNames")) {
            iconsObj.getJSONObject("folderNames").toMap()
        } else emptyMap()

    val folderNamesExpanded =
        if (iconsObj.has("folderNamesExpanded")) {
            iconsObj.getJSONObject("folderNamesExpanded").toMap()
        } else emptyMap()

    val fileNames =
        if (iconsObj.has("fileNames")) {
            iconsObj.getJSONObject("fileNames").toMap()
        } else emptyMap()

    val fileExtensions =
        if (iconsObj.has("fileExtensions")) {
            iconsObj.getJSONObject("fileExtensions").toMap()
        } else emptyMap()

    val languageNames =
        if (iconsObj.has("languageNames")) {
            iconsObj.getJSONObject("languageNames").toMap()
        } else emptyMap()

    val iconsList =
        IconPackList(
            defaultFile = iconsObj.getString("defaultFile"),
            defaultFolder = iconsObj.getString("defaultFolder"),
            defaultFolderExpanded = iconsObj.getString("defaultFolderExpanded"),
            folderNames = folderNames,
            folderNamesExpanded = folderNamesExpanded,
            fileNames = fileNames,
            fileExtensions = fileExtensions,
            languageNames = languageNames,
        )

    return IconPackManifest(
        id = obj.getString("id"),
        name = obj.getString("name"),
        minAppVersion = if (obj.has("minAppVersion")) obj.getInt("minAppVersion") else null,
        applyTint = if (obj.has("applyTint")) obj.getBoolean("applyTint") else false,
        icons = iconsList,
    )
}

val currentIconPack = mutableStateOf<IconPack?>(null)
val iconPackDir = File(com.scto.mobile.ide.utils.application!!.filesDir.parentFile, "mobileide/local/icon_pack").also { it.createDirIfNot() }

class IconPackManager(private val context: Application) {
    val iconPacks = mutableStateMapOf<IconPackId, IconPack>()

    suspend fun installIconPack(zipFile: File) =
        withContext(Dispatchers.IO) {
            // Extract to temp dir first
            val tempDir = File(context.cacheDir, "icon_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                ZipFile(zipFile).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        if (!entry.isDirectory) {
                            val target = tempDir.resolve(entry.name)
                            target.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                target.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                    }
                }

                installIconPackFromDir(tempDir)
            } finally {
                tempDir.deleteRecursively()
            }
        }

    private fun installIconPackFromDir(dir: File) {
        val iconPackManifest = validateIconPack(dir) ?: return

        val packageName = application!!.packageName
        val packageManager = application!!.packageManager
        val currentVersionCode = PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))
        if (iconPackManifest.minAppVersion != null && iconPackManifest.minAppVersion.toLong() > currentVersionCode) {
            val ctx = com.scto.mobile.ide.utils.currentActivity.get()
            dialogRes(
                activity = ctx!!,
                title = strings.warning.getString(),
                msg = strings.incompatible_theme_warning.getString(),
                cancelRes = strings.cancel,
                okRes = strings.continue_action,
                onOk = { writeIconPackToDisk(iconPackManifest, dir) },
            )
            return
        }

        writeIconPackToDisk(iconPackManifest, dir)
    }

    private fun writeIconPackToDisk(iconPackManifest: IconPackManifest, dir: File) {
        val installDir = iconPackDir.child(iconPackManifest.id)
        if (installDir.exists()) {
            uninstallIconPack(iconPackManifest.id)
        }

        dir.copyRecursively(installDir, overwrite = true)

        val iconPack = IconPack(iconPackManifest, installDir)
        iconPacks[iconPackManifest.id] = iconPack
    }

    internal fun validateIconPack(dir: File): IconPackManifest? {
        val iconPackJson = dir.resolve("manifest.json")
        if (!iconPackJson.exists()) {
            val ctx = com.scto.mobile.ide.utils.currentActivity.get()
            dialogRes(
                ctx!!,
                strings.icon_pack_install_failed.getString(),
                strings.manifest_missing.getString(),
                cancelable = false,
            )

            return null
        }
        val iconPackManifest =
            runCatching { parseIconPackManifest(iconPackJson.readText()) }
                .getOrElse { e ->
                    val ctx = com.scto.mobile.ide.utils.currentActivity.get()
                    dialogRes(
                        ctx!!,
                        strings.icon_pack_install_failed.getString(),
                        e.localizedMessage ?: strings.unknown_err.getString(),
                        cancelable = false,
                    )
                    return null
                }

        return iconPackManifest
    }

    fun uninstallIconPack(iconPackId: IconPackId) {
        val iconPack = iconPacks[iconPackId] ?: return
        iconPack.installDir.deleteRecursively()
        iconPacks.remove(iconPackId)
    }

    suspend fun indexIconPacks() {
        iconPacks.clear()
        withContext(Dispatchers.IO) {
            iconPackDir.listFiles()?.forEach { dir ->
                if (dir.isDirectory) {
                    val manifestJson = dir.resolve("manifest.json")
                    if (manifestJson.exists()) {
                        runCatching {
                            val iconPackManifest = parseIconPackManifest(manifestJson.readText())
                            val installDir = iconPackDir.child(iconPackManifest.id)
                            val iconPack = IconPack(iconPackManifest, installDir)
                            iconPacks[iconPackManifest.id] = iconPack
                        }
                    }
                }
            }
        }

        if (Settings.icon_pack.isNotEmpty()) {
            currentIconPack.value = iconPacks[Settings.icon_pack]
        }
    }
}
