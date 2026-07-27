package com.scto.mobile.ide.ui.editor.git.conflict

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import timber.log.Timber
import java.io.File

object GitConflictManager {

    suspend fun getConflictingFiles(projectPath: String): List<String> = withContext(Dispatchers.IO) {
        val rootDir = File(projectPath)
        if (!File(rootDir, ".git").exists()) return@withContext emptyList()

        try {
            val git = Git.open(rootDir)
            val status = git.status().call()
            val conflictFiles = mutableSetOf<String>()

            // 1. Files reported as conflicting by JGit
            conflictFiles.addAll(status.conflicting)

            // 2. Scan modified/untracked files for conflict markers
            (status.modified + status.untracked + status.changed).forEach { relPath ->
                val file = File(rootDir, relPath)
                if (file.isFile && file.length() < 2 * 1024 * 1024) {
                    try {
                        val content = file.readText()
                        if (GitConflictParser.hasConflictMarkers(content)) {
                            conflictFiles.add(relPath)
                        }
                    } catch (ignored: Exception) {}
                }
            }

            git.close()
            conflictFiles.toList().sorted()
        } catch (e: Exception) {
            Timber.e(e, "Error getting conflicting files")
            emptyList()
        }
    }

    suspend fun resolveFileConflict(projectPath: String, relativeFilePath: String, resolvedContent: String): Boolean = withContext(Dispatchers.IO) {
        val rootDir = File(projectPath)
        try {
            val targetFile = File(rootDir, relativeFilePath)
            targetFile.writeText(resolvedContent)

            val git = Git.open(rootDir)
            git.add().addFilepattern(relativeFilePath).call()
            git.close()
            true
        } catch (e: Exception) {
            Timber.e(e, "Error resolving conflict for $relativeFilePath")
            false
        }
    }

    suspend fun completeMerge(projectPath: String, commitMessage: String, authorName: String = "MobileIDE User", authorEmail: String = "dev@mobileide.app"): Boolean = withContext(Dispatchers.IO) {
        val rootDir = File(projectPath)
        try {
            val git = Git.open(rootDir)
            val person = PersonIdent(authorName, authorEmail)
            git.commit().setMessage(commitMessage).setAuthor(person).setCommitter(person).call()
            git.close()
            true
        } catch (e: Exception) {
            Timber.e(e, "Error completing merge commit")
            false
        }
    }

    suspend fun abortMerge(projectPath: String): Boolean = withContext(Dispatchers.IO) {
        val rootDir = File(projectPath)
        try {
            val git = Git.open(rootDir)
            git.checkout().setAllPaths(true).call()
            git.close()
            true
        } catch (e: Exception) {
            Timber.e(e, "Error aborting merge")
            false
        }
    }
}
