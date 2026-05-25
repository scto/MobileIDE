// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.build

import com.mobile.ide.core.utils.LogCatcher

import java.io.*
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Ultimate corrected ZipAligner (unbuffered, precise calculation)
 */
object ZipAligner {

    private const val ALIGNMENT = 4

    fun align(inputFile: File, outputFile: File) {
        ZipFile(inputFile).use { zipFile ->
            ByteCountingOutputStream(FileOutputStream(outputFile)).use { counter ->
                ZipOutputStream(counter).use { zos ->
                    zos.setLevel(9)

                    val entries = zipFile.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name
                        val data = readEntryData(zipFile, entry)

                        val newEntry = ZipEntry(name)
                        val isArsc = name == "resources.arsc"

                        if (isArsc || entry.method == ZipEntry.STORED) {
                            newEntry.method = ZipEntry.STORED
                            newEntry.size = data.size.toLong()
                            newEntry.compressedSize = data.size.toLong()
                            newEntry.crc = CRC32().apply { update(data) }.value
                        } else {
                            newEntry.method = ZipEntry.DEFLATED
                        }

                        if (newEntry.method == ZipEntry.STORED) {
                            val currentPos = counter.bytesWritten
                            val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
                            val headerLenWithoutPadding = 30 + nameBytes.size + 4
                            val predictedDataStart = currentPos + headerLenWithoutPadding
                            
                            var padding = ((ALIGNMENT - (predictedDataStart % ALIGNMENT)) % ALIGNMENT).toInt()
                            
                            // Force 4-byte padding if 0 to maintain structural integrity
                            if (padding == 0) padding = 4

                            if (isArsc) {
                                LogCatcher.i("ZipAligner", "Aligning resources.arsc | StartPos: $currentPos | Padding: $padding")
                            }

                            newEntry.extra = ByteArray(4 + padding).apply {
                                this[0] = 0x35
                                this[1] = 0xD9.toByte()
                                this[2] = padding.toByte()
                                this[3] = 0
                            }
                        }

                        zos.putNextEntry(newEntry)
                        zos.write(data)
                        zos.closeEntry()
                    }
                    zos.finish()
                }
            }
        }
    }

    private fun readEntryData(zf: ZipFile, entry: ZipEntry): ByteArray {
        return zf.getInputStream(entry).use { it.readBytes() }
    }

    // Counting stream (directly connected to FileOutputStream)
    private class ByteCountingOutputStream(out: OutputStream) : FilterOutputStream(out) {
        var bytesWritten: Long = 0
            private set

        override fun write(b: Int) {
            out.write(b)
            bytesWritten++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            bytesWritten += len.toLong()
        }
    }
}
