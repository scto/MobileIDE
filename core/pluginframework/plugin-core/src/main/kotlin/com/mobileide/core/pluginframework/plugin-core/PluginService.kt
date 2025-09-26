package com.mobileide.core.pluginframework.plugin-core

import kotlinx.coroutines.*
import org.pf4j.PluginManager
import org.slf4j.LoggerFactory

class PluginService(val pluginManager: PluginManager) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initializePluginsAsync(): Deferred<Boolean> = scope.async {
        try {
            logger.info("Lade Plugins im Hintergrund...")
            pluginManager.loadPlugins()
            pluginManager.startPlugins()
            logger.info("Alle Plugins erfolgreich gestartet.")
            true
        } catch (e: Exception) {
            logger.error("Fehler beim Initialisieren der Plugins!", e)
            false
        }
    }

    fun <T> getExtensions(extensionType: Class<T>): List<T> {
        return pluginManager.getExtensions(extensionType)
    }

    fun shutdown() {
        logger.info("Fahre Plugin-System herunter...")
        pluginManager.stopPlugins()
        scope.cancel()
    }
}
