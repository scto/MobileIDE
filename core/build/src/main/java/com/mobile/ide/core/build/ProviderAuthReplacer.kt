// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.build

import com.mobile.ide.core.utils.LogCatcher

import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * Provider authority replacement utility class.
 * Specifically used to handle ContentProvider authority conflicts in AndroidManifest.xml
 */
object ProviderAuthReplacer {
    private const val CHUNK_STRING_POOL = 0x001C0001
    private const val AXML_HEADER = 0x00080003

    /**
     * Replace Provider authorities in Manifest
     */
    fun replaceProviderAuthorities(manifestFile: File, oldPackageName: String, newPackageName: String) {
        if (oldPackageName == newPackageName) {
            LogCatcher.d("ProviderAuthReplacer", "Package names are the same, no replacement needed")
            return
        }

        val authMapping = mutableMapOf<String, String>()

        val baseProviders = arrayOf(
            ".provider", ".fileprovider", ".androidx-startup",
            ".appsflyer-provider", ".firebase-provider", ".download-provider",
            ".cache-provider", ".security-provider"
        )

        for (suffix in baseProviders) {
            authMapping[oldPackageName + suffix] = newPackageName + suffix
        }

        val commonProviders = arrayOf(
            "com.web.webapp.provider",
            "com.web.webapp.fileprovider",
            "com.web.webapp.androidx-startup",
            "$oldPackageName.provider.Provider",
            "$oldPackageName.provider.FileProvider",
            "$oldPackageName.provider.DownloadProvider"
        )

        for (oldAuth in commonProviders) {
            val newAuth = when {
                oldAuth.startsWith("com.web.webapp.") -> oldAuth.replace("com.web.webapp.", "$newPackageName.")
                oldAuth.startsWith("$oldPackageName.") -> oldAuth.replace("$oldPackageName.", "$newPackageName.")
                else -> newPackageName + oldAuth.substring(oldAuth.lastIndexOf('.'))
            }
            authMapping[oldAuth] = newAuth
        }

        if (authMapping.isNotEmpty()) {
            LogCatcher.i("ProviderAuthReplacer", "Starting to replace Provider authorities, total ${authMapping.size} mappings")
            batchReplaceStringInAXML(manifestFile, authMapping)
        }
    }

    /**
     * Scan and get all Provider authorities from the Manifest
     */
    fun scanProviderAuthorities(manifestFile: File): List<String> {
        val authorities = mutableListOf<String>()
        val data = manifestFile.readBytes()
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        if (buffer.int != AXML_HEADER) throw IllegalArgumentException("Invalid AXML file")
        buffer.position(8)

        if (buffer.int != CHUNK_STRING_POOL) return authorities

        val chunkSize = buffer.int
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringsOffset = buffer.int
        
        val isUTF8 = (flags and 0x0100) != 0
        val stringPoolStart = buffer.position() - 28
        val offsets = IntArray(stringCount) { buffer.int }
        val dataStart = stringPoolStart + stringsOffset

        for (i in 0 until stringCount) {
            buffer.position(dataStart + offsets[i])
            val str = readString(buffer, isUTF8)
            if (str.contains(".provider") || str.contains(".fileprovider") ||
                str.contains("content://") || str.contains(".startup")
            ) {
                authorities.add(str)
            }
        }
        return authorities
    }

    /**
     * Batch replace strings in AXML
     */
    fun batchReplaceStringInAXML(axmlFile: File, replacementMap: Map<String, String>) {
        if (replacementMap.isEmpty()) return

        val data = axmlFile.readBytes()
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        if (buffer.int != AXML_HEADER) throw IllegalArgumentException("Invalid AXML file")
        buffer.position(8)

        if (buffer.int != CHUNK_STRING_POOL) {
            LogCatcher.w("ProviderAuthReplacer", "String pool not found")
            return
        }

        val chunkSize = buffer.int
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringsOffset = buffer.int
        
        val isUTF8 = (flags and 0x0100) != 0
        val stringPoolStart = buffer.position() - 28
        val offsets = IntArray(stringCount) { buffer.int }
        val dataStart = stringPoolStart + stringsOffset

        val strings = mutableListOf<String>()
        for (i in 0 until stringCount) {
            buffer.position(dataStart + offsets[i])
            strings.add(readString(buffer, isUTF8))
        }

        var modified = false
        for (i in strings.indices) {
            replacementMap[strings[i]]?.let {
                LogCatcher.d("ProviderAuthReplacer", "Replacing: ${strings[i]} -> $it")
                strings[i] = it
                modified = true
            }
        }

        if (!modified) {
            LogCatcher.w("ProviderAuthReplacer", "No matching string found for replacement")
            return
        }

        val newStringData = buildStringPool(strings, isUTF8)
        val output = ByteArrayOutputStream()

        output.write(data, 0, 8)

        val header = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(CHUNK_STRING_POOL)
            putInt(28 + (stringCount * 4) + newStringData.size)
            putInt(stringCount)
            putInt(styleCount)
            putInt(flags)
            putInt(28 + (stringCount * 4))
            putInt(0)
        }
        output.write(header.array())

        var currentOffset = 0
        val offsetsBuffer = ByteBuffer.allocate(stringCount * 4).order(ByteOrder.LITTLE_ENDIAN)
        val tempStrings = ByteArrayOutputStream()

        for (str in strings) {
            offsetsBuffer.putInt(currentOffset)
            val strBytes = if (isUTF8) str.toByteArray(StandardCharsets.UTF_8) else str.toByteArray(StandardCharsets.UTF_16LE)
            
            if (isUTF8) {
                tempStrings.write(str.length and 0xFF)
                tempStrings.write(strBytes.size and 0xFF)
                tempStrings.write(strBytes)
                tempStrings.write(0)
                currentOffset += 2 + strBytes.size + 1
            } else {
                tempStrings.write(str.length and 0xFF)
                tempStrings.write((str.length shr 8) and 0xFF)
                tempStrings.write(strBytes)
                tempStrings.write(0); tempStrings.write(0)
                currentOffset += 2 + strBytes.size + 2
            }
        }

        while (currentOffset % 4 != 0) { tempStrings.write(0); currentOffset++ }

        output.write(offsetsBuffer.array())
        output.write(tempStrings.toByteArray())
        output.write(data, stringPoolStart + chunkSize, data.size - (stringPoolStart + chunkSize))

        val finalData = output.toByteArray()
        ByteBuffer.wrap(finalData).order(ByteOrder.LITTLE_ENDIAN).putInt(4, finalData.size)

        axmlFile.writeBytes(finalData)
        LogCatcher.i("ProviderAuthReplacer", "Provider authority replacement completed")
    }

    private fun readString(buffer: ByteBuffer, isUTF8: Boolean): String = try {
        if (isUTF8) {
            val len1 = buffer.get().toInt() and 0xFF
            val len = if (len1 and 0x80 != 0) ((len1 and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF) else len1
            val len2 = buffer.get().toInt() and 0xFF
            val encodedLen = if (len2 and 0x80 != 0) ((len2 and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF) else len2
            val strBytes = ByteArray(encodedLen).also { buffer.get(it) }
            String(strBytes, StandardCharsets.UTF_8)
        } else {
            val len = buffer.short.toInt() and 0xFFFF
            val strBytes = ByteArray(len * 2).also { buffer.get(it) }
            String(strBytes, StandardCharsets.UTF_16LE)
        }
    } catch (e: Exception) {
        LogCatcher.e("ProviderAuthReplacer", "Failed to read string", e)
        ""
    }

    private fun buildStringPool(strings: List<String>, isUTF8: Boolean): ByteArray {
        val poolData = ByteArrayOutputStream()
        for (str in strings) {
            val strBytes = if (isUTF8) str.toByteArray(StandardCharsets.UTF_8) else str.toByteArray(StandardCharsets.UTF_16LE)
            if (isUTF8) {
                poolData.write(str.length and 0xFF)
                poolData.write(strBytes.size and 0xFF)
                poolData.write(strBytes)
                poolData.write(0)
            } else {
                poolData.write(str.length and 0xFF)
                poolData.write((str.length shr 8) and 0xFF)
                poolData.write(strBytes)
                poolData.write(0); poolData.write(0)
            }
        }
        while (poolData.size() % 4 != 0) poolData.write(0)
        return poolData.toByteArray()
    }

    fun fixProviderConflicts(manifestFile: File, newPackageName: String) {
        try {
            LogCatcher.i("ProviderAuthReplacer", "Checking Provider conflicts...")
            val authorities = scanProviderAuthorities(manifestFile)
            val replacements = authorities.filter { it.contains("com.web.webapp") }
                .associateWith { it.replace("com.web.webapp", newPackageName) }
            
            if (replacements.isNotEmpty()) {
                batchReplaceStringInAXML(manifestFile, replacements)
            }
        } catch (e: Exception) {
            LogCatcher.e("ProviderAuthReplacer", "Fix failed", e)
        }
    }
    /*
    fun fixProviderConflicts(manifestFile: File, newPackageName: String) {
        try {
            LogCatcher.i("ProviderAuthReplacer", "Starting to check for Provider conflicts...")
            val authorities = scanProviderAuthorities(manifestFile)
            val replacements = mutableMapOf<String, String>()
            
            for (auth in authorities) {
                if (auth.contains("com.web.webapp")) {
                    replacements[auth] = auth.replace("com.web.webapp", newPackageName)
                }
            }

            if (replacements.isNotEmpty()) {
                batchReplaceStringInAXML(manifestFile, replacements)
                LogCatcher.i("ProviderAuthReplacer", "Provider conflict fix completed")
            }
        } catch (e: Exception) {
            LogCatcher.e("ProviderAuthReplacer", "Failed to fix Provider conflicts", e)
        }
    }
    */
}
