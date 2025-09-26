package com.mobileide.core.pluginframework.plugin-core

import kotlinx.coroutines.flow.*

/**
 * Ein einfacher, anwendungsinterner Event-Bus auf Basis von Kotlin Flows.
 */
object EventBus {
    private val _events = MutableSharedFlow<Any>()
    val events = _events.asSharedFlow()

    suspend fun publish(event: Any) {
        _events.emit(event)
    }

    suspend inline fun <reified T : Any> subscribe(crossinline onEvent: (T) -> Unit) {
        events.filter { it is T }.collect { onEvent(it as T) }
    }
}
