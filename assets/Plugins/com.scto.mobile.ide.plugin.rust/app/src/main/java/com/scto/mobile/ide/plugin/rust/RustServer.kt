package com.scto.mobile.ide.plugin.rust

import android.app.Activity
import android.content.Context
import com.scto.mobile.ide.plugin.rust.utils.GithubReleasesApi
import com.rk.exec.isTerminalInstalled
import com.rk.extension.ExtensionContext
import com.rk.file.child
import com.rk.file.sandboxHomeDir
import com.rk.icons.Icon
import com.rk.lsp.LspConnectionConfig
import com.rk.lsp.ScriptedLspServer
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI

class RustServer(
    private val context: ExtensionContext,
    override val icon: Icon,
    override val supportedExtensions: List<String>,
    override val installScript: File,
) : ScriptedLspServer() {

    override val id = "rust"
    override val languageName = "Rust"
    override val serverName = "rust-analyzer"

    override val installId = "rust-analyzer language server"

    private suspend fun fetchLatestVersion(): String {
        return GithubReleasesApi("rust-lang", "rust-analyzer").fetchLatestVersion() ?: "2026-07-13"
    }

    override suspend fun isInstalled(context: Context): Boolean {
        if (!isTerminalInstalled()) {
            return false
        }

        return sandboxHomeDir().child(".lsp/rust/rust-analyzer").exists()
    }

    override fun install(activity: Activity) {
        context.scope.launch {
            launchInstaller(activity, "--install", fetchLatestVersion())
        }
    }

    override fun uninstall(activity: Activity) {
        context.scope.launch {
            launchInstaller(activity, "--uninstall", fetchLatestVersion())
        }
    }

    override fun update(activity: Activity) {
        context.scope.launch {
            launchInstaller(activity, "--update", fetchLatestVersion())
        }
    }

    override suspend fun hasUpdate(context: Context): Boolean {
        val versionFile = sandboxHomeDir().child(".lsp/rust/version.txt")
        val currentVersionText = runCatching { versionFile.readText().trim() }.getOrNull() ?: return false
        return currentVersionText != fetchLatestVersion()
    }

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf("/home/.lsp/rust/rust-analyzer"))
    }

    override fun getInitializationOptions(uri: URI?): Any? {
        return null

        // NOTE: see https://github.com/rust-lang/rust-analyzer/issues/14318
        // if (uri == null) return null
        //
        // val projectDir = File(uri)
        // val detachedFiles = findDetachedFiles(projectDir)
        // if (detachedFiles.isEmpty()) return null
        //
        // return mapOf("detachedFiles" to detachedFiles)
    }

    private fun findDetachedFiles(root: File): List<String> {
        val detachedFiles = mutableListOf<String>()

        fun visit(dir: File, insideCargoProject: Boolean) {
            val hasCargoToml = File(dir, "Cargo.toml").exists()
            val cargoProject = insideCargoProject || hasCargoToml

            if (cargoProject) return

            dir.listFiles()?.forEach { file ->
                when {
                    file.isDirectory && file.name !in setOf(".git", "target") -> visit(file, false)
                    file.isFile && supportedExtensions.contains(file.extension) -> detachedFiles.add(file.absolutePath)
                }
            }
        }

        visit(root, false)

        return detachedFiles
    }
}
