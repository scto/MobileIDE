package com.scto.mobile.ide.plugin.zig

import android.app.Activity
import android.os.Bundle
import androidx.annotation.Keep
import com.scto.mobile.ide.features.extensions.ExtensionAPI
import com.scto.mobile.ide.features.extensions.ExtensionContext
import com.scto.mobile.ide.core.common.files.BuiltinFileType
import com.scto.mobile.ide.core.common.files.child
import com.scto.mobile.ide.core.common.icons.Icon
import com.scto.mobile.ide.lsp.LspRegistry
import com.scto.mobile.ide.runner.RunnerManager
import com.scto.mobile.ide.core.common.files.getCacheDir as getTempDir
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.writeText
import com.scto.mobile.ide.plugin.zig.runner.ZigRunner

@Keep
@Suppress("unused")
class Main(context: ExtensionContext) : ExtensionAPI(context) {
    private var zigServer: ZigServer? = null
	private var zigRunner: ZigRunner? = null

    override fun onInstalled() {
    }

    override fun onExtensionLoaded() {
        zigServer = ZigServer(
            installScript = acquireLspInstallScript()
        ).also {
            LspRegistry.registerServer(it)
        }
		zigRunner = ZigRunner().also {
            RunnerManager.registerRunner(it)
        }
    }

    private fun acquireLspInstallScript(): File {
        val zigAssetStreams = context.assets.open("zig-installer.sh")
        val zigAsset = zigAssetStreams.bufferedReader().use { it.readText() }
        val zigLspScript = getTempDir().child("zig-installer.sh").also {
            it.writeText(zigAsset)
            it.setExecutable(true) 
        }
        return zigLspScript
    }

    private fun dispose() {
        zigServer?.let {
            LspRegistry.unregisterServer(it)
        }
		zigRunner?.let {
            RunnerManager.unregisterRunner(it)
        }
    }

    override fun onUpdated() {
        dispose()
    }

    override fun onUninstalled() {
        context.currentActivity?.let { activity ->
            runBlocking {
                val isInstalled = zigServer?.isInstalled(activity) ?: false
                if (isInstalled) {
                    zigServer?.uninstall(activity)
                }
            }
        }
        dispose()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
}