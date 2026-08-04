package com.scto.mobile.ide.features.extensions.languages

import android.content.Context
import com.scto.mobile.ide.features.extensions.ExtensionAPI
import com.scto.mobile.ide.features.extensions.ExtensionContext
import com.scto.mobile.ide.lsp.LspRegistry

class LanguagesExtension(context: ExtensionContext) : ExtensionAPI(context) {

    override fun onInstalled() {
        // Initialization if needed
    }

    override fun onExtensionLoaded() {
        LspRegistry.registerServer(BashLspServer())
        LspRegistry.registerServer(CssLspServer())
        LspRegistry.registerServer(EslintLspServer())
        LspRegistry.registerServer(EmmetLspServer())
        LspRegistry.registerServer(HtmlLspServer())
        LspRegistry.registerServer(MarkdownLspServer())
        LspRegistry.registerServer(TypeScriptLspServer())
        LspRegistry.registerServer(XmlLspServer())
        LspRegistry.registerServer(JavaLspServer())
        LspRegistry.registerServer(KotlinLspServer())
        LspRegistry.registerServer(PythonLspServer())
        LspRegistry.registerServer(CppLspServer())
        LspRegistry.registerServer(TomlLspServer())
        LspRegistry.registerServer(YamlLspServer())
    }

    override fun onUpdated() {
    }

    override fun onUninstalled() {
        // Clean up
    }
}
