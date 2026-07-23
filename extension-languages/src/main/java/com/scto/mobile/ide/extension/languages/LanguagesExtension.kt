package com.scto.mobile.ide.extension.languages

import android.content.Context
import com.scto.mobile.ide.extension.ExtensionAPI
import com.scto.mobile.ide.extension.ExtensionContext
import com.scto.mobile.ide.lsp.LspRegistry

class LanguagesExtension(context: ExtensionContext) : ExtensionAPI(context) {

    override fun onInstalled() {
        // Initialization if needed
    }

    override fun onExtensionLoaded() {
        // Register all language servers supported by this extension
    }

    override fun onUpdated() {
    }

    override fun onUninstalled() {
        // Clean up
    }
}
