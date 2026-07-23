package com.scto.mobile.ide.extension.languages

import com.scto.mobile.ide.extension.ExtensionAPI
import com.scto.mobile.ide.extension.ExtensionContext
import com.scto.mobile.ide.lsp.LspRegistry

class KotlinLspExtension(context: ExtensionContext) : ExtensionAPI(context) {

    override fun onInstalled() {}

    override fun onExtensionLoaded() {
        LspRegistry.registerServer(KotlinLspServer())
    }

    override fun onUpdated() {}

    override fun onUninstalled() {}
}
