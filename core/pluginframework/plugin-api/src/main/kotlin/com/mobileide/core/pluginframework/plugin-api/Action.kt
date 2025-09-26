package com.mobileide.core.pluginframework.plugin-api

import org.pf4j.ExtensionPoint

/**
 * Eine einfache ausführbare Aktion, die von Plugins bereitgestellt werden kann.
 */
interface Action : ExtensionPoint {
    val name: String
    fun execute()
}
