package com.koner.rust

import androidx.annotation.Keep
import com.scto.mobile.ide.features.extensions.ExtensionAPI
import com.scto.mobile.ide.features.extensions.ExtensionContext
import com.rk.file.BuiltinFileType
import com.rk.file.child
import com.scto.mobile.ide.lsp.LspRegistry
import com.rk.utils.getTempDir
import kotlinx.coroutines.runBlocking
import java.io.File

@Keep
@Suppress("unused")
class Main(context: ExtensionContext) : ExtensionAPI(context) {
    private var rustServer: RustServer? = null

    override fun onLoad() {
        val rustFileType = BuiltinFileType.RUST

        rustServer =
            RustServer(
                    context = context,
                    icon = rustFileType.icon!!,
                    supportedExtensions = rustFileType.extensions,
                    installScript = acquireLspInstallScript(),
                )
                .also {
                    LspRegistry.registerServer(it)
                }
    }

    private fun acquireLspInstallScript(): File {
        val rustAssetStreams = context.assets.open("rust-lsp.sh")
        val rustAsset = rustAssetStreams.bufferedReader().use { it.readText() }
        val rustLspScript =
            getTempDir().child("rust-lsp.sh").also {
                it.writeText(rustAsset)
            }
        return rustLspScript
    }

    override fun onDispose() {
        rustServer?.let {
            LspRegistry.unregisterServer(it)
        }
    }

    override fun onUninstalled() {
        context.currentActivity?.let {
            val isInstalled = runBlocking { rustServer?.isInstalled(it) } ?: false
            if (isInstalled) {
                rustServer?.uninstall(it)
            }
        }
    }
}
