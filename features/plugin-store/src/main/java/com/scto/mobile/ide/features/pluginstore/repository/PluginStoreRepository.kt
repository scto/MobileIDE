package com.scto.mobile.ide.features.pluginstore.repository

import android.content.Context
import android.os.Build
import com.scto.mobile.ide.features.pluginstore.model.PluginAuthor
import com.scto.mobile.ide.features.pluginstore.model.PluginStatus
import com.scto.mobile.ide.features.pluginstore.model.PluginType
import com.scto.mobile.ide.features.pluginstore.model.StorePluginItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class InstalledRecord(
    val id: String,
    val version: String,
    val installPath: String
)

class PluginStoreRepository(private val context: Context) {

    companion object {
        private const val TAG = "PluginStoreRepo"
        private const val DEFAULT_INDEX_URL =
            "https://raw.githubusercontent.com/scto/MobileIDE-Plugins/main/plugins-index.json"
    }

    private val extensionsDir: File
        get() = File(context.filesDir.parentFile, "local/extensions").apply { mkdirs() }

    private val installedJsonFile: File
        get() = File(extensionsDir, "installed.json")

    suspend fun fetchPluginList(indexUrl: String = DEFAULT_INDEX_URL): List<StorePluginItem> =
        withContext(Dispatchers.IO) {
            val remoteItems = mutableListOf<StorePluginItem>()

            // 1. Try fetching remote plugin index JSON
            try {
                val connection = (URL(indexUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                }
                if (connection.responseCode == 200) {
                    val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                    remoteItems.addAll(parsePluginIndexJson(jsonText))
                    Timber.tag(TAG).i("Fetched ${remoteItems.size} plugins from remote repository.")
                } else {
                    Timber.tag(TAG).w("Remote plugin index returned HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to fetch remote plugin index from $indexUrl. Falling back to local catalog.")
            }

            // 2. Fallback / Merge with local bundled plugins catalog
            val bundledItems = fetchBundledPluginCatalog()
            val mergedMap = LinkedHashMap<String, StorePluginItem>()

            for (item in remoteItems) {
                mergedMap[item.id] = item
            }
            for (bundled in bundledItems) {
                if (!mergedMap.containsKey(bundled.id)) {
                    mergedMap[bundled.id] = bundled
                }
            }

            // 3. Resolve local installation status for all items via installed.json and filesystem
            return@withContext mergedMap.values.map { item ->
                resolveInstallationStatus(item)
            }
        }

    private fun parsePluginIndexJson(jsonText: String): List<StorePluginItem> {
        val list = mutableListOf<StorePluginItem>()
        try {
            val root = JSONObject(jsonText)
            val pluginsArray = root.optJSONArray("plugins") ?: JSONArray()
            for (i in 0 until pluginsArray.length()) {
                val obj = pluginsArray.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.optString("name", id)
                val version = obj.optString("version", "1.0.0")
                val description = obj.optString("description", "")
                val typeStr = obj.optString("category", obj.optString("type", "lsp"))
                val downloadUrl = obj.optString("downloadUrl", "")
                val size = obj.optLong("sizeBytes", obj.optLong("size", 0L))
                val minAppVersion = obj.optInt("minAppVersion", 1)

                val authorStr = obj.optString("author", "")
                val authorObj = obj.optJSONObject("author")
                val author = if (authorObj != null) {
                    PluginAuthor(
                        displayName = authorObj.optString("displayName", ""),
                        github = authorObj.optString("github", "")
                    )
                } else PluginAuthor(displayName = authorStr)

                val tagsList = mutableListOf<String>()
                val tagsArr = obj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (j in 0 until tagsArr.length()) {
                        tagsList.add(tagsArr.getString(j))
                    }
                }

                val archList = mutableListOf<String>()
                val archArr = obj.optJSONArray("arch")
                if (archArr != null) {
                    for (j in 0 until archArr.length()) {
                        archList.add(archArr.getString(j))
                    }
                }

                list.add(
                    StorePluginItem(
                        id = id,
                        name = name,
                        version = version,
                        description = description,
                        author = author,
                        type = PluginType.fromString(typeStr),
                        downloadUrl = downloadUrl,
                        size = size,
                        minAppVersion = minAppVersion,
                        tags = tagsList,
                        arch = archList
                    )
                )
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error parsing plugin index JSON")
        }
        return list
    }

    private fun fetchBundledPluginCatalog(): List<StorePluginItem> {
        val catalog = mutableListOf<StorePluginItem>()
        try {
            val assets = context.assets.list("bundled_plugins") ?: emptyArray()
            for (asset in assets) {
                if (asset.endsWith(".zip")) {
                    val stream = context.assets.open("bundled_plugins/$asset")
                    val manifestJson = readManifestFromZipStream(stream)
                    if (manifestJson != null) {
                        val obj = JSONObject(manifestJson)
                        val id = obj.optString("id")
                        if (id.isNotEmpty()) {
                            catalog.add(
                                StorePluginItem(
                                    id = id,
                                    name = obj.optString("name", id),
                                    version = obj.optString("version", "1.0.0"),
                                    description = obj.optString("description", "Bundled IDE Extension"),
                                    type = PluginType.fromString(obj.optString("type", "lsp")),
                                    downloadUrl = "asset://bundled_plugins/$asset",
                                    tags = listOf("bundled", "lsp")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error scanning bundled_plugins assets")
        }
        return catalog
    }

    fun getInstalledRecords(): List<InstalledRecord> {
        val list = mutableListOf<InstalledRecord>()
        if (!installedJsonFile.exists()) return list
        try {
            val jsonText = installedJsonFile.readText()
            val array = JSONArray(jsonText)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    InstalledRecord(
                        id = obj.getString("id"),
                        version = obj.getString("version"),
                        installPath = obj.getString("installPath")
                    )
                )
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error reading installed.json")
        }
        return list
    }

    private fun updateInstalledRecord(id: String, version: String, installPath: String) {
        val list = getInstalledRecords().toMutableList()
        list.removeAll { it.id == id }
        list.add(InstalledRecord(id, version, installPath))

        val array = JSONArray()
        for (rec in list) {
            array.put(JSONObject().apply {
                put("id", rec.id)
                put("version", rec.version)
                put("installPath", rec.installPath)
            })
        }
        installedJsonFile.writeText(array.toString(2))
    }

    private fun removeInstalledRecord(id: String) {
        val list = getInstalledRecords().toMutableList()
        list.removeAll { it.id == id }

        val array = JSONArray()
        for (rec in list) {
            array.put(JSONObject().apply {
                put("id", rec.id)
                put("version", rec.version)
                put("installPath", rec.installPath)
            })
        }
        installedJsonFile.writeText(array.toString(2))
    }

    fun isAbiCompatible(archList: List<String>): Boolean {
        if (archList.isEmpty()) return true
        val supportedAbis = Build.SUPPORTED_ABIS.map { it.lowercase() }
        return archList.any { arch -> supportedAbis.contains(arch.lowercase()) }
    }

    private fun resolveInstallationStatus(item: StorePluginItem): StorePluginItem {
        val records = getInstalledRecords()
        val record = records.firstOrNull { it.id == item.id }

        val pluginDir = File(extensionsDir, item.id)
        val manifestFile = File(pluginDir, "manifest.json")

        if (record == null && (!pluginDir.exists() || !manifestFile.exists())) {
            return item.copy(status = PluginStatus.NOT_INSTALLED, installedVersion = null)
        }

        val installedVer = record?.version ?: run {
            if (manifestFile.exists()) {
                try {
                    JSONObject(manifestFile.readText()).optString("version", "0.0.0")
                } catch (e: Exception) { "0.0.0" }
            } else "0.0.0"
        }

        return if (isVersionGreater(item.version, installedVer)) {
            item.copy(status = PluginStatus.UPDATE_AVAILABLE, installedVersion = installedVer)
        } else {
            item.copy(status = PluginStatus.INSTALLED, installedVersion = installedVer)
        }
    }

    suspend fun installPlugin(
        item: StorePluginItem,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. ABI Architecture Check BEFORE download
            if (!isAbiCompatible(item.arch)) {
                val msg = "Incompatible CPU architecture (${item.arch.joinToString()}) for device ABIs (${Build.SUPPORTED_ABIS.joinToString()})"
                Timber.tag(TAG).e(msg)
                return@withContext Result.failure(IllegalArgumentException(msg))
            }

            // 2. Prepare target versioned directory
            val rootPluginDir = File(extensionsDir, item.id)
            val versionedTargetDir = File(rootPluginDir, item.version)
            versionedTargetDir.mkdirs()

            if (item.downloadUrl.startsWith("asset://")) {
                val assetPath = item.downloadUrl.removePrefix("asset://")
                context.assets.open(assetPath).use { stream ->
                    extractZipStream(stream, versionedTargetDir)
                    extractZipStream(context.assets.open(assetPath), rootPluginDir)
                }
            } else {
                val connection = (URL(item.downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                if (connection.responseCode != 200) {
                    return@withContext Result.failure(Exception("HTTP error ${connection.responseCode} downloading plugin"))
                }
                val totalLength = connection.contentLengthLong
                val tempZip = File(context.cacheDir, "${item.id}-${item.version}.zip")

                connection.inputStream.use { input ->
                    FileOutputStream(tempZip).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloaded = 0L
                        while (input.read(buffer).also { bytesRead = it } > 0) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            if (totalLength > 0) {
                                onProgress(downloaded.toFloat() / totalLength.toFloat())
                            }
                        }
                    }
                }

                // Verify SHA-256 if provided
                if (!item.sha256.isNullOrBlank()) {
                    if (!verifySha256(tempZip, item.sha256)) {
                        tempZip.delete()
                        return@withContext Result.failure(SecurityException("SHA-256 checksum mismatch for plugin ${item.id}"))
                    }
                }

                tempZip.inputStream().use { stream ->
                    extractZipStream(stream, versionedTargetDir)
                }
                tempZip.inputStream().use { stream ->
                    extractZipStream(stream, rootPluginDir)
                }
                tempZip.delete()
            }

            // 3. Update installed.json record
            updateInstalledRecord(item.id, item.version, versionedTargetDir.absolutePath)

            Timber.tag(TAG).i("Successfully installed plugin ${item.id} v${item.version} at ${versionedTargetDir.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to install plugin ${item.id}")
            Result.failure(e)
        }
    }

    suspend fun uninstallPlugin(item: StorePluginItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(extensionsDir, item.id)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            removeInstalledRecord(item.id)
            Timber.tag(TAG).i("Successfully uninstalled plugin ${item.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to uninstall plugin ${item.id}")
            Result.failure(e)
        }
    }

    private fun verifySha256(file: File, expectedHash: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = file.readBytes()
            val computedHash = digest.digest(bytes).joinToString("") { "%02x".format(it) }
            computedHash.equals(expectedHash.trim(), ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    private fun extractZipStream(inputStream: InputStream, targetDir: File) {
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { output ->
                        zis.copyTo(output)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun readManifestFromZipStream(inputStream: InputStream): String? {
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "manifest.json") {
                    return zis.bufferedReader().readText()
                }
                entry = zis.nextEntry
            }
        }
        return null
    }

    private fun isVersionGreater(v1: String, v2: String): Boolean {
        try {
            val p1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
            val p2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(p1.size, p2.size)
            for (i in 0 until maxLen) {
                val n1 = p1.getOrElse(i) { 0 }
                val n2 = p2.getOrElse(i) { 0 }
                if (n1 > n2) return true
                if (n1 < n2) return false
            }
        } catch (e: Exception) {
            return v1 != v2
        }
        return false
    }
}
