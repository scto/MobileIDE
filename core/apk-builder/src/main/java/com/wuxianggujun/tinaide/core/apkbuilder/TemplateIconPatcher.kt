package com.wuxianggujun.tinaide.core.apkbuilder

import com.reandroid.apk.ApkModule
import timber.log.Timber
import java.io.File

/**
 * 读取模板 APK 的 resources.arsc，定位 launcher 图标（mipmap/ic_launcher）对应的 ZIP 条目路径。
 * AAPT2 压缩发布构建时，会把 `res/mipmap-<dpi>/ic_launcher.webp` 之类的路径重命名为 `res/XX.webp`。
 * 直接改包时需要先拿到真实路径，再按路径覆盖字节。
 */
internal object TemplateIconPatcher {

    private const val TAG = "TemplateIconPatcher"
    private const val LAUNCHER_ENTRY_NAME = "ic_launcher"
    private val LAUNCHER_TYPE_NAMES = setOf("mipmap", "drawable")

    /**
     * 返回模板 APK 内所有对应 `@mipmap/ic_launcher` 或 `@drawable/ic_launcher` 的 ZIP 条目路径。
     * 若模板里压根没有对应的资源（例如用户给的模板没有 launcher 图标），返回空集合。
     */
    fun findLauncherIconPaths(templateApk: File): Set<String> {
        val paths = linkedSetOf<String>()
        runCatching {
            val module = ApkModule.loadApkFile(templateApk)
            for (resFile in module.listResFiles()) {
                val entry = resFile.pickOne() ?: continue
                if (entry.name == LAUNCHER_ENTRY_NAME &&
                    entry.typeName in LAUNCHER_TYPE_NAMES
                ) {
                    paths.add(resFile.filePath)
                }
            }
        }.onFailure { error ->
            Timber.tag(TAG).w(error, "Failed to scan launcher icon paths in template %s", templateApk.name)
        }
        return paths
    }
}
