package com.mobileide.data

import com.mobileide.common.GitStatusResult

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import java.io.File
import javax.inject.Inject

/**
 * Das Repository für alle Git-Operationen.
 * Fügt Push, Pull und Unstage-Funktionen hinzu.
 */
interface GitRepository {
    suspend fun cloneRepository(url: String, localPath: String): Result<Unit>
    suspend fun getStatus(localPath: String): Result<GitStatusResult>
    suspend fun add(localPath: String, filePattern: String): Result<Unit>
    suspend fun unstage(localPath: String, filePattern: String): Result<Unit>
    suspend fun commit(localPath: String, message: String): Result<String>
    suspend fun push(localPath: String): Result<String>
    suspend fun pull(localPath: String): Result<String>
}

class GitRepositoryImpl @Inject constructor() : GitRepository {

    // WICHTIG: In einer echten App müssen diese sicher gespeichert und abgerufen werden!
    private val credentialsProvider = UsernamePasswordCredentialsProvider("USERNAME", "PASSWORD/TOKEN")

    override suspend fun cloneRepository(url: String, localPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            File(localPath).deleteRecursively()
            Git.cloneRepository()
                .setURI(url)
                .setDirectory(File(localPath))
                .setCredentialsProvider(credentialsProvider)
                .call()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStatus(localPath: String): Result<GitStatusResult> = withContext(Dispatchers.IO) {
        try {
            val git = Git.open(File(localPath))
            val status = git.status().call()
            val result = GitStatusResult(
                branch = git.repository.branch,
                modified = status.modified,
                untracked = status.untracked,
                added = status.added,
                changed = status.changed,
                removed = status.removed,
                conflicting = status.conflicting
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun add(localPath: String, filePattern: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Git.open(File(localPath)).add().addFilepattern(filePattern).call()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unstage(localPath: String, filePattern: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Git.open(File(localPath)).reset().addPath(filePattern).call()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun commit(localPath: String, message: String): Result<String> = withContext(Dispatchers.IO) {
        if (message.isBlank()) return@withContext Result.failure(Exception("Commit message cannot be empty."))
        try {
            val git = Git.open(File(localPath))
            val revCommit = git.commit().setMessage(message).call()
            Result.success(revCommit.name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun push(localPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val results = Git.open(File(localPath)).push().setCredentialsProvider(credentialsProvider).call()
            val messages = results.joinToString("\n") { it.messages }
            Result.success(messages.ifBlank { "Push successful." })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pull(localPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = Git.open(File(localPath)).pull().setCredentialsProvider(credentialsProvider).call()
            Result.success("Pull successful: ${result.fetchResult.trackingRefUpdates}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

