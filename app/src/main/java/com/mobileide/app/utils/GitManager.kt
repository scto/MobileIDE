package com.mobileide.app.utils

import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LogTag

import com.mobileide.app.data.LineType
import com.mobileide.app.data.TerminalLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class GitStatus(
    val branch: String = "unknown",
    val modified: List<String> = emptyList(),
    val untracked: List<String> = emptyList(),
    val staged: List<String> = emptyList(),
    val isRepo: Boolean = false
)

class GitManager(private val termux: TermuxBridge) {

    // ── Check if dir is a git repo ─────────────────────────────────────────────
    suspend fun isGitRepo(projectPath: String): Boolean {
        val result = runBlocking("git -C '$projectPath' rev-parse --is-inside-work-tree 2>/dev/null")
        return result.trim() == "true"
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    fun init(projectPath: String): Flow<TerminalLine> = flow {
        Logger.info(LogTag.GIT, "init: $projectPath")
        Logger.info(LogTag.GIT, "init: $projectPath")
        emit(TerminalLine("Initializing git repository…", LineType.INFO))
        termux.executeStream("git init '$projectPath'", projectPath).collect { emit(it) }
        // Create .gitignore for Android
        termux.executeStream("""
            cat > '$projectPath/.gitignore' << 'EOF'
.gradle/
build/
*.apk
*.aab
*.ap_
*.dex
local.properties
.DS_Store
captures/
.externalNativeBuild/
.cxx/
EOF
        """.trimIndent(), projectPath).collect { emit(it) }
        emit(TerminalLine("✓ .gitignore created", LineType.SUCCESS))
    }.flowOn(Dispatchers.IO)

    // ── Status ────────────────────────────────────────────────────────────────
    fun status(projectPath: String): Flow<TerminalLine> =
        termux.executeStream("git -C '$projectPath' status", projectPath)

    // ── Add all ───────────────────────────────────────────────────────────────
    fun addAll(projectPath: String): Flow<TerminalLine> = flow {
        Logger.info(LogTag.GIT, "addAll: $projectPath")
        emit(TerminalLine("Staging all changes…", LineType.INFO))
        termux.executeStream("git -C '$projectPath' add -A", projectPath).collect { emit(it) }
        termux.executeStream("git -C '$projectPath' status --short", projectPath).collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    // ── Commit ────────────────────────────────────────────────────────────────
    fun commit(projectPath: String, message: String): Flow<TerminalLine> = flow {
        Logger.info(LogTag.GIT, "commit", projectPath, "msg=$message")
        Logger.info(LogTag.GIT, "commit: $message")
        emit(TerminalLine("Committing: \"$message\"", LineType.INFO))
        // Set default user if not set
        termux.executeStream(
            "git -C '$projectPath' config user.email 2>/dev/null || git -C '$projectPath' config user.email 'dev@mobileide.local'",
            projectPath
        ).collect {}
        termux.executeStream(
            "git -C '$projectPath' config user.name 2>/dev/null || git -C '$projectPath' config user.name 'MobileIDE Dev'",
            projectPath
        ).collect {}
        termux.executeStream(
            "git -C '$projectPath' commit -m \"$message\"",
            projectPath
        ).collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    // ── Log ───────────────────────────────────────────────────────────────────
    fun log(projectPath: String, limit: Int = 10): Flow<TerminalLine> =
        termux.executeStream(
            "git -C '$projectPath' log --oneline --decorate --graph -$limit 2>/dev/null || echo 'No commits yet'",
            projectPath
        )

    // ── Diff ──────────────────────────────────────────────────────────────────
    fun diff(projectPath: String): Flow<TerminalLine> =
        termux.executeStream("git -C '$projectPath' diff --stat", projectPath)

    // ── Push ──────────────────────────────────────────────────────────────────
    fun push(projectPath: String): Flow<TerminalLine> = flow {
        Logger.info(LogTag.GIT, "push: $projectPath")
        termux.executeStream("git -C '$projectPath' push", projectPath).collect { emit(it) }
    }

    // ── Pull ──────────────────────────────────────────────────────────────────
    fun pull(projectPath: String): Flow<TerminalLine> =
        termux.executeStream("git -C '$projectPath' pull", projectPath)

    // ── Branch ────────────────────────────────────────────────────────────────
    fun branch(projectPath: String): Flow<TerminalLine> =
        termux.executeStream("git -C '$projectPath' branch -a", projectPath)

    fun createBranch(projectPath: String, name: String): Flow<TerminalLine> =
        termux.executeStream("git -C '$projectPath' checkout -b '$name'", projectPath)

    // ── Remote ────────────────────────────────────────────────────────────────
    fun addRemote(projectPath: String, url: String): Flow<TerminalLine> =
        termux.executeStream("git -C '$projectPath' remote add origin '$url'", projectPath)

    // ── Stash ─────────────────────────────────────────────────────────────────
    fun stash(projectPath: String): Flow<TerminalLine> =
        termux.executeStream("git -C '$projectPath' stash", projectPath)

    fun stashPop(projectPath: String): Flow<TerminalLine> =
        termux.executeStream("git -C '$projectPath' stash pop", projectPath)

    private fun runBlocking(cmd: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            p.inputStream.bufferedReader().readText()
        } catch (e: Exception) { "" }
    }
}
