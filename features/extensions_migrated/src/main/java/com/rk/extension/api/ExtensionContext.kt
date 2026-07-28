// DO NOT UPDATE PACKAGE NAME OTHERWISE EXTENSIONS WILL BREAK
package com.scto.mobile.ide.features.extensions

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import androidx.annotation.Keep
import com.scto.mobile.ide.features.extensions.api.ExtensionActivity
import com.scto.mobile.ide.features.extensions.api.ExtensionScreen
import com.scto.mobile.ide.features.extensions.api.logDebug
import com.scto.mobile.ide.features.extensions.api.logError
import com.scto.mobile.ide.features.extensions.api.logInfo
import com.scto.mobile.ide.features.extensions.api.logWarn
import com.scto.mobile.ide.file.createDirIfNot
import kotlinx.coroutines.CoroutineScope
import java.io.File

@Keep
class ExtensionContext(val extension: LocalExtension, val appContext: Context, val scope: CoroutineScope) {

    val settings = SharedPrefExtensionSettings(extension.id)

    val currentActivity
        get() = ActivityProvider.currentActivity

    val appResources by lazy {
        AppResources(appContext, appContext.resources, appContext.packageName)
    }

    val extensionFiles
        get() = File(extension.installPath).resolve("files").createDirIfNot()

    val assets: AssetManager by lazy {
        AssetManager::class.java.getDeclaredConstructor().newInstance().apply {
            val method = javaClass.getMethod("addAssetPath", String::class.java)
            method.invoke(this, extension.apkFile.absolutePath)
        }
    }

    val resources by lazy { Resources(assets, appContext.resources.displayMetrics, appContext.resources.configuration) }

    fun logDebug(msg: String) = extension.id.logDebug(msg)

    fun logInfo(msg: String) = extension.id.logInfo(msg)

    fun logWarn(msg: String) = extension.id.logWarn(msg)

    fun logError(msg: String) = extension.id.logError(msg)

    fun startScreen(screen: ExtensionScreen) {
        ExtensionActivity.start(ActivityProvider.currentActivity ?: appContext, screen)
    }
}
