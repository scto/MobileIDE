package com.mobileide.core.pluginframework.another-plugin

import com.mobileide.core.pluginframework.plugin-api.*
import com.mobileide.core.pluginframework.plugin-core.EventBus
import kotlinx.coroutines.*
import org.pf4j.*
import org.slf4j.LoggerFactory

class AnotherPlugin(wrapper: PluginWrapper) : Plugin(wrapper) {
    private val logger = LoggerFactory.getLogger(javaClass)
    override fun start() {
        logger.info("AnotherPlugin.start()")
    }
    override fun stop() {
        logger.info("AnotherPlugin.stop()")
    }
}

@Extension
class AnotherAction : Action {
    override val name = "Another Fancy Action"
    override fun execute() {
        println("ACTION: Eine weitere Aktion aus dem AnotherPlugin!")
    }
}

@Extension
class AnotherEventListener : EventListener {
    private var job: Job? = null
    override fun onStart(scope: CoroutineScope) {
        job = scope.launch {
            EventBus.subscribe<AppEvent.UserLoggedIn> {
                println("ANOTHER PLUGIN: Benutzer '${it.userId}' hat sich angemeldet!")
            }
        }
    }
    override fun onStop() {
        job?.cancel()
    }
}
