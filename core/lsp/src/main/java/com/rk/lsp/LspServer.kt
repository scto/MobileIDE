package com.rk.lsp

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
        return supportedExtensions.contains(file.getExtension().lowercase())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LspServer
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

