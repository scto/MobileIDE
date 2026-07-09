package com.rk.lsp

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.rk.extension.api.XedExtensionPoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.net.URI
import org.eclipse.lsp4j.ServerCapabilities

data class ExternalServerData(
    val id: String,
    val languageName: String,
    val serverName: String,
    val supportedExtensions: List<String>,
    val command: List<String>
)

class ExternalLspServer(
    override val id: String,
    override val languageName: String,
    override val serverName: String,
    override val supportedExtensions: List<String>,
    val command: List<String>
) : LspServer() {
    override val icon: Any? = null
    override val canBeUninstalled = true
    override suspend fun isInstalled(context: Context): Boolean = true
    override fun install(activity: Activity) {}
    override fun uninstall(activity: Activity) {}
    override suspend fun isUpdatable(context: Context): Boolean = false
    override fun update(activity: Activity) {}
    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(command.toTypedArray())
    }
}

class IntegratedLspServer(
    override val id: String,
    override val languageName: String,
    override val serverName: String,
    override val supportedExtensions: List<String>,
    val binaryName: String,
    val installScriptAsset: String
) : ScriptedLspServer() {
    override val icon: Any? = null
    override val installScript = File(localBinDir(), "lsp/$installScriptAsset")
    override val installId = id + "_installer"

    override suspend fun isInstalled(context: Context): Boolean {
        val prefixDir = context.filesDir.parentFile!!
        val distroName = context.getSharedPreferences("MobileIDE_Settings", Context.MODE_PRIVATE)
            .getString("selected_distro", "ubuntu") ?: "ubuntu"
        val distroDir = File(prefixDir, "local/$distroName")
        return File(distroDir, "usr/bin/$binaryName").exists() ||
               File(distroDir, "usr/local/bin/$binaryName").exists()
    }

    override suspend fun isUpdatable(context: Context): Boolean = false
    override fun getConnectionConfig(): LspConnectionConfig {
        return LspConnectionConfig.Process(arrayOf(binaryName, "--stdio"))
    }
}

object LspRegistry {
    private val _extensionServers = mutableStateListOf<LspServer>()
    val extensionServers: List<LspServer>
        get() = _extensionServers.toList()

    private val _externalServers = mutableStateListOf<LspServer>()
    val externalServers: List<LspServer>
        get() = _externalServers.toList()

    private val configuration: MutableMap<LspServer, Boolean> = mutableMapOf()

    init {
        // Register integrated servers
        _extensionServers.add(
            IntegratedLspServer(
                id = "html_lsp",
                languageName = "HTML",
                serverName = "vscode-html-language-server",
                supportedExtensions = listOf("html", "htm"),
                binaryName = "vscode-html-language-server",
                installScriptAsset = "html.sh"
            )
        )
        _extensionServers.add(
            IntegratedLspServer(
                id = "emmet_lsp",
                languageName = "Emmet",
                serverName = "emmet-language-server",
                supportedExtensions = listOf("html", "htm", "css", "ts", "js", "tsx", "jsx"),
                binaryName = "emmet-language-server",
                installScriptAsset = "emmet.sh"
            )
        )
        _extensionServers.add(
            IntegratedLspServer(
                id = "css_lsp",
                languageName = "CSS",
                serverName = "vscode-css-language-server",
                supportedExtensions = listOf("css"),
                binaryName = "vscode-css-language-server",
                installScriptAsset = "css.sh"
            )
        )
        _extensionServers.add(
            IntegratedLspServer(
                id = "typescript_lsp",
                languageName = "TypeScript",
                serverName = "typescript-language-server",
                supportedExtensions = listOf("js", "jsx", "ts", "tsx"),
                binaryName = "typescript-language-server",
                installScriptAsset = "typescript.sh"
            )
        )
    }

    suspend fun updateConfiguration(context: Context) {
        externalServers.forEach { configuration[it] = it.isInstalled(context) }
    }

    suspend fun getConfigurationChanges(context: Context): List<LspServer> {
        return extensionServers + externalServers.filter {
            val isInstalled = it.isInstalled(context)
            (configuration[it] ?: false) != isInstalled
        }
    }

    fun loadExternalServers(context: Context) {
        val prefs = context.getSharedPreferences("MobileIDE_External_Lsp", Context.MODE_PRIVATE)
        val json = prefs.getString("servers", null)
        if (!json.isNullOrEmpty()) {
            try {
                val listType = object : TypeToken<List<ExternalServerData>>() {}.type
                val dataList: List<ExternalServerData> = Gson().fromJson(json, listType)
                _externalServers.clear()
                dataList.forEach { data ->
                    _externalServers.add(
                        ExternalLspServer(
                            id = data.id,
                            languageName = data.languageName,
                            serverName = data.serverName,
                            supportedExtensions = data.supportedExtensions,
                            command = data.command
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveExternalServers(context: Context) {
        val prefs = context.getSharedPreferences("MobileIDE_External_Lsp", Context.MODE_PRIVATE)
        val dataList = _externalServers.map { server ->
            val cmd = when (val config = server.getConnectionConfig()) {
                is LspConnectionConfig.Process -> config.command.toList()
                is LspConnectionConfig.AndroidProcess -> config.command.toList()
                else -> emptyList()
            }
            ExternalServerData(
                id = server.id,
                languageName = server.languageName,
                serverName = server.serverName,
                supportedExtensions = server.supportedExtensions,
                command = cmd
            )
        }
        val json = Gson().toJson(dataList)
        prefs.edit().putString("servers", json).apply()
    }

    fun addExternalServer(context: Context, server: LspServer) {
        if (!_externalServers.contains(server)) {
            _externalServers.add(server)
            saveExternalServers(context)
        }
    }

    fun removeExternalServer(context: Context, server: LspServer) {
        if (_externalServers.remove(server)) {
            saveExternalServers(context)
        }
    }

    fun clearExternalServers(context: Context) {
        _externalServers.clear()
        saveExternalServers(context)
    }

    fun replaceExternalServer(context: Context, replaceIndex: Int, newServer: LspServer) {
        _externalServers[replaceIndex] = newServer
        saveExternalServers(context)
    }

    fun getForId(id: String): LspServer? {
        return _externalServers.find { it.id == id }
            ?: _extensionServers.find { it.id == id }
    }

    @XedExtensionPoint
    fun registerServer(server: LspServer) {
        if (!_extensionServers.contains(server)) {
            _extensionServers.add(server)
        }
    }

    @XedExtensionPoint
    fun unregisterServer(server: LspServer) {
        _extensionServers.remove(server)
    }
}
