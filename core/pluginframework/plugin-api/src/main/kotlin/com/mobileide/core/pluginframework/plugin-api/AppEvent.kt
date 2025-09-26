package com.mobileide.core.pluginframework.plugin-api

/**
 * Definiert Anwendungs-Events, auf die Plugins reagieren können.
 */
sealed interface AppEvent {
    data class DocumentOpened(val documentId: String) : AppEvent
    data class UserLoggedIn(val userId: String) : AppEvent
}
