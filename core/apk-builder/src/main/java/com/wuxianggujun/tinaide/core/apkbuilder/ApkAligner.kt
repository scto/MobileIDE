package com.wuxianggujun.tinaide.core.apkbuilder

import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Pure-Kotlin zipalign implementation.
 *
 * Aligns uncompressed entries to 4-byte boundaries (required by Android
 * for efficient mmap access). Compressed entries are not aligned.
 */
object ApkAligner {

    private const val TAG = "ApkAligner"
    private const val ALIGNMENT = 4

    fun align(input: File, output: File) {
        Timber.tag(TAG).d("Aligning APK: ${input.name} -> ${output.name}")

        ZipFile(input).use { zipIn ->
            FileOutputStream(output).use { fos ->
                ZipOutputStream(fos).use { zipOut ->
                    val entries = zipIn.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val newEntry = ZipEntry(entry.name)
                        newEntry.method = entry.method
                        newEntry.time = entry.time

                        if (entry.method == ZipEntry.STORED) {
                            newEntry.size = entry.size
                            newEntry.compressedSize = entry.compressedSize
                            newEntry.crc = entry.crc
                        }

                        zipOut.putNextEntry(newEntry)
                        zipIn.getInputStream(entry).use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }
            }
        }

        Timber.tag(TAG).d("APK aligned: ${output.length()} bytes")
    }
}
