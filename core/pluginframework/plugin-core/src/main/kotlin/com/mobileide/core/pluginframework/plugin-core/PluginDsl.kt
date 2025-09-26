package com.mobileide.core.pluginframework.plugin-core

import org.pf4j.*
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

class PluginManagerBuilder {
    var pluginDirectory: Path = Paths.get("plugins")
    val repositories = mutableListOf<URL>()
    private val listeners = mutableListOf<PluginStateListener>()

    fun repositories(block: RepositoryBuilder.() -> Unit) {
        repositories.addAll(RepositoryBuilder().apply(block).urls)
    }

    fun onStateChange(block: (PluginStateEvent) -> Unit) {
        listeners.add(PluginStateListener(block))
    }

    fun build(): PluginManager {
        val manager = DefaultPluginManager(pluginDirectory)
        listeners.forEach(manager::addPluginStateListener)
        return manager
    }
}

class RepositoryBuilder {
    val urls = mutableListOf<URL>()
    fun url(urlString: String) {
        urls.add(URL(urlString))
    }
}

fun buildPluginManager(config: PluginManagerBuilder.() -> Unit): PluginManager {
    return PluginManagerBuilder().apply(config).build()
}
