package com.wuxianggujun.tinaide.core.apkbuilder

import android.content.Context
import timber.log.Timber
import java.io.File

/**
 * Manages template APK extraction from assets and caching.
 *
 * Templates live in `assets/apk_templates/` and are extracted to
 * `filesDir/apk_templates/`.
 *
 * 导出 APK 时优先刷新缓存，避免用户升级后仍命中旧模板。
 */
class ApkTemplate(private val context: Context) {

    companion object {
        private const val TAG = "ApkTemplate"
        private const val ASSETS_DIR = "apk_templates"
    }

    private val cacheDir = File(context.filesDir, "apk_templates")

    fun getTemplateFile(type: ApkTemplateType): File? {
        val cached = File(cacheDir, type.templateFileName)
        return try {
            cacheDir.mkdirs()
            context.assets.open("$ASSETS_DIR/${type.templateFileName}").use { input ->
                cached.outputStream().use { output -> input.copyTo(output) }
            }
            Timber.tag(TAG).i("Template refreshed: ${type.templateFileName}")
            cached
        } catch (e: Exception) {
            if (cached.exists()) {
                Timber.tag(TAG).w(
                    e,
                    "Failed to refresh template from assets, using cached copy: %s",
                    type.templateFileName
                )
                cached
            } else {
                Timber.tag(TAG).e(e, "Template not found in assets: ${type.templateFileName}")
                null
            }
        }
    }

    fun isTemplateAvailable(type: ApkTemplateType): Boolean {
        val cached = File(cacheDir, type.templateFileName)
        if (cached.exists()) return true
        return try {
            context.assets.open("$ASSETS_DIR/${type.templateFileName}").use { }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearCache() {
        cacheDir.deleteRecursively()
        Timber.tag(TAG).d("Template cache cleared")
    }
}
