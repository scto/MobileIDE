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

package com.scto.mobile.ide.core.icons.pack

import com.scto.mobile.ide.files.FileObject
import com.scto.mobile.ide.files.FileType
import com.scto.mobile.ide.files.FileTypeManager

import java.io.File

typealias IconPackId = String

typealias IconPackPath = String

data class IconPackManifest(
    val id: IconPackId,
    val name: String,
    val minAppVersion: Int? = null,
    val applyTint: Boolean = false,
    val icons: IconPackList,
)

data class IconPackList(
    val defaultFile: IconPackPath,
    val defaultFolder: IconPackPath,
    val defaultFolderExpanded: IconPackPath,
    val folderNames: Map<String, IconPackPath> = emptyMap(),
    val folderNamesExpanded: Map<String, IconPackPath> = emptyMap(),
    val fileNames: Map<String, IconPackPath> = emptyMap(),
    val fileExtensions: Map<String, IconPackPath> = emptyMap(),
    val languageNames: Map<String, IconPackPath> = emptyMap(),
)

data class IconPack(val manifest: IconPackManifest, val installDir: File) {
    fun getIconFileForFile(file: FileObject, isExpanded: Boolean = false): File? {
        val fileName = file.getName()
        val isDirectory = file.isDirectory()
        return getIconFileForName(fileName, isDirectory, isExpanded)
    }

    fun getIconFileForName(fileName: String, isDirectory: Boolean, isExpanded: Boolean = false): File? {
        val path =
            if (isDirectory) {
                if (isExpanded) {
                    // First use folderNamesExpanded, then defaultFolderExpanded
                    manifest.icons.folderNamesExpanded[fileName.lowercase()]
                        ?.let { installDir.resolve(it) }
                        ?.takeIf { it.exists() } ?: installDir.resolve(manifest.icons.defaultFolderExpanded)
                } else {
                    // First use folderNames, then defaultFolder
                    manifest.icons.folderNames[fileName.lowercase()]
                        ?.let { installDir.resolve(it) }
                        ?.takeIf { it.exists() } ?: installDir.resolve(manifest.icons.defaultFolder)
                }
            } else {
                // First use fileNames, then fileExtensions, then languageNames, then defaultFile
                val ext = fileName.substringAfterLast(".", "")

                manifest.icons.fileNames[fileName.lowercase()]?.let { installDir.resolve(it) }?.takeIf { it.exists() }
                    ?: manifest.icons.fileExtensions[ext.lowercase()]
                        ?.let { installDir.resolve(it) }
                        ?.takeIf { it.exists() }
                    ?: manifest.icons.languageNames[FileTypeManager.fromExtension(ext).name.lowercase()]
                        ?.let { installDir.resolve(it) }
                        ?.takeIf { it.exists() }
                    ?: installDir.resolve(manifest.icons.defaultFile)
            }

        // If no icon was working (even the fallback ones)
        if (!path.exists()) return null

        return path
    }

    fun getIconFileForExt(fileExtension: String): File? {
        val path =
            // First use fileExtensions, then languageNames, then defaultFile
            manifest.icons.fileExtensions[fileExtension.lowercase()]
                ?.let { installDir.resolve(it) }
                ?.takeIf { it.exists() }
                ?: manifest.icons.languageNames[FileTypeManager.fromExtension(fileExtension).name.lowercase()]
                    ?.let { installDir.resolve(it) }
                    ?.takeIf { it.exists() }
                ?: installDir.resolve(manifest.icons.defaultFile)

        // If no icon was working (even the fallback ones)
        if (!path.exists()) return null

        return path
    }

    fun getIconFileForFileType(fileType: FileType): File? {
        val extension = fileType.extensions.firstOrNull()?.lowercase()
        val typeName = fileType.name.lowercase()

        val path =
            // First use fileExtensions, then languageNames, then defaultFile
            extension?.let { manifest.icons.fileExtensions[it] }?.let { installDir.resolve(it) }?.takeIf { it.exists() }
                ?: manifest.icons.languageNames[typeName]?.let { installDir.resolve(it) }?.takeIf { it.exists() }
                ?: installDir.resolve(manifest.icons.defaultFile)

        // If no icon was working (even the fallback ones)
        if (!path.exists()) return null

        return path
    }
}
