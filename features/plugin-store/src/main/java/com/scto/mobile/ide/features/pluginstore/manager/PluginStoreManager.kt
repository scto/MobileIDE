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

data class DependencyCheckResult(
    val hasMissingDependencies: Boolean,
    val missingRuntimes: List<String>
)

class PluginStoreManager(private val context: Context) {

    private val repository = PluginStoreRepository(context)
    private val extensionsDir: File
        get() = File(context.filesDir.parentFile, "local/extensions").apply { mkdirs() }

    private val sandboxDir: File
        get() = File(context.filesDir.parentFile, "local/sandbox")

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

    suspend fun checkDependencies(plugin: StorePluginItem): DependencyCheckResult = withContext(Dispatchers.IO) {
        if (plugin.dependencies.isNullOrEmpty()) {
            return@withContext DependencyCheckResult(false, emptyList())
        }

        val missing = mutableListOf<String>()

        for (dep in plugin.dependencies) {
            when (dep.lowercase().trim()) {
                "runtime-node", "node", "nodejs" -> {
                    val hasNode = File(sandboxDir, "usr/bin/node").exists() ||
                            File(sandboxDir, "usr/bin/npm").exists() ||
                            File(sandboxDir, "usr/local/bin/node").exists()
                    if (!hasNode) missing.add("Node.js (runtime-node)")
                }
                "runtime-java", "java", "jdk", "openjdk" -> {
                    val hasJava = File(sandboxDir, "usr/bin/java").exists() ||
                            File(sandboxDir, "usr/lib/jvm").exists()
                    if (!hasJava) missing.add("Java JDK (runtime-java)")
                }
                "runtime-python", "python", "pyright" -> {
                    val hasPython = File(sandboxDir, "usr/bin/python3").exists() ||
                            File(sandboxDir, "usr/bin/python").exists()
                    if (!hasPython) missing.add("Python 3 (runtime-python)")
                }
                "runtime-dotnet", "dotnet" -> {
                    val hasDotnet = File(sandboxDir, "usr/bin/dotnet").exists() ||
                            File(sandboxDir, "usr/share/dotnet").exists()
                    if (!hasDotnet) missing.add(".NET Core SDK (runtime-dotnet)")
                }
            }
        }

        DependencyCheckResult(missing.isNotEmpty(), missing)
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
            val pluginVersionDir = File(extensionsDir, "${plugin.id}/${plugin.version}")
            if (pluginVersionDir.exists()) {
                pluginVersionDir.walkTopDown().forEach { file ->
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

    suspend fun updatePlugin(plugin: StorePluginItem, onProgress: (Float) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        val oldRecord = repository.getInstalledRecords().firstOrNull { it.id == plugin.id }
        val oldVersion = oldRecord?.version ?: plugin.installedVersion

        val installResult = downloadAndInstallPlugin(plugin, onProgress)
        if (installResult.isSuccess && !oldVersion.isNullOrEmpty() && oldVersion != plugin.version) {
            val oldVersionDir = File(extensionsDir, "${plugin.id}/$oldVersion")
            if (oldVersionDir.exists()) {
                oldVersionDir.deleteRecursively()
            }
        }
        installResult
    }

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
