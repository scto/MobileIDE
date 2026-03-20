package com.mobileide.app.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore

/**
 * Converts Android content [Uri]s to filesystem paths.
 *
 * Handles:
 * - Tree URIs (folder picker)
 * - Document URIs (file picker)
 * - External-storage documents
 * - Downloads folder
 * - Media (image/video/audio)
 *
 * Usage:
 * ```kotlin
 * val path = PathUtils.toPath(context, uri)
 * ```
 */
object PathUtils {

    fun toPath(context: Context, uri: Uri): String =
        convert(context, uri)
            .replace("/document", "/storage")
            .replace(":", "/")

    private fun convert(context: Context, uri: Uri): String {
        when {
            DocumentsContract.isTreeUri(uri) -> {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                return if (docId.startsWith("primary:"))
                    "${Environment.getExternalStorageDirectory()}/${docId.substringAfter("primary:")}"
                else {
                    val split = docId.split(":")
                    "/storage/${split[0]}/${split.getOrElse(1) { "" }}"
                }
            }
            DocumentsContract.isDocumentUri(context, uri) -> {
                val docId = DocumentsContract.getDocumentId(uri)
                when {
                    isExternalStorage(uri) -> {
                        val split = docId.split(":")
                        return if ("primary".equals(split[0], ignoreCase = true))
                            "${Environment.getExternalStorageDirectory()}/${split.getOrElse(1) { "" }}"
                        else
                            "/storage/${split[0]}/${split.getOrElse(1) { "" }}"
                    }
                    isDownloads(uri) -> {
                        val name = getDataColumn(context, uri, null, null)
                        return "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$name"
                    }
                    isMedia(uri) -> {
                        val split = docId.split(":")
                        val contentUri: Uri? = when (split[0]) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else    -> null
                        }
                        return getDataColumn(context, contentUri, "_id=?", arrayOf(split[1]))
                    }
                }
            }
            uri.scheme == "file" -> return uri.path ?: ""
        }
        return uri.path ?: ""
    }

    private fun isExternalStorage(uri: Uri) =
        "com.android.externalstorage.documents" == uri.authority

    private fun isDownloads(uri: Uri) =
        "com.android.providers.downloads.documents" == uri.authority

    private fun isMedia(uri: Uri) =
        "com.android.providers.media.documents" == uri.authority

    private fun getDataColumn(
        context: Context, uri: Uri?, selection: String?, args: Array<String>?,
    ): String {
        if (uri == null) return ""
        return try {
            context.contentResolver.query(uri, arrayOf("_data"), selection, args, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else ""
                } ?: ""
        } catch (e: Exception) { "" }
    }
}
