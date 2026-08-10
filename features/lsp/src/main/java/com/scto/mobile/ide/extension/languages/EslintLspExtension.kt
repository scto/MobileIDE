package com.scto.mobile.ide.features.extensions.languages

import com.scto.mobile.ide.features.extensions.ExtensionAPI
import com.scto.mobile.ide.features.extensions.ExtensionContext
import com.scto.mobile.ide.lsp.LspRegistry

class EslintLspExtension(context: ExtensionContext) : ExtensionAPI(context) {
    override fun onInstalled() {}
    override fun onExtensionLoaded() {
        LspRegistry.registerServer(EslintLspServer())
    }
    override fun onUpdated() {}
    override fun onUninstalled() {}
}
