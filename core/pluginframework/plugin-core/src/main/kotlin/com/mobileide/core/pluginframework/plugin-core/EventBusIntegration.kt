package com.mobileide.core.pluginframework.plugin-core

import com.mobileide.core.pluginframework.plugin-api.EventListener
import kotlinx.coroutines.*
import org.pf4j.*

fun PluginManagerBuilder.integrateWithEventBus(pluginManager: PluginManager) {
    onStateChange { event ->
        val pluginId = event.plugin.pluginId
        when (event.pluginState) {
            PluginState.STARTED -> {
                pluginManager.getExtensions(EventListener::class.java, pluginId).forEach {
                    it.onStart(CoroutineScope(Dispatchers.Default))
                }
            }
            PluginState.STOPPED -> {
                pluginManager.getExtensions(EventListener::class.java, pluginId).forEach {
                    it.onStop()
                }
            }
            else -> {
                // Nichts zu tun für andere Zustände
            }
        }
    }
}
