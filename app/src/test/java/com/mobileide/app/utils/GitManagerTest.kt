package com.mobileide.app.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito.mock

class GitManagerTest {

    @Test
    fun isGitRepo_returnsTrue_whenDirectoryIsGitRepo() = runBlocking {
        // Create a temporary directory and initialize a git repo in it
        val tempDir = File.createTempFile("git-repo-test", "")
        tempDir.delete()
        tempDir.mkdir()

        try {
            // Initialize git repository
            ProcessBuilder("git", "init").directory(tempDir).start().waitFor()

            // Create a mock TermuxBridge (not actually used in isGitRepo currently)
            val termuxMock = mock(TermuxBridge::class.java)
            val gitManager = GitManager(termuxMock)

            val isRepo = gitManager.isGitRepo(tempDir.absolutePath)

            assertTrue("Expected to be a git repository", isRepo)
        } finally {
            // Clean up
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun isGitRepo_returnsFalse_whenDirectoryIsNotGitRepo() = runBlocking {
        // Create a temporary directory that is NOT a git repo
        val tempDir = File.createTempFile("git-repo-test", "")
        tempDir.delete()
        tempDir.mkdir()

        try {
            val termuxMock = mock(TermuxBridge::class.java)
            val gitManager = GitManager(termuxMock)

            val isRepo = gitManager.isGitRepo(tempDir.absolutePath)

            assertFalse("Expected NOT to be a git repository", isRepo)
        } finally {
            // Clean up
            tempDir.deleteRecursively()
        }
    }
}
