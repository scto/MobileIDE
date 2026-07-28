// DO NOT UPDATE PACKAGE NAME OTHERWISE EXTENSIONS WILL BREAK
package com.scto.mobile.ide.features.extensions

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.runtime.Composable

abstract class ExtensionAPI(protected val context: ExtensionContext) : Application.ActivityLifecycleCallbacks {
    /** Called every time the extension is loaded (app start or extension installation). */
    @Deprecated("Rename to onLoad instead.", ReplaceWith("onLoad()"))
    open fun onExtensionLoaded() {
        onLoad()
    }

    /** Called every time the extension is loaded (app start or extension installation). */
    abstract fun onLoad()

    /** Called when the extension is unloaded (extension uninstallation or update). */
    open fun onDispose() {}

    /** Called only once when the extension is installed for the first time. */
    open fun onInstalled() {}

    /** Called immediately before the extension is updated to a new version (old instance). */
    open fun beforeUpdate() {}

    /** Called after the extension was updated to a new version (new instance). */
    open fun afterUpdate() {}

    /** Called when the extension is uninstalled. */
    open fun onUninstalled() {}

    @Composable open fun SettingsContent() {}

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {}

    override fun onActivityDestroyed(p0: Activity) {}

    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityResumed(p0: Activity) {}

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

    override fun onActivityStarted(p0: Activity) {}

    override fun onActivityStopped(p0: Activity) {}
}
