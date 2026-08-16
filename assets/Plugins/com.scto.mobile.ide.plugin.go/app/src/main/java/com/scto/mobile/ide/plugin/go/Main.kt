package com.scto.mobile.ide.plugin.go

import android.app.Activity
import android.os.Bundle
import androidx.annotation.Keep
import com.rk.extension.ExtensionAPI
import com.rk.extension.ExtensionContext
import com.rk.file.child
import com.rk.icons.Icon
import com.rk.lsp.LspRegistry
import com.rk.runner.RunnerManager
import com.rk.utils.getTempDir
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.writeText
import com.scto.mobile.ide.plugin.go.runner.GoRunner

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