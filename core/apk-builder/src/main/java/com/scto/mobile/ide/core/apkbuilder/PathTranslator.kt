package com.scto.mobile.ide.core.apkbuilder

object PathTranslator {

    fun toSandboxPath(hostPath: String): String {
        val trimmed = hostPath.trim()
        return when {
            trimmed.startsWith("/storage/emulated/0/") -> "/sdcard/" + trimmed.removePrefix("/storage/emulated/0/")
            trimmed.startsWith("/storage/emulated/") -> {
                val rest = trimmed.substringAfter("/storage/emulated/")
                val pathAfterUser = rest.substringAfter("/")
                "/sdcard/$pathAfterUser"
            }
            else -> trimmed
        }
    }

    fun toHostPath(sandboxPath: String): String {
        val trimmed = sandboxPath.trim()
        return when {
            trimmed.startsWith("/sdcard/") -> "/storage/emulated/0/" + trimmed.removePrefix("/sdcard/")
            else -> trimmed
        }
    }
}
