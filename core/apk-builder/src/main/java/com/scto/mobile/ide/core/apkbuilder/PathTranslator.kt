package com.scto.mobile.ide.core.apkbuilder

object PathTranslator {

    fun toSandboxPath(hostPath: String): String {
        val trimmed = hostPath.trim()
        val result = when {
            trimmed.startsWith("/storage/emulated/0/") -> "/sdcard/" + trimmed.removePrefix("/storage/emulated/0/")
            trimmed.startsWith("/storage/emulated/") -> {
                val rest = trimmed.substringAfter("/storage/emulated/")
                val pathAfterUser = rest.substringAfter("/")
                "/sdcard/$pathAfterUser"
            }
            else -> trimmed
        }
        val inputSegments = trimmed.split('/').filter { it.isNotEmpty() }
        val resultSegments = result.split('/').filter { it.isNotEmpty() }
        if (inputSegments.lastOrNull() != resultSegments.lastOrNull()) {
            throw IllegalStateException("PathTranslator assertion error: Last path segment modified! Input: '$trimmed', Output: '$result'")
        }
        return result
    }

    fun toHostPath(sandboxPath: String): String {
        val trimmed = sandboxPath.trim()
        val result = when {
            trimmed.startsWith("/sdcard/") -> "/storage/emulated/0/" + trimmed.removePrefix("/sdcard/")
            else -> trimmed
        }
        val inputSegments = trimmed.split('/').filter { it.isNotEmpty() }
        val resultSegments = result.split('/').filter { it.isNotEmpty() }
        if (inputSegments.lastOrNull() != resultSegments.lastOrNull()) {
            throw IllegalStateException("PathTranslator assertion error: Last path segment modified! Input: '$trimmed', Output: '$result'")
        }
        return result
    }
}
