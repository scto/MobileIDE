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
        private const val LOCAL_ASSET_CATALOG_PATH = "Plugins/LSP/catalog.json"
        private const val LOCAL_ASSET_DIR_PATH = "Plugins/LSP"
    }

    private val extensionsDir: File
        get() = File(context.filesDir.parentFile, "local/extensions").apply { mkdirs() }

    private val cachePluginsDir: File
        get() = File(context.filesDir, "cache/plugins").apply { mkdirs() }

    private val installedJsonFile: File
        get() = File(extensionsDir, "installed.json")

    private val catalogCacheFile: File
        get() = File(context.cacheDir, "catalog_cache.json")

    suspend fun fetchPluginList(remoteUrl: String = DEFAULT_INDEX_URL): List<StorePluginItem> =
        withContext(Dispatchers.IO) {
            val assetItems = fetchAssetCatalog()
            val remoteItems = fetchRemoteCatalog(remoteUrl)
            val cachedItems = if (remoteItems.isEmpty()) fetchCachedCatalog() else emptyList()

            // Merge by plugin-id (Priority 1: Asset, Priority 2: Remote, Priority 3: Cache)
            // In case of ID collision between asset and remote, higher version wins
            val mergedMap = LinkedHashMap<String, StorePluginItem>()

            // First add cached
            for (item in cachedItems) {
                mergedMap[item.id] = item
            }

            // Then remote
            for (item in remoteItems) {
                val existing = mergedMap[item.id]
                if (existing == null || isVersionGreater(item.version, existing.version)) {
                    mergedMap[item.id] = item
                }
            }

            // Save remote/cached to cache file if remote was fetched
            if (remoteItems.isNotEmpty()) {
                saveCatalogToCache(remoteItems)
            }

            // Priority 1: Assets (Always present, compare version if present in remote)
            for (assetItem in assetItems) {
                val existing = mergedMap[assetItem.id]
                if (existing == null || !isVersionGreater(existing.version, assetItem.version)) {
                    mergedMap[assetItem.id] = assetItem
                } else {
                    // Remote has higher version than asset, but preserve isAsset or mark as Remote
                    mergedMap[assetItem.id] = existing.copy(isAsset = false)
                }
            }

            // Resolve local installation status for all items
            return@withContext mergedMap.values.map { item ->
                resolveInstallationStatus(item)
            }
        }

    private fun fetchAssetCatalog(): List<StorePluginItem> {
        return try {
            val inputStream = context.assets.open(LOCAL_ASSET_CATALOG_PATH)
            val jsonText = inputStream.bufferedReader().use { it.readText() }
            parsePluginIndexJson(jsonText, defaultIsAsset = true)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load local asset catalog from $LOCAL_ASSET_CATALOG_PATH: ${e.message}")
            emptyList()
        }
    }

    private fun fetchRemoteCatalog(indexUrl: String): List<StorePluginItem> {
        if (indexUrl.isEmpty()) return emptyList()
        return try {
            val connection = (URL(indexUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
            }
            if (connection.responseCode == 200) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                Log.i(TAG, "Fetched remote plugin index successfully.")
                parsePluginIndexJson(jsonText, defaultIsAsset = false)
            } else {
                Log.w(TAG, "Remote plugin index returned HTTP ${connection.responseCode}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch remote plugin catalog: ${e.message}")
            emptyList()
        }
    }

    private fun fetchCachedCatalog(): List<StorePluginItem> {
        if (!catalogCacheFile.exists()) return emptyList()
        return try {
            val jsonText = catalogCacheFile.readText()
            parsePluginIndexJson(jsonText, defaultIsAsset = false)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read cached catalog: ${e.message}")
            emptyList()
        }
    }

    private fun saveCatalogToCache(items: List<StorePluginItem>) {
        try {
            val root = JSONObject()
            val array = JSONArray()
            for (item in items) {
                array.put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("version", item.version)
                    put("description", item.description)
                    put("category", item.type.name.lowercase())
                    put("downloadUrl", item.downloadUrl)
                    put("sizeBytes", item.size)
                    put("sha256", item.sha256 ?: "")
                    put("dependencies", JSONArray(item.dependencies))
                    put("fileExtensions", JSONArray(item.fileExtensions))
                })
            }
            root.put("plugins", array)
            catalogCacheFile.writeText(root.toString(2))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save catalog to cache", e)
        }
    }

    private fun parsePluginIndexJson(jsonText: String, defaultIsAsset: Boolean = false): List<StorePluginItem> {
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
                var downloadUrl = obj.optString("downloadUrl", "")
                val size = obj.optLong("sizeBytes", obj.optLong("size", 0L))
                val minAppVersion = obj.optInt("minAppVersion", 1)
                val sha256 = obj.optString("sha256", "").ifBlank { null }

                val isAsset = defaultIsAsset || (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://") && !downloadUrl.startsWith("asset://"))

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

                val extList = mutableListOf<String>()
                val extArr = obj.optJSONArray("fileExtensions")
                if (extArr != null) {
                    for (j in 0 until extArr.length()) {
                        extList.add(extArr.getString(j))
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
                        dependencies = depList,
                        fileExtensions = extList,
                        sha256 = sha256,
                        isAsset = isAsset
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

            val tempZip: File
            val isAsset = item.isAsset || (!item.downloadUrl.startsWith("http://") && !item.downloadUrl.startsWith("https://"))

            if (isAsset) {
                // Asset installation flow: copy from APK assets to staging cache dir
                val assetRelativePath = if (item.downloadUrl.startsWith("./")) {
                    "$LOCAL_ASSET_DIR_PATH/${item.downloadUrl.removePrefix("./")}"
                } else if (item.downloadUrl.startsWith("asset://")) {
                    item.downloadUrl.removePrefix("asset://")
                } else if (!item.downloadUrl.contains("/")) {
                    "$LOCAL_ASSET_DIR_PATH/${item.downloadUrl}"
                } else {
                    item.downloadUrl
                }

                tempZip = File(cachePluginsDir, "${item.id}-${item.version}.zip")
                if (tempZip.exists()) {
                    tempZip.delete()
                }

                onProgress(0.1f)
                context.assets.open(assetRelativePath).use { input ->
                    FileOutputStream(tempZip).use { output ->
                        input.copyTo(output)
                    }
                }
                onProgress(0.5f)

                // Verify SHA-256 from catalog.json if available
                if (!item.sha256.isNullOrBlank()) {
                    if (!verifySha256(tempZip, item.sha256)) {
                        tempZip.delete()
                        val msg = "SHA-256 Prüfsummen-Fehler bei Asset Plugin ${item.id}. Installation abgebrochen."
                        Log.e(TAG, msg)
                        return@withContext Result.failure(SecurityException(msg))
                    }
                }
            } else {
                if (!item.downloadUrl.startsWith("https://")) {
                    val msg = "Insecure HTTP downloadUrl rejected. HTTPS is strictly required for security."
                    Log.e(TAG, msg)
                    return@withContext Result.failure(SecurityException(msg))
                }

                tempZip = File(cachePluginsDir, "${item.id}-${item.version}.zip")
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
            }

            // Extract tempZip into target versioned directory and root directory with structure normalization
            onProgress(0.8f)
            tempZip.inputStream().use { stream ->
                extractZipStreamNormalized(stream, versionedTargetDir)
            }
            tempZip.inputStream().use { stream ->
                extractZipStreamNormalized(stream, rootPluginDir)
            }
            tempZip.delete()
            onProgress(1.0f)

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

    private fun extractZipStreamNormalized(inputStream: InputStream, targetDir: File) {
        val bytes = inputStream.readBytes()
        var prefixToRemove: String? = null

        // Pass 1: detect if all files are inside a single top-level directory and manifest.json/plugin.json is inside it
        ZipInputStream(bytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            var topLevelDir: String? = null
            var singleTopLevel = true
            var foundManifestAtRoot = false

            while (entry != null) {
                val name = entry.name.replace('\\', '/')
                if (name == "manifest.json" || name == "plugin.json") {
                    foundManifestAtRoot = true
                }
                val parts = name.split('/').filter { it.isNotEmpty() }
                if (parts.isNotEmpty()) {
                    if (topLevelDir == null) {
                        topLevelDir = parts[0]
                    } else if (parts[0] != topLevelDir) {
                        singleTopLevel = false
                    }
                }
                entry = zis.nextEntry
            }

            if (!foundManifestAtRoot && singleTopLevel && topLevelDir != null) {
                prefixToRemove = "$topLevelDir/"
            }
        }

        // Pass 2: Extract and normalize path
        val canonicalTargetDir = targetDir.canonicalPath
        ZipInputStream(bytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                var entryName = entry.name.replace('\\', '/')
                if (prefixToRemove != null && entryName.startsWith(prefixToRemove)) {
                    entryName = entryName.substring(prefixToRemove.length)
                }

                if (entryName.isNotEmpty()) {
                    val outFile = File(targetDir, entryName)
                    val canonicalOutFile = outFile.canonicalPath
                    if (!canonicalOutFile.startsWith(canonicalTargetDir)) {
                        throw SecurityException("Zip-Slip vulnerability detected: ${entry.name} targets outside $canonicalTargetDir")
                    }

                    if (entry.isDirectory || entryName.endsWith("/")) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { output ->
                            zis.copyTo(output)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun extractZipStream(inputStream: InputStream, targetDir: File) {
        extractZipStreamNormalized(inputStream, targetDir)
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
