package com.scto.mobile.ide.core.common.utils

import java.io.File

object ProjectPaths {

    data class ResolvedPaths(
        val hostDir: File,
        val sandboxDir: File,
        val hostPath: String,
        val sandboxPath: String
    )

    fun toSandboxPath(hostPath: String): String {
        val trimmed = hostPath.trim()
        val cleaned = if (trimmed.contains(" ")) trimmed.replace("\\s+".toRegex(), "") else trimmed
        return when {
            cleaned.startsWith("/storage/emulated/0/") -> "/sdcard/" + cleaned.removePrefix("/storage/emulated/0/")
            cleaned.startsWith("/storage/emulated/") -> {
                val rest = cleaned.substringAfter("/storage/emulated/")
                val pathAfterUser = rest.substringAfter("/")
                "/sdcard/$pathAfterUser"
            }
            else -> cleaned
        }
    }

    fun toHostPath(sandboxPath: String): String {
        val trimmed = sandboxPath.trim()
        val cleaned = if (trimmed.contains(" ")) trimmed.replace("\\s+".toRegex(), "") else trimmed
        return when {
            cleaned.startsWith("/sdcard/") -> "/storage/emulated/0/" + cleaned.removePrefix("/sdcard/")
            else -> cleaned
        }
    }

    fun resolve(projectDir: File): ResolvedPaths {
        val rawPath = projectDir.absolutePath.trim()
        val hostPath = toHostPath(rawPath)
        val sandboxPath = toSandboxPath(rawPath)

        check(!sandboxPath.contains(" /")) {
            "Resolved sandbox path contains invalid space slash sequence: $sandboxPath"
        }

        return ResolvedPaths(
            hostDir = File(hostPath),
            sandboxDir = File(sandboxPath),
            hostPath = hostPath,
            sandboxPath = sandboxPath
        )
    }

    fun resolve(projectPath: String): ResolvedPaths = resolve(File(projectPath))
}
