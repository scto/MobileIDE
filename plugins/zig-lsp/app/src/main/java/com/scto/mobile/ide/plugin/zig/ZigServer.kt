package com.scto.mobile.ide.plugin.zig

import android.app.Activity
import android.content.Context
import com.scto.mobile.ide.exec.isTerminalInstalled
import com.scto.mobile.ide.core.common.files.child
import com.scto.mobile.ide.core.common.files.sandboxHomeDir
import com.scto.mobile.ide.core.common.icons.Icon
import com.scto.mobile.ide.core.common.files.BuiltinFileType
import com.scto.mobile.ide.lsp.LspConnectionConfig
import com.scto.mobile.ide.lsp.ScriptedLspServer
import com.scto.mobile.ide.plugin.zig.utils.GithubReleasesApi
import java.io.File

class ZigServer(
    override val icon: Icon? = BuiltinFileType.ZIG.icon,
    override val supportedExtensions: List<String> = listOf("zig"),
    override val installScript: File
) : ScriptedLspServer() {

    override val id = "zig"
    override val languageName = "Zig"
    override val serverName = "zls"
    override val installId = "Zig and ZLS (Zig Language Server)"

    private val latestVersion = "0.13.0"

    override suspend fun isInstalled(context: Context): Boolean {
        if (!isTerminalInstalled()) {
            return false
        }
        return sandboxHomeDir().child(".local/zig/zls/zls").exists()
    }

    override fun install(activity: Activity) {
        launchInstaller(activity, latestVersion)
    }

    override fun uninstall(activity: Activity) {
        launchInstaller(activity, "--uninstall", latestVersion)
    }

    override fun update(activity: Activity) {
        launchInstaller(activity, "--update", latestVersion)
    }

    override suspend fun isUpdatable(context: Context): Boolean {
        val versionFile = sandboxHomeDir().child(".local/zig/zls/zls_version.txt")
        val currentVersionText = runCatching { versionFile.readText().trim() }.getOrNull() ?: return false
        return currentVersionText != latestVersion
    }

    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf(
            sandboxHomeDir().child(".local/bin/zls").absolutePath
        ))
    }
}