/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  Thomas Schmid  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.scto.mobile.ide.ui.terminal

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import com.scto.mobile.ide.core.utils.LogCatcher

/**
 * Downloader for proot binary and Linux RootFS archives (Ubuntu, Debian). Detects CPU architecture automatically and
 * fetches the correct variant.
 */
object Downloader {

    // ─────────────────────────────────────────────────────────────────────────
    // Architecture detection
    // ─────────────────────────────────────────────────────────────────────────

    enum class Arch(
        /** ABI string as used in Android JNI dirs */
        val abiName: String,
        /** proot URL suffix (termux-packages release naming) */
        val prootArch: String,
        /** Architecture string used by Ubuntu/Debian CDN */
        val debianArch: String,
    ) {
        ARM64("arm64-v8a", "aarch64", "arm64"),
        ARM32("armeabi-v7a", "arm", "armhf"),
        X86_64("x86_64", "x86_64", "amd64"),
        X86("x86", "i686", "i386"),
    }

    /** Returns the best matching [Arch] for the running device. */
    fun detectArch(): Arch {
        val supported = Build.SUPPORTED_ABIS.toList()
        return when {
            supported.any { it == "arm64-v8a" } -> Arch.ARM64
            supported.any { it == "x86_64" } -> Arch.X86_64
            supported.any { it == "armeabi-v7a" } -> Arch.ARM32
            else -> Arch.X86
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RootFS URL table (termux/proot-distro releases)
    // ─────────────────────────────────────────────────────────────────────────

    private const val PROOT_DISTRO_BASE = "https://github.com/termux/proot-distro/releases/download"

    private val DISTRO_VERSIONS = mapOf("ubuntu" to "4.30.1", "debian" to "4.30.1")

    /**
     * Returns the download URL for the given [distro] and [arch].
     *
     * Example: ubuntu → https://.../v4.30.1/ubuntu-aarch64-pd-v4.30.1.tar.xz debian →
     * https://.../v4.30.1/debian-aarch64-pd-v4.30.1.tar.xz
     */
    fun getRootFsUrl(distro: String, arch: Arch): String {
        val version =
            DISTRO_VERSIONS[distro.lowercase()] ?: throw IllegalArgumentException("Unsupported distro: $distro")
        val tag = "v$version"
        val file = "${distro.lowercase()}-${arch.prootArch}-pd-v${version}.tar.xz"
        return "$PROOT_DISTRO_BASE/$tag/$file"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // proot binary URLs (static builds from termux-packages)
    // ─────────────────────────────────────────────────────────────────────────

    private const val PROOT_RELEASE_BASE = "https://github.com/termux-play-store/termux-packages/releases/download"

    private const val PROOT_TAG = "proot-2025.01.15-r2"
    private const val PROOT_VERSION = "5.1.107-66"

    /** Returns the URL for the proot static binary for [arch]. */
    fun getProotUrl(arch: Arch): String =
        "$PROOT_RELEASE_BASE/$PROOT_TAG/libproot-loader-${arch.prootArch}-$PROOT_VERSION.so"

    // ─────────────────────────────────────────────────────────────────────────
    // Progress callback
    // ─────────────────────────────────────────────────────────────────────────

    /** Called periodically during a download. All values in bytes. */
    fun interface ProgressCallback {
        /**
         * @param downloaded bytes written so far
         * @param total total content length (-1 if unknown)
         */
        fun onProgress(downloaded: Long, total: Long)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Download engine
    // ─────────────────────────────────────────────────────────────────────────

    private const val CONNECT_TIMEOUT_MS = 30_000
    private const val READ_TIMEOUT_MS = 60_000
    private const val BUFFER_SIZE = 64 * 1024 // 64 KB

    /**
     * Downloads [url] into [destFile], reporting progress via [onProgress].
     * - Follows HTTP redirects automatically (up to 5 hops).
     * - Resumes partial downloads when the server supports `Range` requests.
     * - Verifies SHA-256 checksum when [expectedSha256] is non-null.
     *
     * @throws IOException on network or I/O errors.
     * @throws SecurityException when the checksum does not match.
     */
    @Throws(IOException::class, SecurityException::class)
    fun download(url: String, destFile: File, expectedSha256: String? = null, onProgress: ProgressCallback? = null) {
        LogCatcher.i("Downloader", "Starting download: URL=$url, Dest=${destFile.absolutePath}, expectedSha256=$expectedSha256")
        destFile.parentFile?.mkdirs()

        val existingBytes = if (destFile.exists()) destFile.length() else 0L
        var downloaded = existingBytes

        val digest = if (expectedSha256 != null) MessageDigest.getInstance("SHA-256") else null

        var resolvedUrl = url
        var connection = openConnection(resolvedUrl, if (existingBytes > 0) existingBytes else -1L)

        // Follow redirects manually so we can keep the Range header.
        repeat(5) {
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location") ?: return@repeat
                connection.disconnect()
                resolvedUrl = if (location.startsWith("http")) location else "https://${URL(resolvedUrl).host}$location"
                connection = openConnection(resolvedUrl, if (existingBytes > 0) existingBytes else -1L)
            }
        }

        val responseCode = connection.responseCode
        val resuming = responseCode == HttpURLConnection.HTTP_PARTIAL

        // Server did not honour Range → restart from scratch.
        if (!resuming && existingBytes > 0) {
            destFile.delete()
            downloaded = 0L
            connection.disconnect()
            connection = openConnection(resolvedUrl, -1L)
        }

        val totalBytes =
            when {
                resuming -> existingBytes + (connection.contentLengthLong.takeIf { it > 0 } ?: -1L)
                else -> connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            }

        try {
            connection.inputStream.use { input ->
                FileOutputStream(destFile, resuming).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        digest?.update(buffer, 0, read)
                        downloaded += read
                        onProgress?.onProgress(downloaded, totalBytes)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        // SHA-256 verification
        if (expectedSha256 != null && digest != null) {
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(expectedSha256, ignoreCase = true)) {
                LogCatcher.e("Downloader", "SHA-256 verification failed for $url. Expected: $expectedSha256, Actual: $actual")
                destFile.delete()
                throw SecurityException("SHA-256 mismatch for $url\n  Expected: $expectedSha256\n  Actual:   $actual")
            }
        }
        LogCatcher.i("Downloader", "Download completed successfully: ${destFile.absolutePath} (${destFile.length()} bytes)")
    }

    private fun openConnection(url: String, rangeStart: Long): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "MobileIDE/1.0 (Android)")
        if (rangeStart > 0) {
            conn.setRequestProperty("Range", "bytes=$rangeStart-")
        }
        conn.connect()
        return conn
    }

    // ─────────────────────────────────────────────────────────────────────────
    // High-level helpers used by SetupWorker
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Downloads the proot binary for the current device architecture into [context]'s files directory and marks it as
     * executable.
     *
     * The binary is cached: if it already exists and [force] is false the download is skipped.
     */
    fun downloadProot(context: Context, force: Boolean = false, onProgress: ProgressCallback? = null) {
        val arch = detectArch()
        val destFile = File(context.filesDir, "proot")
        LogCatcher.i("Downloader", "downloadProot: arch=$arch, dest=${destFile.absolutePath}, force=$force")

        if (!force && destFile.exists() && destFile.length() > 0L) {
            LogCatcher.i("Downloader", "downloadProot: proot binary already exists. Skipping.")
            return
        }

        val url = getProotUrl(arch)
        download(url, destFile, onProgress = onProgress)
        destFile.setExecutable(true)

        // Also place a copy in local/bin for the shell environment.
        val binDir = File(context.filesDir.parentFile!!, "local/bin").also { it.mkdirs() }
        destFile.copyTo(File(binDir, "proot"), overwrite = true)
        File(binDir, "proot").setExecutable(true)
    }

    /**
     * Downloads the rootfs archive for [distro] into [context]'s files directory.
     *
     * The archive is cached: if it already exists and [force] is false the download is skipped.
     *
     * @return the downloaded (or cached) archive [File].
     */
    fun downloadRootFs(
        context: Context,
        distro: String,
        force: Boolean = false,
        onProgress: ProgressCallback? = null,
    ): File {
        val arch = detectArch()
        val url = getRootFsUrl(distro, arch)
        // Store as <distro>.tar.gz so SetupWorker / init-host.sh can find it.
        val destFile = File(context.filesDir, "${distro.lowercase()}.tar.gz")
        LogCatcher.i("Downloader", "downloadRootFs: distro=$distro, arch=$arch, URL=$url, dest=${destFile.absolutePath}, force=$force")

        if (!force && destFile.exists() && destFile.length() > 0L) {
            LogCatcher.i("Downloader", "downloadRootFs: rootfs archive already exists. Skipping.")
            return destFile
        }

        // Download into a .tmp file first; rename on success.
        val tmpFile = File(context.filesDir, "${distro.lowercase()}.tar.xz.tmp")
        tmpFile.delete()

        download(url, tmpFile, onProgress = onProgress)

        destFile.delete()
        if (!tmpFile.renameTo(destFile)) {
            try {
                tmpFile.copyTo(destFile, overwrite = true)
                tmpFile.delete()
            } catch (copyEx: Exception) {
                copyEx.printStackTrace()
            }
        }
        return destFile
    }

    /** Returns a human-readable description of the current device architecture, e.g. "arm64-v8a (aarch64)". */
    fun archDescription(): String {
        val arch = detectArch()
        return "${arch.abiName} (${arch.prootArch})"
    }
}
