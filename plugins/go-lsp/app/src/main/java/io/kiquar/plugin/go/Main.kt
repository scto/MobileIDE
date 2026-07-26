package io.kiquar.plugin.go

import android.app.Activity
import android.os.Bundle
import androidx.annotation.Keep
import com.scto.mobile.ide.extension.ExtensionAPI
import com.scto.mobile.ide.extension.ExtensionContext
import com.scto.mobile.ide.core.common.files.child
import com.scto.mobile.ide.lsp.LspRegistry
import com.scto.mobile.ide.runner.RunnerManager
import com.scto.mobile.ide.core.common.files.getCacheDir as getTempDir
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.writeText
import io.kiquar.plugin.go.runner.GoRunner

@Keep
@Suppress("unused")
class Main(context: ExtensionContext) : ExtensionAPI(context) {
    private var goServer: GoServer? = null
	private var goRunner: GoRunner? = null

    override fun onInstalled() {

    }

    override fun onExtensionLoaded() {
        goServer = GoServer(
            installScript = acquireLspInstallScript(),
			context = context
        ).also {
            LspRegistry.registerServer(it)
        }
		goRunner = GoRunner().also {
            RunnerManager.registerRunner(it)
        }
    }

    private fun acquireLspInstallScript(): File {
        val assetStream = context.assets.open("gopls-installer.sh")
        val assetContent = assetStream.bufferedReader().use { it.readText() }
        val scriptFile = getTempDir().child("gopls-installer.sh").also {
            it.writeText(assetContent)
            it.setExecutable(true)
        }
        return scriptFile
    }

    private fun dispose() {
        goServer?.let {
            LspRegistry.unregisterServer(it)
        }
		goRunner?.let {
            RunnerManager.unregisterRunner(it)
        }
    }

    override fun onUpdated() {
        dispose()
    }

    override fun onUninstalled() {
        context.currentActivity?.let { activity ->
            runBlocking {
                val isInstalled = goServer?.isInstalled(activity) ?: false
                if (isInstalled) {
                    goServer?.uninstall(activity)
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