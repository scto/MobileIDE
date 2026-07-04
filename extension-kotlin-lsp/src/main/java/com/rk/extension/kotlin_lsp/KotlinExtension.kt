package com.rk.extension.kotlin_lsp

import android.content.Context
import com.rk.extension.ExtensionAPI
import com.rk.extension.ExtensionContext
import com.rk.lsp.LspRegistry
import com.rk.commands.CommandManager

class KotlinExtension(context: ExtensionContext) : ExtensionAPI(context) {

    override fun onInstalled() {
        // Initialization if needed
    }

    override fun onExtensionLoaded() {
        // Register the Kotlin LSP Server
        LspRegistry.registerServer(KotlinLspServer())
        
        // Example: Register a custom command
        // CommandManager.registerCommand(RestartKotlinServerCommand())
    }

    override fun onUpdated() {
    }

    override fun onUninstalled() {
        // Clean up
    }
}
