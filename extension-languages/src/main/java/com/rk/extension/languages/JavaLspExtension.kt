package com.rk.extension.languages

import com.rk.extension.ExtensionAPI
import com.rk.extension.ExtensionContext
import com.rk.lsp.LspRegistry

class JavaLspExtension(context: ExtensionContext) : ExtensionAPI(context) {

    override fun onInstalled() {}

    override fun onExtensionLoaded() {
        LspRegistry.registerServer(JavaLspServer())
    }

    override fun onUpdated() {}

    override fun onUninstalled() {}
}
