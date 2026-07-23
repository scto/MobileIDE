package com.scto.mobile.ide.extension.manager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.scto.mobile.ide.extension.ExtensionAPI
import com.scto.mobile.ide.extension.extensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object ExtensionAPIManager : Application.ActivityLifecycleCallbacks {
    private val scope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    private fun runForAll(block: ExtensionAPI.() -> Unit) {
        extensionManager.loadedExtensions.values.forEach { loaded -> loaded?.api?.block() }
    }

    private fun runForAllAsync(block: ExtensionAPI.() -> Unit) {
        scope.launch { runForAll(block) }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        runForAllAsync { onActivityCreated(activity, savedInstanceState) }
    }

    override fun onActivityStarted(activity: Activity) {
        runForAllAsync { onActivityStarted(activity) }
    }

    override fun onActivityResumed(activity: Activity) {
        runForAllAsync { onActivityResumed(activity) }
    }

    override fun onActivityPaused(activity: Activity) {
        runForAllAsync { onActivityPaused(activity) }
    }

    override fun onActivityStopped(activity: Activity) {
        runForAllAsync { onActivityStopped(activity) }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        runForAllAsync { onActivitySaveInstanceState(activity, outState) }
    }

    override fun onActivityDestroyed(activity: Activity) {
        runForAllAsync { onActivityDestroyed(activity) }
    }
}
