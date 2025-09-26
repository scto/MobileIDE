package com.yourcompany.yourplugin

import com.mobileide.core.pluginframework.plugin-api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.pf4j.*
import org.slf4j.LoggerFactory

class YourPlugin(wrapper: PluginWrapper) : Plugin(wrapper) {
    private val logger = LoggerFactory.getLogger(javaClass)
    override fun start() {
        logger.info("YourPlugin started!")
    }
    override fun stop() {
        logger.info("YourPlugin stopped!")
    }
}

@Extension
class MyAction : Action {
    override val name = "Meine Aktion"
    override fun execute() {
        println("Meine Aktion wurde ausgeführt!")
    }
}

@Extension
class MyEventListener : EventListener {
    private var job: Job? = null
    override fun onStart(scope: CoroutineScope) {
        // Starte hier das Lauschen auf Events
    }
    override fun onStop() {
        // Räume hier auf, z.B. Coroutinen beenden
        job?.cancel()
    }
}
