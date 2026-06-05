/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.scto.mobile.ide.ui.editor.git

import android.util.Log
import java.io.File
import java.net.InetSocketAddress
import java.security.PublicKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.sshd.common.util.io.PathUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase
import org.eclipse.jgit.transport.sshd.SshdSessionFactory
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter

// Log tag, search in Logcat "WebIDE_Git"
private const val TAG = "WebIDE_Git"

enum class GitConnectivityError {
    AUTH_FAILED,
    REPO_NOT_FOUND,
    TIMEOUT,
    UNKNOWN_HOST,
    SSH_ENV_FAILED,
    UNKNOWN,
}

data class GitConnectivityResult(
    val isSuccess: Boolean,
    val refsCount: Int = 0,
    val error: GitConnectivityError? = null,
    val rawMessage: String? = null,
)

private val DEFAULT_GITIGNORE =
    """
    # --- WebIDE Security (Must never be uploaded) ---
    .git_ssh_config/
    id_rsa
    id_rsa.pub

    # --- Android Build ---
    build/
    .gradle/
    app/build/
    *.apk
    *.ap_
    *.dex

    # --- IDE Settings ---
    .idea/
    .vscode/
    *.iml
    *.ipr
    *.iws
    local.properties

    # --- System ---
    .DS_Store
    Thumbs.db
    """
        .trimIndent()

class GitManager(projectPath: String) {
    private val rootDir = File(projectPath)

    // SSH Key storage directory (hidden folder)
    private val sshConfigDir = File(rootDir, ".git_ssh_config")

    // ========================================================================
    // Core fix: initialize SSH environment
    // ========================================================================
    init {
        try {
            // Redirect UserHome to safely manage SSH configurations within the app storage
            PathUtils.setUserHomeFolderResolver {
                if (!sshConfigDir.exists()) sshConfigDir.mkdirs()
                rootDir.toPath()
            }
            Log.i(TAG, "SSH environment repair: UserHome Redirected to -> $rootDir")
        } catch (e: Throwable) {
            Log.e(TAG, "SSH environment repair failed (may cause SSH connection crash)", e)
        }
    }

    fun isGitRepo(): Boolean = File(rootDir, ".git").exists()

    // ========================================================================
    // Debug utilities
    // ========================================================================
    fun debugGitConfig() {
        try {
            val configFile = File(rootDir, ".git/config")
            if (configFile.exists()) {
                Log.d(TAG, "⬇️⬇️⬇️ [.git/config] ⬇️⬇️⬇️")
                Log.d(TAG, configFile.readText())
                Log.d(TAG, "⬆️⬆️⬆️ [End Config] ⬆️⬆️⬆️")
            } else {
                Log.e(TAG, "❌ Error: .git/config does not exist")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read config", e)
        }
    }

    // ========================================================================
    // Basic operations
    // ========================================================================
    suspend fun initRepo() =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Initializing repository: $rootDir")

            // Initialize empty Git repository
            val git = Git.init().setDirectory(rootDir).call()

            // Create default gitignore to protect IDE configurations and SSH keys
            val ignoreFile = File(rootDir, ".gitignore")
            if (!ignoreFile.exists()) {
                try {
                    ignoreFile.writeText(DEFAULT_GITIGNORE)
                    Log.i(TAG, "Automation: Default .gitignore rule created")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write .gitignore", e)
                }
            } else {
                // Check if existing .gitignore contains security rules, otherwise append them
                val currentContent = ignoreFile.readText()
                if (!currentContent.contains(".git_ssh_config/")) {
                    ignoreFile.appendText("\n# Safety check by MobileIDE\n.git_ssh_config/\n")
                    Log.i(TAG, "Automation: Security ignore rule added")
                }
            }

            git.close()
            Log.i(TAG, "Repository initialization process completed (Init + Security Rules)")
        }

    suspend fun getBranches(): List<GitBranch> =
        withContext(Dispatchers.IO) {
            if (!isGitRepo()) return@withContext emptyList()
            val git = Git.open(rootDir)
            val repo = git.repository
            val currentBranchRef = repo.fullBranch

            val branchList = mutableListOf<GitBranch>()
            val refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()

            refs.forEach { ref ->
                val fullName = ref.name
                var displayName = fullName
                var type = BranchType.LOCAL

                if (fullName.startsWith(Constants.R_HEADS)) {
                    displayName = fullName.substring(Constants.R_HEADS.length)
                    type = BranchType.LOCAL
                } else if (fullName.startsWith(Constants.R_REMOTES)) {
                    displayName = fullName.substring(Constants.R_REMOTES.length)
                    type = BranchType.REMOTE
                }
                branchList.add(GitBranch(displayName, fullName, type, fullName == currentBranchRef))
            }

            git.close()
            branchList.sortedWith(compareByDescending<GitBranch> { it.isCurrent }.thenBy { it.type }.thenBy { it.name })
        }

    suspend fun getStatus(): List<GitFileChange> =
        withContext(Dispatchers.IO) {
            if (!isGitRepo()) return@withContext emptyList()
            val git = Git.open(rootDir)
            val status = git.status().call()
            val changes = mutableListOf<GitFileChange>()

            status.added.forEach { changes.add(GitFileChange(it, GitFileStatus.ADDED)) }
            status.changed.forEach { changes.add(GitFileChange(it, GitFileStatus.MODIFIED)) }
            status.modified.forEach { changes.add(GitFileChange(it, GitFileStatus.MODIFIED)) }
            status.untracked.forEach { changes.add(GitFileChange(it, GitFileStatus.UNTRACKED)) }
            status.missing.forEach { changes.add(GitFileChange(it, GitFileStatus.MISSING)) }
            status.removed.forEach { changes.add(GitFileChange(it, GitFileStatus.REMOVED)) }
            status.conflicting.forEach { changes.add(GitFileChange(it, GitFileStatus.CONFLICTING)) }

            git.close()
            if (changes.isNotEmpty()) Log.d(TAG, "Detected ${changes.size} file changes")
            changes.sortedBy { it.filePath }
        }

    suspend fun commitAll(message: String, author: String, email: String) =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Commit: '$message'")
            val git = Git.open(rootDir)

            // Ensure SSH config folder is ignored before committing
            val sshDir = File(rootDir, ".git_ssh_config")
            if (sshDir.exists()) {
                val ignoreFile = File(rootDir, ".gitignore")
                if (!ignoreFile.exists()) {
                    ignoreFile.writeText(DEFAULT_GITIGNORE)
                }
            }
            // ----------------------------------------------------

            git.add().addFilepattern(".").call()

            // Handle deleted files
            val status = git.status().call()
            if (status.missing.isNotEmpty() || status.removed.isNotEmpty()) {
                val rm = git.rm()
                status.missing.forEach { rm.addFilepattern(it) }
                status.removed.forEach { rm.addFilepattern(it) }
                rm.call()
            }

            val person = PersonIdent(author, email)
            git.commit().setMessage(message).setAuthor(person).setCommitter(person).call()

            git.close()
        }

    // ========================================================================
    // Remote operations (Connect, Add, Push, Pull)
    // ========================================================================

    /** Test connection (ls-remote) */
    suspend fun testConnectivity(url: String, auth: GitAuth): GitConnectivityResult =
        withContext(Dispatchers.IO) {
            Log.i(TAG, ">>> Start connection test: $url")
            try {
                val cmd = Git.lsRemoteRepository().setRemote(url).setHeads(true).setTags(false)

                if (auth.type == AuthType.HTTPS) {
                    cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(auth.username, auth.token))
                } else {
                    cmd.setTransportConfigCallback(prepareSshEnvironment(auth))
                }

                val result = cmd.callAsMap()
                Log.i(TAG, "✅ Connection successful! Found ${result.size} refs")
                return@withContext GitConnectivityResult(isSuccess = true, refsCount = result.size)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Connection test failed", e)
                val msg = e.message ?: e.toString()
                return@withContext when {
                    msg.contains("401") -> GitConnectivityResult(false, error = GitConnectivityError.AUTH_FAILED)
                    msg.contains("not found") ->
                        GitConnectivityResult(false, error = GitConnectivityError.REPO_NOT_FOUND)
                    msg.contains("timeout") || msg.contains("abort") ->
                        GitConnectivityResult(false, error = GitConnectivityError.TIMEOUT)
                    msg.contains("UnknownHost") ->
                        GitConnectivityResult(false, error = GitConnectivityError.UNKNOWN_HOST)
                    msg.contains("No user home") ->
                        GitConnectivityResult(false, error = GitConnectivityError.SSH_ENV_FAILED)
                    else -> GitConnectivityResult(false, error = GitConnectivityError.UNKNOWN, rawMessage = msg)
                }
            }
        }

    /** Add and configure remote URL */
    suspend fun addRemote(name: String, url: String) =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Set remote: $name -> $url")
            val git = Git.open(rootDir)
            val config = git.repository.config

            config.setString("remote", name, "url", url)
            // Set up the fetch refspec
            val fetchSpec = "+refs/heads/*:refs/remotes/$name/*"
            config.setString("remote", name, "fetch", fetchSpec)

            config.save()
            git.close()
            debugGitConfig()
        }

    /** Push with specific force-like refspecs */
    suspend fun push(auth: GitAuth, remote: String = "origin") =
        withContext(Dispatchers.IO) {
            Log.i(TAG, ">>> PUSH (FORCE+) Start <<<")
            val git = Git.open(rootDir)
            val currentBranch = git.repository.branch ?: throw Exception("Not currently on any branch")

            // Define a targeted push rule
            val refSpecStr = "+refs/heads/$currentBranch:refs/heads/$currentBranch"

            Log.i(TAG, "Using force push rule: $refSpecStr")
            val spec = RefSpec(refSpecStr)

            val cmd = git.push().setRemote(remote).setRefSpecs(spec)

            if (auth.type == AuthType.HTTPS) {
                cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(auth.username, auth.token))
            } else {
                cmd.setTransportConfigCallback(prepareSshEnvironment(auth))
            }

            try {
                val results = cmd.call()
                var errorMsg = ""
                var isSuccess = false

                for (result in results) {
                    for (update in result.remoteUpdates) {
                        Log.i(TAG, "Branch [${update.remoteName}] -> ${update.status}")

                        if (
                            update.status == RemoteRefUpdate.Status.OK ||
                                update.status == RemoteRefUpdate.Status.UP_TO_DATE
                        ) {
                            isSuccess = true
                        } else {
                            errorMsg += "Push failed: ${update.status} - ${update.message}\n"
                        }
                    }
                }

                if (!isSuccess && errorMsg.isNotEmpty()) throw Exception(errorMsg)
                Log.i(TAG, "✅ Push Success")
            } catch (e: Exception) {
                // Handle possible broken pipe or connection aborts gracefully
                if (e.message?.contains("Software caused connection abort") == true) {
                    Log.w(TAG, "Network instability detected, please retry...")
                    throw Exception("Network connection interrupted, please retry")
                }
                Log.e(TAG, "❌ Push Exception", e)
                throw e
            } finally {
                git.close()
            }
        }

    // Retrieve file contents at the current HEAD commit for diffing
    suspend fun getFileContentAtHead(filePath: String): String =
        withContext(Dispatchers.IO) {
            if (!isGitRepo()) return@withContext ""
            val git = Git.open(rootDir)
            val repo = git.repository

            try {
                val headId = repo.resolve(Constants.HEAD) ?: return@withContext ""

                // Walk through revisions to find the tree
                val revWalk = RevWalk(repo)
                val commit = revWalk.parseCommit(headId)
                val tree = commit.tree

                // Resolve the relative file path for TreeWalk
                val relativePath =
                    if (filePath.startsWith(rootDir.absolutePath)) {
                        filePath.substring(rootDir.absolutePath.length + 1).replace("\\", "/")
                    } else {
                        filePath
                    }

                // Find the object ID of our file within the tree
                val treeWalk = TreeWalk(repo)
                treeWalk.addTree(tree)
                treeWalk.isRecursive = true
                treeWalk.filter = PathFilter.create(relativePath)

                if (!treeWalk.next()) {
                    // File does not exist in HEAD
                    return@withContext ""
                }

                val objectId = treeWalk.getObjectId(0)
                val loader = repo.open(objectId)

                return@withContext String(loader.bytes, Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retrieve HEAD content: $filePath", e)
                return@withContext ""
            } finally {
                git.close()
            }
        }

    suspend fun pull(auth: GitAuth, remote: String = "origin") =
        withContext(Dispatchers.IO) {
            Log.i(TAG, ">>> PULL Start <<<")
            val git = Git.open(rootDir)
            val cmd = git.pull().setRemote(remote)

            if (auth.type == AuthType.HTTPS) {
                cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(auth.username, auth.token))
            } else {
                cmd.setTransportConfigCallback(prepareSshEnvironment(auth))
            }

            try {
                val result = cmd.call()
                if (!result.isSuccessful) {
                    throw Exception("Pull Failed: ${result.mergeResult?.mergeStatus}")
                }
                Log.i(TAG, "✅ Pull Success")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Pull Exception", e)
                throw e
            } finally {
                git.close()
            }
        }

    suspend fun pullRebase(auth: GitAuth, remote: String = "origin") =
        withContext(Dispatchers.IO) {
            Log.i(TAG, ">>> PULL (Rebase) Start <<<")
            val git = Git.open(rootDir)
            val cmd = git.pull().setRemote(remote).setRebase(true)

            if (auth.type == AuthType.HTTPS) {
                cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(auth.username, auth.token))
            } else {
                cmd.setTransportConfigCallback(prepareSshEnvironment(auth))
            }

            try {
                cmd.call()
                Log.i(TAG, "✅ Rebase Success")
            } finally {
                git.close()
            }
        }

    // ========================================================================
    // SSH configuration (TrustAll + KeyInjection)
    // ========================================================================
    class CustomSshSessionFactory(private val sshDir: File) : SshdSessionFactory() {
        override fun getSshDirectory(): File = sshDir

        override fun getHomeDirectory(): File = sshDir.parentFile ?: sshDir

        override fun getServerKeyDatabase(homeDir: File, sshDir: File): ServerKeyDatabase {
            return object : ServerKeyDatabase {
                override fun lookup(
                    c: String,
                    r: InetSocketAddress,
                    conf: ServerKeyDatabase.Configuration,
                ): List<PublicKey> = emptyList()

                override fun accept(
                    c: String,
                    r: InetSocketAddress,
                    k: PublicKey,
                    conf: ServerKeyDatabase.Configuration,
                    p: CredentialsProvider?,
                ): Boolean {
                    Log.d(TAG, "SSH: Trusting Host Key -> $c")
                    return true
                }
            }
        }
    }

    private fun prepareSshEnvironment(auth: GitAuth): TransportConfigCallback {
        return TransportConfigCallback { transport ->
            if (transport is SshTransport) {
                if (!sshConfigDir.exists()) sshConfigDir.mkdirs()

                if (auth.privateKey.isNotBlank()) {
                    val keyFile = File(sshConfigDir, "id_rsa")
                    // Update key if it doesn't match the current authenticated key
                    if (!keyFile.exists() || keyFile.readText() != auth.privateKey) {
                        keyFile.writeText(auth.privateKey)
                        Log.d(TAG, "SSH: Injecting new private key")
                    }
                }
                transport.sshSessionFactory = CustomSshSessionFactory(sshConfigDir)
            }
        }
    }

    // ========================================================================
    // Branching and Tagging
    // ========================================================================
    suspend fun createBranch(name: String, checkout: Boolean = true) =
        withContext(Dispatchers.IO) {
            val git = Git.open(rootDir)
            git.branchCreate().setName(name).call()
            if (checkout) git.checkout().setName(name).call()
            git.close()
        }

    suspend fun createTag(name: String, message: String) =
        withContext(Dispatchers.IO) {
            val git = Git.open(rootDir)
            git.tag().setName(name).setMessage(message).call()
            git.close()
        }

    suspend fun checkout(name: String) =
        withContext(Dispatchers.IO) {
            val git = Git.open(rootDir)
            git.checkout().setName(name).call()
            git.close()
        }

    suspend fun getCurrentBranch(): String =
        withContext(Dispatchers.IO) {
            if (!isGitRepo()) return@withContext ""
            val git = Git.open(rootDir)
            val b = git.repository.branch
            git.close()
            b ?: "HEAD"
        }

    suspend fun getCommitLog(): Pair<List<RevCommit>, Map<String, List<GitRefUI>>> =
        withContext(Dispatchers.IO) {
            if (!isGitRepo()) return@withContext Pair(emptyList(), emptyMap())
            val git = Git.open(rootDir)
            val repo = git.repository

            val refMap = mutableMapOf<String, MutableList<GitRefUI>>()

            val head = repo.resolve(Constants.HEAD)
            if (head != null) {
                refMap.getOrPut(head.name) { mutableListOf() }.add(GitRefUI("HEAD", RefType.HEAD))
            }

            repo.refDatabase.refs.forEach { ref ->
                val id = ref.objectId.name
                val name = ref.name
                val simpleName = RepositoryUtils.shortenRefName(name)

                val type =
                    when {
                        name.startsWith(Constants.R_HEADS) -> RefType.LOCAL_BRANCH
                        name.startsWith(Constants.R_REMOTES) -> RefType.REMOTE_BRANCH
                        name.startsWith(Constants.R_TAGS) -> RefType.TAG
                        else -> RefType.LOCAL_BRANCH
                    }

                if (name != Constants.HEAD) {
                    refMap.getOrPut(id) { mutableListOf() }.add(GitRefUI(simpleName, type))
                }
            }

            val walk = RevWalk(repo)
            repo.refDatabase.refs.forEach { ref ->
                if (ref.objectId != null) {
                    try {
                        walk.markStart(walk.parseCommit(ref.objectId))
                    } catch (_: Exception) {}
                }
            }
            walk.sort(org.eclipse.jgit.revwalk.RevSort.COMMIT_TIME_DESC)
            walk.sort(org.eclipse.jgit.revwalk.RevSort.TOPO)

            val commits = mutableListOf<RevCommit>()
            for (commit in walk) {
                commits.add(commit)
            }

            walk.dispose()
            git.close()

            Pair(commits, refMap)
        }
}

object RepositoryUtils {
    fun shortenRefName(refName: String): String {
        if (refName.startsWith(Constants.R_HEADS)) return refName.substring(Constants.R_HEADS.length)
        if (refName.startsWith(Constants.R_TAGS)) return refName.substring(Constants.R_TAGS.length)
        if (refName.startsWith(Constants.R_REMOTES)) return refName.substring(Constants.R_REMOTES.length)
        return refName
    }
}
