package com.scto.mobile.ide.features.extensions.manager

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import com.scto.mobile.ide.features.extensions.EXTENSION_API_BASE
import com.scto.mobile.ide.features.extensions.ExtensionManifest
import com.scto.mobile.ide.features.extensions.ICONPACKS_API_BASE
import com.scto.mobile.ide.features.extensions.THEMES_API_BASE
import com.scto.mobile.ide.icons.pack.IconPackManifest
import com.scto.mobile.ide.settings.extension.InstallState
import com.scto.mobile.ide.utils.errorDialog
import com.scto.mobile.ide.utils.okHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@Serializable private data class ExtensionListResponse(val extensions: List<ExtensionEntry>)

@Serializable
data class ExtensionEntry(
    val id: String,
    val manifest: ExtensionManifest,
    val downloads: Int? = null,
    val download: DownloadUrls,
    val size: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class DownloadUrls(
    val icon: String? = null,
    val readme: String? = null,
    val zip: String,
    val size: Long? = null,
)

@Serializable data class ThemeListResponse(val themes: List<ThemeEntry>)

@Serializable
data class ThemeEntry(
    val id: String,
    val manifest: ThemeManifest,
    val createdAt: Int,
    val updatedAt: Int,
)

@Serializable
data class ThemeManifest(
    val id: String,
    val name: String,
)

@Serializable data class IconPackListResponse(val iconPacks: List<IconPackEntry>)

@Serializable
data class IconPackEntry(
    val id: String,
    val manifest: IconPackManifest,
    val createdAt: Int,
    val updatedAt: Int,
)

object ExtensionRegistry {
    private const val TAG = "ExtensionRegistry"
    private const val BASE_URL = EXTENSION_API_BASE

    private val client: OkHttpClient = okHttpClient
    private val json = Json {
        ignoreUnknownKeys = true
        allowTrailingComma = true
    }

    val downloadProgress = mutableStateMapOf<String, Float>()
    val activeInstalls = mutableStateMapOf<String, InstallState>()

    suspend fun downloadFileWithProgress(
        url: String,
        destFile: File,
        onProgress: (progress: Float) -> Unit,
    ): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    val totalBytes = response.body.contentLength()
                    destFile.parentFile?.mkdirs()
                    response.body.byteStream().use { input ->
                        destFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                if (totalBytes > 0) {
                                    val progress = totalBytesRead.toFloat() / totalBytes
                                    onProgress(progress)
                                } else {
                                    onProgress(-1f)
                                }
                            }
                        }
                    }
                }
                true
            }
                .onFailure {
                    it.printStackTrace()
                }
                .getOrElse { false }
        }

    suspend fun fetchExtensions(): List<ExtensionEntry> =
        withContext(Dispatchers.IO) {
            runCatching {
                val jsonString = requestJson(BASE_URL)
                val response = json.decodeFromString<ExtensionListResponse>(jsonString)
                response.extensions
            }
                .onFailure {
                    it.printStackTrace()
                    throw it
                }
                .getOrElse { emptyList() }
        }

    fun getIconUrl(id: String): String = "$BASE_URL/$id/icon.png"

    fun getReadmeUrl(id: String): String = "$BASE_URL/$id/README.md"

    fun getChangelogUrl(id: String): String = "$BASE_URL/$id/CHANGELOG.md"

    private fun requestJson(url: String): String {
        val req = Request.Builder().url(url).build()
        return client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("HTTP ${res.code}")
            val body = res.body.string()
            Log.d(TAG, body)
            body
        }
    }

    suspend fun downloadZip(manifest: ExtensionManifest, destFile: File): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val zipUrl = "$BASE_URL/${manifest.id}/plugin.zip"

                val request = Request.Builder().url(zipUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    destFile.parentFile?.mkdirs()
                    response.body.byteStream().use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                true
            }
                .onFailure {
                    it.printStackTrace()
                    errorDialog(throwable = it)
                }
                .getOrElse { false }
        }

    suspend fun fetchThemes(): List<ThemeEntry> =
        withContext(Dispatchers.IO) {
            runCatching {
                val jsonString = requestJson(THEMES_API_BASE)
                val response = json.decodeFromString<ThemeListResponse>(jsonString)
                response.themes
            }
                .onFailure {
                    it.printStackTrace()
                }
                .getOrElse { emptyList() }
        }

    suspend fun fetchIconPacks(): List<IconPackEntry> =
        withContext(Dispatchers.IO) {
            runCatching {
                val jsonString = requestJson(ICONPACKS_API_BASE)
                val response = json.decodeFromString<IconPackListResponse>(jsonString)
                response.iconPacks
            }
                .onFailure {
                    it.printStackTrace()
                }
                .getOrElse { emptyList() }
        }

    suspend fun downloadIconPackZip(id: String, destFile: File): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val zipUrl = "${ICONPACKS_API_BASE}/$id/iconpack.zip"
                val request = Request.Builder().url(zipUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    destFile.parentFile?.mkdirs()
                    response.body.byteStream().use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                true
            }
                .onFailure {
                    it.printStackTrace()
                    errorDialog(throwable = it)
                }
                .getOrElse { false }
        }
}
