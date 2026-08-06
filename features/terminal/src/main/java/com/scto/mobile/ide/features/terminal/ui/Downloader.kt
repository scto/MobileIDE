/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  Thomas Schmid  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.scto.mobile.ide.features.terminal.ui

import android.content.Context
import android.os.Build
import com.scto.mobile.ide.core.common.Constants
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Downloader for proot binary and Linux RootFS archives (Ubuntu, Debian). Detects CPU architecture automatically and
 * fetches the correct variant with OkHttp3, exponential backoff retries, atomic downloads (.part file), and mirror fallbacks.
 */
object Downloader {

    // ─────────────────────────────────────────────────────────────────────────
    // Architecture detection
    // ─────────────────────────────────────────────────────────────────────────

    enum class Arch(
        val abiName: String,
        val prootArch: String,
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

    /** Returns primary and fallback download URLs for the given [distro] and [arch]. */
    fun getRootFsUrls(distro: String, arch: Arch): List<String> {
        val primary = getRootFsUrl(distro, arch)
        // Fallback mirror using scto repository
        val fallbackBase = "https://raw.githubusercontent.com/scto/Karbon-PackagesX/main"
        val fallback = when (distro.lowercase()) {
            "ubuntu" -> when (arch) {
                Arch.ARM64 -> "$fallbackBase/ubuntu/ubuntu-base-24.04.3-base-arm64.tar.gz"
                Arch.ARM32 -> "$fallbackBase/ubuntu/ubuntu-base-24.04.3-base-armhf.tar.gz"
                Arch.X86_64 -> "$fallbackBase/ubuntu/ubuntu-base-24.04.3-base-amd64.tar.gz"
                Arch.X86 -> "$fallbackBase/ubuntu/ubuntu-base-24.04.3-base-armhf.tar.gz"
            }
            "debian" -> when (arch) {
                Arch.ARM64 -> "$fallbackBase/debian/debian-rootfs-arm64.tar.xz"
                Arch.ARM32 -> "$fallbackBase/debian/debian-rootfs-amdhf.tar.xz"
                Arch.X86_64 -> "$fallbackBase/debian/debian-rootfs-amd64.tar.xz"
                Arch.X86 -> "$fallbackBase/debian/debian-rootfs-amdhf.tar.xz"
            }
            else -> primary
        }
        return if (primary != fallback) listOf(primary, fallback) else listOf(primary)
    }

    /** Returns the primary download URL for the given [distro] and [arch]. */
    fun getRootFsUrl(distro: String, arch: Arch): String {
        return when (distro.lowercase()) {
            "ubuntu" -> {
                when (arch) {
                    Arch.ARM64 -> Constants.UBUNTU_ARM64
                    Arch.ARM32 -> Constants.UBUNTU_ARM
                    Arch.X86_64 -> Constants.UBUNTU_X64
                    Arch.X86 -> Constants.UBUNTU_ARM
                }
            }
            "debian" -> {
                when (arch) {
                    Arch.ARM64 -> Constants.DEBIAN_ARM64
                    Arch.ARM32 -> Constants.DEBIAN_ARM
                    Arch.X86_64 -> Constants.DEBIAN_X64
                    Arch.X86 -> Constants.DEBIAN_ARM
                }
            }
            else -> throw IllegalArgumentException("Unsupported distro: $distro")
        }
    }

    fun getProotUrl(arch: Arch): String =
        when (arch) {
            Arch.ARM64 -> Constants.PROOT_ARM64
            Arch.ARM32 -> Constants.PROOT_ARM
            Arch.X86_64 -> Constants.PROOT_X64
            Arch.X86 -> Constants.PROOT_ARM
        }

    fun getTallocUrl(arch: Arch): String =
        when (arch) {
            Arch.ARM64 -> Constants.TALLOC_ARM64
            Arch.ARM32 -> Constants.TALLOC_ARM
            Arch.X86_64 -> Constants.TALLOC_X64
            Arch.X86 -> Constants.TALLOC_ARM
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Progress callback
    // ─────────────────────────────────────────────────────────────────────────

    fun interface ProgressCallback {
        fun onProgress(downloaded: Long, total: Long)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OkHttp3 Client & Download Engine
    // ─────────────────────────────────────────────────────────────────────────

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private const val BUFFER_SIZE = 64 * 1024 // 64 KB
    private const val MAX_RETRIES = 3

    /**
     * Downloads [url] into [destFile], reporting progress via [onProgress].
     * Uses OkHttp3, atomic `.part` files, Range resume support, SHA-256 verification,
     * and exponential backoff retries.
     *
     * @throws IOException on network or I/O errors.
     * @throws SecurityException when the checksum does not match.
     */
    @Throws(IOException::class, SecurityException::class)
    fun download(
        url: String,
        destFile: File,
        expectedSha256: String? = null,
        minSizeBytes: Long = 100_000L,
        onProgress: ProgressCallback? = null
    ) {
        val urls = listOf(url)
        downloadWithMirrors(urls, destFile, expectedSha256, minSizeBytes, onProgress)
    }

    /**
     * Downloads from a list of mirror [urls] into [destFile], switching mirrors if a mirror fails after retries.
     */
    @Throws(IOException::class, SecurityException::class)
    fun downloadWithMirrors(
        urls: List<String>,
        destFile: File,
        expectedSha256: String? = null,
        minSizeBytes: Long = 100_000L,
        onProgress: ProgressCallback? = null
    ) {
        var lastException: Exception? = null

        for ((index, currentUrl) in urls.withIndex()) {
            Timber.tag("Downloader").i("Versuche Download von Mirror ${index + 1}/${urls.size}: $currentUrl")
            try {
                downloadSingleUrlWithRetry(currentUrl, destFile, expectedSha256, minSizeBytes, onProgress)
                return // Success!
            } catch (e: Exception) {
                lastException = e
                Timber.tag("Downloader").w("Mirror ${index + 1} fehlgeschlagen ($currentUrl): ${e.message}")
            }
        }

        throw lastException ?: IOException("Download von allen verfügbaren Mirrors fehlgeschlagen.")
    }

    private fun downloadSingleUrlWithRetry(
        url: String,
        destFile: File,
        expectedSha256: String?,
        minSizeBytes: Long,
        onProgress: ProgressCallback?
    ) {
        destFile.parentFile?.mkdirs()
        val partFile = File(destFile.parentFile, "${destFile.name}.part")

        var attempt = 0
        while (attempt <= MAX_RETRIES) {
            try {
                attempt++
                executeDownloadAttempt(url, destFile, partFile, expectedSha256, minSizeBytes, onProgress)
                return // Success
            } catch (e: Exception) {
                val isRetryable = e is SocketTimeoutException || e is SocketException || e is IOException
                if (attempt <= MAX_RETRIES && isRetryable) {
                    val backoffMs = 3_000L * attempt
                    Timber.tag("Downloader").w(
                        "Download-Fehler (Versuch $attempt/$MAX_RETRIES): ${e.message}. Erneuter Versuch in ${backoffMs / 1000}s..."
                    )
                    Timber.tag("Downloader").w(e, "Download attempt $attempt failed, retrying in ${backoffMs}ms")
                    try {
                        Thread.sleep(backoffMs)
                    } catch (interrupted: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Download unterbrochen", interrupted)
                    }
                } else {
                    Timber.tag("Downloader").e("Download endgültig fehlgeschlagen nach $attempt Versuchen: ${e.message}")
                    partFile.delete()
                    throw e
                }
            }
        }
    }

    private fun executeDownloadAttempt(
        url: String,
        destFile: File,
        partFile: File,
        expectedSha256: String?,
        minSizeBytes: Long,
        onProgress: ProgressCallback?
    ) {
        val existingBytes = if (partFile.exists()) partFile.length() else 0L

        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "MobileIDE/1.0 (Android)")

        if (existingBytes > 0L) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val request = requestBuilder.build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful && response.code != 206) {
            response.close()
            if (response.code in 500..599) {
                throw IOException("Server-Fehler HTTP ${response.code} von $url")
            }
            throw IOException("Download fehlgeschlagen mit HTTP ${response.code}: $url")
        }

        val body = response.body ?: throw IOException("Leere HTTP-Antwort von $url")
        val isPartial = response.code == 206
        var downloaded = if (isPartial) existingBytes else 0L

        if (!isPartial && existingBytes > 0L) {
            partFile.delete()
        }

        val contentLength = body.contentLength()
        val totalBytes = if (contentLength > 0L) downloaded + contentLength else -1L

        val digest = if (expectedSha256 != null) MessageDigest.getInstance("SHA-256") else null

        try {
            body.byteStream().use { input ->
                FileOutputStream(partFile, isPartial).use { output ->
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
            response.close()
        }

        // SHA-256 Checksum Verification
        if (expectedSha256 != null && digest != null) {
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(expectedSha256, ignoreCase = true)) {
                partFile.delete()
                throw SecurityException("SHA-256 Prüfsummenfehler für $url\n  Erwartet: $expectedSha256\n  Erhalten: $actual")
            }
        }

        // Atomic move from .part file to destFile
        destFile.delete()
        var moved = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Files.move(partFile.toPath(), destFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                moved = true
            } catch (e: Exception) {
                Timber.tag("Downloader").w(e, "Atomic move failed, falling back to file rename/copy")
            }
        }

        if (!moved) {
            if (!partFile.renameTo(destFile)) {
                partFile.copyTo(destFile, overwrite = true)
                partFile.delete()
            }
        }

        // Plausible Size Check
        if (!destFile.exists() || destFile.length() < minSizeBytes) {
            val actualSize = if (destFile.exists()) destFile.length() else 0L
            destFile.delete()
            throw IOException("Datei nach Download unvollständig oder zu klein ($actualSize Bytes, Erwartet >= $minSizeBytes Bytes).")
        }

        Timber.tag("Downloader").i("Download erfolgreich abgeschlossen: ${destFile.name} (${destFile.length()} Bytes)")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // High-level helpers used by SetupWorker
    // ─────────────────────────────────────────────────────────────────────────

    fun downloadTalloc(context: Context, force: Boolean = false, onProgress: ProgressCallback? = null) {
        val arch = detectArch()
        val destFile = File(context.filesDir, "libtalloc.so.2")
        if (!force && destFile.exists() && destFile.length() >= 10_000L) {
            Timber.tag("Downloader").i("libtalloc already exists. Skipping.")
            return
        }

        val url = getTallocUrl(arch)
        download(url, destFile, minSizeBytes = 10_000L, onProgress = onProgress)
    }

    fun downloadProot(context: Context, force: Boolean = false, onProgress: ProgressCallback? = null) {
        val arch = detectArch()
        val destFile = File(context.filesDir, "proot")
        if (!force && destFile.exists() && destFile.length() >= 50_000L) {
            Timber.tag("Downloader").i("proot binary already exists. Skipping.")
            return
        }

        val url = getProotUrl(arch)
        download(url, destFile, minSizeBytes = 50_000L, onProgress = onProgress)
        destFile.setExecutable(true)

        val binDir = File(context.filesDir.parentFile!!, "local/bin").also { it.mkdirs() }
        destFile.copyTo(File(binDir, "proot"), overwrite = true)
        File(binDir, "proot").setExecutable(true)
    }

    fun downloadRootFs(
        context: Context,
        distro: String,
        force: Boolean = false,
        onProgress: ProgressCallback? = null,
    ): File {
        val arch = detectArch()
        val urls = getRootFsUrls(distro, arch)
        val destFile = File(context.filesDir, "${distro.lowercase()}.tar.gz")

        // RootFS minimum size: 1 MB
        if (!force && destFile.exists() && destFile.length() >= 1_000_000L) {
            Timber.tag("Downloader").i("rootfs archive already exists. Skipping.")
            return destFile
        }

        downloadWithMirrors(urls, destFile, minSizeBytes = 1_000_000L, onProgress = onProgress)

        return destFile
    }

    fun archDescription(): String {
        val arch = detectArch()
        return "${arch.abiName} (${arch.prootArch})"
    }
}
