package com.scto.mobile.ide.events

import com.scto.mobile.ide.core.common.files.FileObject
import kotlin.reflect.KClass

interface Event

sealed interface FileTreeEvent : Event {
    data class Opened(val projectRoot: FileObject) : FileTreeEvent
    data class TreeSynchronized(val parent: FileObject) : FileTreeEvent
}

sealed interface EditorTabEvent : Event {
    data class Saved(val tab: Any?, val file: FileObject, val quickSave: Boolean) : EditorTabEvent
}

interface EventSubscription {
    fun unsubscribe()
}

object Events {
    @PublishedApi internal val listeners = mutableMapOf<KClass<out Event>, MutableList<suspend (Event) -> Unit>>()

    inline fun <reified T : Event> subscribe(noinline listener: suspend (T) -> Unit): EventSubscription {
        val list = listeners.getOrPut(T::class) { mutableListOf() }
        val wrapper: suspend (Event) -> Unit = { listener(it as T) }
        list += wrapper
        return object : EventSubscription {
            override fun unsubscribe() {
                list -= wrapper
            }
        }
    }

    suspend fun publish(event: Event) {
        listeners.filterKeys { it.isInstance(event) }.values.flatten().forEach { listener ->
            try {
                listener(event)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}
