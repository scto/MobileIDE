package com.scto.mobile.ide.features.pluginstore.manager

import android.content.Context
import com.scto.mobile.ide.features.pluginstore.model.PluginStatus
import com.scto.mobile.ide.features.pluginstore.model.StorePluginItem
import com.scto.mobile.ide.features.pluginstore.repository.InstalledRecord
import com.scto.mobile.ide.features.pluginstore.repository.PluginStoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class PluginStoreManager(private val context: Context) {

    private val repository = PluginStoreRepository(context)
    private val extensionsDir: File
        get() = File(context.filesDir.parentFile, "local/extensions").apply { mkdirs() }

    suspend fun fetchCatalog(catalogUrl: String? = null): List<StorePluginItem> = withContext(Dispatchers.IO) {
        if (catalogUrl.isNullOrEmpty()) {
            repository.fetchPluginList()
        } else {
            repository.fetchPluginList(catalogUrl)
        }
    }

    suspend fun listInstalledPlugins(): List<StorePluginItem> = withContext(Dispatchers.IO) {
        val allPlugins = repository.fetchPluginList()
        allPlugins.filter { it.status == PluginStatus.INSTALLED || it.status == PluginStatus.UPDATE_AVAILABLE }
    }

    fun getInstalledRecords(): List<InstalledRecord> {
        return repository.getInstalledRecords()
    }

    suspend fun downloadAndInstallPlugin(
        plugin: StorePluginItem,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // 1. Check ABI compatibility BEFORE downloading
        if (!repository.isAbiCompatible(plugin.arch)) {
            val msg = "Incompatible CPU Architecture for plugin ${plugin.id}"
            return@withContext Result.failure(IllegalArgumentException(msg))
        }

        // 2. Download and extract to local/extensions/<pluginId>/<version>/
        val result = repository.installPlugin(plugin, onProgress)
        if (result.isSuccess) {
            val pluginDir = File(extensionsDir, "${plugin.id}/${plugin.version}")
            if (pluginDir.exists()) {
                pluginDir.walkTopDown().forEach { file ->
                    if (file.name.endsWith(".sh") || file.name == plugin.id || file.name == "entry") {
                        file.setExecutable(true, false)
                    }
                }
            }
        }
        result
    }

    suspend fun installPlugin(plugin: StorePluginItem, onProgress: (Float) -> Unit = {}): Result<Unit> =
        downloadAndInstallPlugin(plugin, onProgress)

    suspend fun updatePlugin(plugin: StorePluginItem, onProgress: (Float) -> Unit = {}): Result<Unit> =
        downloadAndInstallPlugin(plugin, onProgress)

    suspend fun uninstallPlugin(plugin: StorePluginItem): Result<Unit> = withContext(Dispatchers.IO) {
        repository.uninstallPlugin(plugin)
    }

    suspend fun checkForUpdates(): List<StorePluginItem> = withContext(Dispatchers.IO) {
        val catalog = fetchCatalog()
        catalog.filter { it.hasUpdate }
    }

    fun verifySha256(file: File, expectedHash: String): Boolean {
        if (expectedHash.isBlank()) return true
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = file.readBytes()
            val computedHash = digest.digest(bytes).joinToString("") { "%02x".format(it) }
            computedHash.equals(expectedHash.trim(), ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
