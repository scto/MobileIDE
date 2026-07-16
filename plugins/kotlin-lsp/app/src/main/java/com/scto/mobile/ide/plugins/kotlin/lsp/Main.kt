package com.scto.mobile.ide.plugins.kotlin.lsp

import android.app.Activity
import android.os.Bundle
import androidx.annotation.Keep
import com.rk.extension.ExtensionAPI
import com.rk.extension.ExtensionContext
import com.rk.file.child
import com.rk.icons.Icon
import com.rk.lsp.LspRegistry
import com.rk.utils.getTempDir
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.writeText

@Keep
@Suppress("unused")
class Main(context: ExtensionContext) : ExtensionAPI(context) {
    private var kotlinServer: KotlinServer? = null

    override fun onInstalled() {

    }

    override fun onExtensionLoaded() {
        kotlinServer = KotlinServer(
            installScript = acquireLspInstallScript(),
			context = context
        ).also {
            LspRegistry.registerServer(it)
        }
    }

    private fun acquireLspInstallScript(): File {
        val assetStream = context.assets.open("kotlin-lsp-installer.sh")
        val assetContent = assetStream.bufferedReader().use { it.readText() }
        val scriptFile = getTempDir().child("kotlin-lsp-installer.sh").also {
            it.writeText(assetContent)
            it.setExecutable(true)
        }
        return scriptFile
    }

    private fun dispose() {
        kotlinServer?.let {
            LspRegistry.unregisterServer(it)
        }
    }

    override fun onUpdated() {
        dispose()
    }

    override fun onUninstalled() {
        context.currentActivity?.let { activity ->
            runBlocking {
                val isInstalled = kotlinServer?.isInstalled(activity) ?: false
                if (isInstalled) {
                    kotlinServer?.uninstall(activity)
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