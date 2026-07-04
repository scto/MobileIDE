package com.rk.extension.languages

import android.content.Context
import com.rk.extension.ExtensionAPI
import com.rk.extension.ExtensionContext
import com.rk.lsp.LspRegistry

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
