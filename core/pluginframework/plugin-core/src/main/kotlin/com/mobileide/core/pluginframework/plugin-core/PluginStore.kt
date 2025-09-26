package com.mobileide.core.pluginframework.plugin-core

import kotlinx.coroutines.*
import org.pf4j.update.*
import org.slf4j.LoggerFactory

data class AvailablePlugin(val id: String, val description: String, val latestVersion: String)

class PluginStore(repositories: List<UpdateRepository>) {
    private val updateManager = UpdateManager(null, repositories) // Manager ohne PluginManager für Store-Abfragen
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun findAvailablePlugins(): List<AvailablePlugin> = withContext(Dispatchers.IO) {
        updateManager.refresh()
        updateManager.availablePlugins.map {
            AvailablePlugin(it.id, it.description, it.releases.first().version)
        }
    }

    suspend fun installPlugin(pluginId: String, version: String): Boolean = withContext(Dispatchers.IO) {
        updateManager.installPlugin(pluginId, version)
    }
}
