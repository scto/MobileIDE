package com.scto.mobile.ide.icons.pack





import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.pm.PackageInfoCompat
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.activities.settings.SettingsActivity
import com.scto.mobile.ide.common.XedPackage
import com.scto.mobile.ide.extension.manager.StoreManager
import com.scto.mobile.ide.extension.model.PackageCache
import com.scto.mobile.ide.file.FileOperations
import com.scto.mobile.ide.file.FileWrapper
import com.scto.mobile.ide.file.child
import com.scto.mobile.ide.file.createDirIfNot
import com.scto.mobile.ide.file.localDir
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.utils.application
import com.scto.mobile.ide.utils.dialogRes
import com.scto.mobile.ide.utils.logError
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json











@Serializable
data class IconPackEntry(
    val id: String,
    val manifest: IconPackManifest,
    val size: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

val currentIconPack = mutableStateOf<LocalIconPack?>(null)
val iconPackDir = localDir().child("icon_pack").also { it.createDirIfNot() }

class IconPackManager(private val context: Application) : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        allowTrailingComma = true
    }

    val localIconPacks = mutableStateMapOf<String, LocalIconPack>()
    val storeIconPacks = mutableStateMapOf<String, StoreIconPack>()

    fun isInstalled(id: String) = localIconPacks.containsKey(id)

    fun getIconPackPackage(id: String): IconPackPackage? {
        val local = localIconPacks[id]
        val store = storeIconPacks[id]

        return when {
            local != null && store != null -> UpdatableIconPack(local, store)
            local != null -> local
            store != null -> store
            else -> null
        }
    }

    fun getSyncedIconPacks(): List<IconPackPackage> {
        val allIds = localIconPacks.keys + storeIconPacks.keys
        return allIds.mapNotNull { getIconPackPackage(it) }
    }

    private suspend fun calcSize(dir: File): Long {
        return FileOperations.calculateContent(FileWrapper(dir)).totalSize
    }

    private fun resolveCache(dir: File): PackageCache {
        val cacheFile = dir.resolve("cache.json")

        if (!cacheFile.exists() || !cacheFile.isFile) {
            return PackageCache()
        }

        return runCatching {
            json.decodeFromString<PackageCache>(cacheFile.readText())
        }
            .getOrElse {
                PackageCache()
            }
    }

    private fun writeCache(dir: File, cache: PackageCache) {
        val cacheFile = dir.resolve("cache.json")
        cacheFile.writeText(json.encodeToString(cache))
    }

    suspend fun invalidateSize(pkg: IconPackPackage) {
        if (pkg is StoreIconPack) return

        withContext(Dispatchers.IO) {
            val dir = iconPackDir.resolve(pkg.id)
            val cache = resolveCache(dir)
            val newSize = calcSize(dir)
            writeCache(dir, cache.copy(size = newSize))

            withContext(Dispatchers.Main) {
                localIconPacks[pkg.id]?.size = newSize
            }
        }
    }

    suspend fun installIconPack(xedFile: File) =
        withContext(Dispatchers.IO) {
            val tempDir = File(context.cacheDir, "icon_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                XedPackage.extract(xedFile, tempDir)
                installIconPackFromDir(tempDir)
            } finally {
                tempDir.deleteRecursively()
            }
        }

    private suspend fun installIconPackFromDir(dir: File) {
        val iconPackManifest = validateIconPack(dir) ?: return

        val packageName = application!!.packageName
        val packageManager = application!!.packageManager
        val currentVersionCode = PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))
        if (iconPackManifest.minAppVersion != null && iconPackManifest.minAppVersion.toLong() > currentVersionCode) {
            dialogRes(
                activity = SettingsActivity.instance,
                title = com.scto.mobile.ide.core.main.R.string.warning.getString(),
                msg = com.scto.mobile.ide.core.main.R.string.incompatible_icon_pack_warning.getString(),
                cancelRes = com.scto.mobile.ide.core.main.R.string.cancel,
                okRes = com.scto.mobile.ide.core.main.R.string.continue_action,
                onOk = {
                    DefaultScope.launch {
                        writeIconPackToDisk(iconPackManifest, dir)
                    }
                },
            )
            return
        }

        writeIconPackToDisk(iconPackManifest, dir)
    }

    private suspend fun writeIconPackToDisk(iconPackManifest: IconPackManifest, dir: File) {
        val installDir = iconPackDir.child(iconPackManifest.id)

        var oldCreatedAt: Long? = null
        if (installDir.exists()) {
            oldCreatedAt = resolveCache(installDir).createdAt
            uninstallIconPack(iconPackManifest.id)
        }

        dir.copyRecursively(installDir, overwrite = true)

        val size = calcSize(installDir)
        val newCache =
            PackageCache(
                createdAt = oldCreatedAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                size = size,
            )
        writeCache(installDir, newCache)

        indexLocalPacks()
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal fun validateIconPack(dir: File): IconPackManifest? {
        val iconPackJson = dir.resolve("manifest.json")
        if (!iconPackJson.exists()) {
            dialogRes(
                SettingsActivity.instance,
                com.scto.mobile.ide.core.main.R.string.icon_pack_install_failed.getString(),
                com.scto.mobile.ide.core.main.R.string.manifest_missing.getString(),
                cancelable = false,
            )

            return null
        }
        val iconPackManifest = runCatching {
            json.decodeFromString<IconPackManifest>(iconPackJson.readText())
        }
            .getOrElse { e ->
                if (e is MissingFieldException) {
                    val fields = e.missingFields.joinToString("\n") { "• $it" }
                    dialogRes(
                        SettingsActivity.instance,
                        com.scto.mobile.ide.core.main.R.string.icon_pack_install_failed.getString(),
                        com.scto.mobile.ide.core.main.R.string.manifest_missing_fields.getFilledString(fields),
                        cancelable = false,
                    )
                    return null
                }
                dialogRes(
                    SettingsActivity.instance,
                    com.scto.mobile.ide.core.main.R.string.icon_pack_install_failed.getString(),
                    e.localizedMessage ?: com.scto.mobile.ide.core.main.R.string.unknown_err.getString(),
                    cancelable = false,
                )
                return null
            }

        return iconPackManifest
    }

    fun uninstallIconPack(iconPackId: String) {
        val iconPack = localIconPacks[iconPackId] ?: return
        File(iconPack.installPath).deleteRecursively()
        localIconPacks.remove(iconPackId)
    }

    suspend fun indexStoreIconPacks() =
        withContext(Dispatchers.IO) {
            val packsList = runCatching { StoreManager.fetchIconPacks() }.getOrNull() ?: return@withContext
            val newPacks = packsList.associateBy({ it.id }, { StoreIconPack(it) })
            withContext(Dispatchers.Main) {
                storeIconPacks.clear()
                storeIconPacks.putAll(newPacks)
            }
        }

    suspend fun indexLocalPacks() = mutex.withLock {
        val newLocal = mutableMapOf<String, LocalIconPack>()
        withContext(Dispatchers.IO) {
            iconPackDir.listFiles()?.forEach { dir ->
                if (dir.isDirectory) {
                    val manifestJson = dir.resolve("manifest.json")
                    if (manifestJson.exists()) {
                        runCatching {
                            val iconPackManifest = json.decodeFromString<IconPackManifest>(manifestJson.readText())
                            val cache = resolveCache(dir)
                            val size = cache.size ?: calcSize(dir).also { writeCache(dir, cache.copy(size = it)) }

                            val iconPack =
                                LocalIconPack(
                                    manifest = iconPackManifest,
                                    installPath = dir.absolutePath,
                                    createdAt = cache.createdAt,
                                    updatedAt = cache.updatedAt,
                                    initSize = size,
                                )
                            newLocal[iconPackManifest.id] = iconPack
                        }
                            .onFailure {
                                logError(it, "Failed to index local icon pack")
                            }
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            localIconPacks.clear()
            localIconPacks.putAll(newLocal)
        }

        if (Settings.icon_pack.isNotEmpty()) {
            currentIconPack.value = localIconPacks[Settings.icon_pack]
        }
    }
}
