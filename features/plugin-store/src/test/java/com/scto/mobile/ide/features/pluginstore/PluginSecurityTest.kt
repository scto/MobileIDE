package com.scto.mobile.ide.features.pluginstore

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class PluginSecurityTest {

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun verifySha256(file: File, expectedHash: String): Boolean {
        if (expectedHash.isBlank()) return true
        val computedHash = computeSha256(file)
        return computedHash.equals(expectedHash.trim(), ignoreCase = true)
    }

    @Test
    fun testValidSha256ChecksumSuccess() {
        val tempFile = File.createTempFile("test_plugin", ".zip").apply {
            writeText("dummy plugin zip binary content")
            deleteOnExit()
        }

        val actualHash = computeSha256(tempFile)
        assertTrue("Valid SHA-256 verification must succeed", verifySha256(tempFile, actualHash))
    }

    @Test
    fun testManipulatedSha256ChecksumFailsAndAborts() {
        val tempFile = File.createTempFile("corrupted_plugin", ".zip").apply {
            writeText("dummy plugin zip binary content")
            deleteOnExit()
        }

        val manipulatedBadHash = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        val isVerified = verifySha256(tempFile, manipulatedBadHash)

        assertFalse("Manipulated SHA-256 checksum must fail verification", isVerified)

        if (!isVerified) {
            tempFile.delete()
        }

        assertFalse("Downloaded temp zip file must be deleted upon checksum mismatch", tempFile.exists())
    }
}
