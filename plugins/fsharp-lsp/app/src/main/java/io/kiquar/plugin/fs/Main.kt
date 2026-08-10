package io.kiquar.plugin.fs

import android.app.Activity
import android.os.Bundle
import androidx.annotation.Keep
import com.scto.mobile.ide.features.extensions.ExtensionAPI
import com.scto.mobile.ide.features.extensions.ExtensionContext
import com.scto.mobile.ide.core.common.files.FileTypeManager
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import java.io.File

@Keep
@Suppress("unused")
class Main(context: ExtensionContext) : ExtensionAPI(context) {
    private var fileResolver: AssetsFileResolver? = null
    private var fsLanguage: FSLanguage? = null
	private var fsprojLanguage: FSProjLanguage? = null

    override fun onExtensionLoaded() {
        val fileProviderRegistry = FileProviderRegistry.getInstance()
        fileResolver = AssetsFileResolver(context.assets)
        fileProviderRegistry.addFileProvider(fileResolver)

        val grammarRegistry = GrammarRegistry.getInstance()
        grammarRegistry.loadGrammars("lang/language.json")

        FSLanguage(context.resources).also {
            fsLanguage = it
            FileTypeManager.register(it)
        }
		FSProjLanguage(context.resources).also {
            fsprojLanguage = it
            FileTypeManager.register(it)
        }
    }

    override fun onInstalled() {
    }

    override fun onUpdated() {
        dispose()
    }

    override fun onUninstalled() {
        dispose()
    }

    private fun dispose() {
        val fileProviderRegistry = FileProviderRegistry.getInstance()
        fileResolver?.let {
            fileProviderRegistry.removeFileProvider(it)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
}