package com.scto.mobile.ide.plugins.java.lsp

import android.app.Activity
import android.os.Bundle
import androidx.annotation.Keep
import com.rk.extension.ActivityProvider
import com.rk.extension.ExtensionAPI
import com.rk.extension.ExtensionContext
import com.scto.mobile.ide.core.common.files.child
import com.scto.mobile.ide.core.common.files.createDirIfNot
import com.scto.mobile.ide.core.common.files.localBinDir
import com.scto.mobile.ide.core.common.icons.Icon
import com.rk.lsp.LspRegistry

@Keep
@Suppress("unused")
class Main(context: ExtensionContext) : ExtensionAPI(context) {
    private var javaServer: JavaServer? = null

    override fun onExtensionLoaded() {
        // Copy LSP install script
        val javaAssetStream = context.assets.open("java-lsp.sh")
        val javaAsset = javaAssetStream.bufferedReader().use { it.readText() }
        val lspScriptDir = localBinDir().child("lsp").createDirIfNot()
        val javaInstallScript = lspScriptDir.child("java-lsp.sh").also {
            it.writeText(javaAsset)
        }

        javaServer = JavaServer(Icon.ExternalResourceIcon(R.drawable.java, context.resources), javaInstallScript).also {
            LspRegistry.registerServer(it)
        }
    }

    private fun dispose() {
        javaServer?.let {
            LspRegistry.unregisterServer(it)
        }
    }

    override fun onUninstalled() {
        ActivityProvider.currentActivity?.let {
            javaServer?.uninstall(it)
        }
        dispose()
    }

    override fun onUpdated() {
        dispose()
    }

    override fun onInstalled() {}

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}
}
