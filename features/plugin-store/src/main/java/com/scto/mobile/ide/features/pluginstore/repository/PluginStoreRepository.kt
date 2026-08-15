package com.scto.mobile.ide.features.pluginstore.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.scto.mobile.ide.features.pluginstore.model.PluginAuthor
import com.scto.mobile.ide.features.pluginstore.model.PluginStatus
import com.scto.mobile.ide.features.pluginstore.model.PluginType
import com.scto.mobile.ide.features.pluginstore.model.StorePluginItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
                    Log.i(TAG, "Fetched ${remoteItems.size} plugins from remote repository.")
                } else {
                    Log.w(TAG, "Remote plugin index returned HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch remote plugin index from $indexUrl. Falling back to local catalog: ${e.message}")
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

                val depList = mutableListOf<String>()
                val depArr = obj.optJSONArray("dependencies")
                if (depArr != null) {
                    for (j in 0 until depArr.length()) {
                        depList.add(depArr.getString(j))
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
                        arch = archList,
                        dependencies = depList
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing plugin index JSON", e)
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
            Log.w(TAG, "Error scanning bundled_plugins assets", e)
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
            Log.w(TAG, "Error reading installed.json", e)
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
                Log.e(TAG, msg)
                return@withContext Result.failure(IllegalArgumentException(msg))
            }

            // 2. Storage space check BEFORE download
            val freeSpace = context.filesDir.freeSpace
            val requiredSpace = if (item.size > 0) (item.size * 1.5).toLong() else 20 * 1024 * 1024L
            if (freeSpace < requiredSpace) {
                val msg = "Knapper Speicherplatz: ${freeSpace / (1024 * 1024)} MB frei, mind. ${requiredSpace / (1024 * 1024)} MB benötigt."
                Log.e(TAG, msg)
                return@withContext Result.failure(IOException(msg))
            }

            // 3. Prepare target versioned directory
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
                if (!item.downloadUrl.startsWith("https://")) {
                    val msg = "Insecure HTTP downloadUrl rejected. HTTPS is strictly required for security."
                    Log.e(TAG, msg)
                    return@withContext Result.failure(SecurityException(msg))
                }

                val tempZip = File(context.cacheDir, "${item.id}-${item.version}.zip")
                var existingLength = 0L
                if (tempZip.exists()) {
                    existingLength = tempZip.length()
                }

                val connection = (URL(item.downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    if (existingLength > 0) {
                        setRequestProperty("Range", "bytes=$existingLength-")
                    }
                }

                val isPartial = connection.responseCode == 206
                val isFull = connection.responseCode == 200

                if (!isPartial && !isFull) {
                    if (tempZip.exists()) tempZip.delete()
                    return@withContext Result.failure(Exception("HTTP Fehler ${connection.responseCode} beim Herunterladen"))
                }

                val appendMode = isPartial && existingLength > 0
                val totalLength = if (isPartial) {
                    existingLength + connection.contentLengthLong
                } else {
                    connection.contentLengthLong
                }

                var downloaded = if (appendMode) existingLength else 0L
                if (!appendMode && tempZip.exists()) {
                    tempZip.delete()
                }

                connection.inputStream.use { input ->
                    FileOutputStream(tempZip, appendMode).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
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
                        val msg = "SHA-256 checksum mismatch for plugin ${item.id}. Download aborted and file deleted."
                        Log.e(TAG, msg)
                        return@withContext Result.failure(SecurityException(msg))
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

            // 4. Update installed.json record
            updateInstalledRecord(item.id, item.version, versionedTargetDir.absolutePath)

            Log.i(TAG, "Successfully installed plugin ${item.id} v${item.version} at ${versionedTargetDir.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install plugin ${item.id}", e)
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
            Log.i(TAG, "Successfully uninstalled plugin ${item.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to uninstall plugin ${item.id}", e)
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
        val canonicalTargetDir = targetDir.canonicalPath
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)

                // Zip-Slip / Path-Traversal Prevention
                val canonicalOutFile = outFile.canonicalPath
                if (!canonicalOutFile.startsWith(canonicalTargetDir)) {
                    throw SecurityException("Zip-Slip vulnerability detected: ${entry.name} targets outside $canonicalTargetDir")
                }

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
