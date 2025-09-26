package com.mobileide.core.pluginframework.plugin-api

import kotlinx.coroutines.CoroutineScope
import org.pf4j.ExtensionPoint

/**
 * Ein Extension Point für Plugins, die auf den App-Lebenszyklus reagieren
 * und auf Events lauschen wollen.
 */
interface EventListener : ExtensionPoint {
    fun onStart(scope: CoroutineScope)
    fun onStop()
}
