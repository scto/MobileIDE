package com.mobileide.core.pluginframework.sample-plugin

import com.mobileide.core.pluginframework.plugin-api.*
import com.mobileide.core.pluginframework.plugin&core.EventBus
import kotlinx.coroutines.*
import org.pf4j.*
import org.slf4j.LoggerFactory

class SamplePlugin(wrapper: PluginWrapper) : Plugin(wrapper) {
    private val logger = LoggerFactory.getLogger(javaClass)
    override fun start() {
        logger.info("SamplePlugin.start()")
    }
    override fun stop() {
        logger.info("SamplePlugin.stop()")
    }
}

@Extension
class MyAction : Action {
    override val name = "Hello World Action"
    override fun execute() {
        println("ACTION: Hallo aus dem SamplePlugin!")
    }
}

@Extension
class MyEventListener : EventListener {
    private var job: Job? = null
    override fun onStart(scope: CoroutineScope) {
        job = scope.launch {
            EventBus.subscribe<AppEvent.DocumentOpened> {
                println("PLUGIN: Dokument '${it.documentId}' wurde geöffnet!")
            }
        }
    }
    override fun onStop() {
        job?.cancel()
    }
}
