package io.kiquar.plugin.zig

import android.app.Activity
import android.os.Bundle
import androidx.annotation.Keep
import com.rk.extension.ExtensionAPI
import com.rk.extension.ExtensionContext
import com.rk.file.BuiltinFileType
import com.rk.file.child
import com.rk.icons.Icon
import com.rk.lsp.LspRegistry
import com.rk.runner.RunnerManager
import com.rk.utils.getTempDir
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.writeText
import io.kiquar.plugin.zig.runner.ZigRunner

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