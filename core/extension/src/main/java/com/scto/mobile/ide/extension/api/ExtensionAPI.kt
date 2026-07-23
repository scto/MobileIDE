


//DO NOT UPDATE PACKAGE NAME OTHERWISE EXTENSIONS WILL BREAK
package com.scto.mobile.ide.extension

import android.app.Application
import androidx.compose.runtime.Composable

abstract class ExtensionAPI(protected val context: ExtensionContext) : Application.ActivityLifecycleCallbacks {
    /** Called only once when the extension is installed for the first time. */
    abstract fun onInstalled()

    /** Called every time the extension is loaded (app start or extension installation). */
    abstract fun onExtensionLoaded()

    /** Called when the extension is updated to a new version. */
    abstract fun onUpdated()

    /** Called when the extension is uninstalled. */
    abstract fun onUninstalled()

    @Composable open fun SettingsContent() {}

    override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
    override fun onActivityStarted(activity: android.app.Activity) {}
    override fun onActivityResumed(activity: android.app.Activity) {}
    override fun onActivityPaused(activity: android.app.Activity) {}
    override fun onActivityStopped(activity: android.app.Activity) {}
    override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
    override fun onActivityDestroyed(activity: android.app.Activity) {}
}
