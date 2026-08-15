package com.scto.mobile.ide.lsp

import android.app.Activity
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color

import java.io.File
import java.net.URI
import org.eclipse.lsp4j.ServerCapabilities

abstract class ScriptedLspServer : LspServer() {
    abstract val installScript: File
    abstract val installId: String

    override fun install(activity: Activity) = launchInstaller(activity)

    override fun uninstall(activity: Activity) = launchInstaller(activity, "--uninstall")

    override fun update(activity: Activity) = launchInstaller(activity, "--update")

    protected fun launchInstaller(activity: Activity, vararg flags: String) {
        val actionName = when {
            flags.contains("--uninstall") -> "Uninstalling"
            flags.contains("--update") -> "Updating"
            else -> "Installing"
        }

        // Validate installScript exists and is executable; auto-extract if missing
        var attempts = 0
        while ((!installScript.exists() || !installScript.canExecute()) && attempts < 3) {
            attempts++
            android.util.Log.w("LSP_Installer", "Install script '${installScript.absolutePath}' missing or not executable. Triggering asset extraction (attempt $attempts)...")
            com.scto.mobile.ide.core.common.utils.TerminalAssetsExtractor.ensureAssetsExtracted(activity, force = true)
            try { Thread.sleep(300) } catch (_: Exception) {}
        }

        try {
            installScript.setExecutable(true, false)
        } catch (e: Exception) {
            android.util.Log.w("LSP_Installer", "Could not set executable permissions on ${installScript.absolutePath}: ${e.message}")
        }

        if (!installScript.exists()) {
            android.util.Log.e("LSP_Installer", "CRITICAL: Install script '${installScript.absolutePath}' still missing after extraction!")
            return
        }

        android.util.Log.i("LSP_Installer", "$actionName LSP server '$id' ($serverName) using script: ${installScript.absolutePath}")
        terminalLauncher?.invoke(activity, installScript, flags.toList())
    }

    companion object {
        var terminalLauncher: ((Activity, File, List<String>) -> Unit)? = null
    }
}

abstract class LspServer {
    abstract val id: String
    abstract val languageName: String
    abstract val serverName: String
    abstract val supportedExtensions: List<String>
    abstract val icon: Any?

    open val canBeUninstalled = true

    open val expectedCapabilities: ServerCapabilities? = null



    abstract suspend fun isInstalled(context: Context): Boolean

    abstract fun install(activity: Activity)

    abstract fun uninstall(activity: Activity)

    abstract suspend fun isUpdatable(context: Context): Boolean

    abstract fun update(activity: Activity)

    abstract fun getConnectionConfig(): LspConnectionConfig

    open suspend fun beforeConnect() {}

    open suspend fun onInitialize(lspConnector: LspConnector) {}

    open fun getInitializationOptions(uri: URI?): Any? = null

    open fun isSupported(file: java.io.File): Boolean {
        return supportedExtensions.contains(file.extension.lowercase())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LspServer
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

interface LspConnector

