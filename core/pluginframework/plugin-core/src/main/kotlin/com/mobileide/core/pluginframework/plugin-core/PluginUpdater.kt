package com.mobileide.core.pluginframework.plugin-core

import kotlinx.coroutines.*
import org.pf4j.*
import org.pf4j.update.*
import org.slf4j.LoggerFactory

class PluginUpdater(private val pluginManager: PluginManager, repositories: List<UpdateRepository>) {
    private val updateManager = UpdateManager(pluginManager, repositories)
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun checkForUpdates(): List<UpdateManager.UpdateInfo> = withContext(Dispatchers.IO) {
        if (updateManager.hasUpdates()) {
            val updates = updateManager.updates
            logger.info("${updates.size} Update(s) gefunden.")
            return@withContext updates
        }
        logger.info("Alle Plugins sind auf dem neuesten Stand.")
        emptyList()
    }

    suspend fun installUpdates(): List<String> = withContext(Dispatchers.IO) {
        val updatedPlugins = mutableListOf<String>()
        val updates = updateManager.updates
        if (updates.isEmpty()) return@withContext emptyList()

        for (update in updates) {
            val lastVersion = updateManager.getLastRelease(update.pluginId)
            if (updateManager.installPlugin(update.pluginId, lastVersion.version)) {
                updatedPlugins.add(update.pluginId)
            }
        }
        updatedPlugins
    }
}
