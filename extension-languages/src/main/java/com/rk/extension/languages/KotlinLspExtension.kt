package com.rk.extension.languages

import com.rk.extension.ExtensionAPI
import com.rk.extension.ExtensionContext
import com.rk.lsp.LspRegistry

class KotlinLspExtension(context: ExtensionContext) : ExtensionAPI(context) {

    override fun onInstalled() {}

    override fun onExtensionLoaded() {
        LspRegistry.registerServer(KotlinLspServer())
    }

    override fun onUpdated() {}

    override fun onUninstalled() {}
}
