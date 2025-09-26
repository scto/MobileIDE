package com.mobileide.core.pluginframework.sample-host-app

import com.mobileide.core.pluginframework.plugin-api.*
import com.mobileide.core.pluginframework.plugin-core.*
import kotlinx.coroutines.*
import org.pf4j.update.DefaultUpdateRepository
import java.net.URL

fun main() = runBlocking {
    println("### Starte Host-Anwendung ###")

    // Konfiguration
    val repoUrl = "http://localhost:8080/" // Port 8080 ist üblicher für lokale Server
    val repositories = listOf(DefaultUpdateRepository("main-store", URL(repoUrl)))

    // PluginManager mit DSL erstellen
    val pluginManager = buildPluginManager {
        // Hier könnten weitere Konfigurationen vorgenommen werden
        // z.B. onStateChange { ... }
    }
    
    // Services instanziieren
    val pluginService = PluginService(pluginManager)
    val updater = PluginUpdater(pluginManager, repositories)
    val store = PluginStore(repositories)

    // Plugins initialisieren
    pluginService.initializePluginsAsync().await()
    delay(500) // kurz warten

    println("\n--- Suche nach Updates ---")
    val updates = updater.checkForUpdates()
    if (updates.isNotEmpty()) {
        println("${updates.size} Update(s) gefunden!")
    }
    
    println("\n--- Suche im Store nach neuen Plugins ---")
    val availablePlugins = store.findAvailablePlugins()
    if (availablePlugins.isNotEmpty()) {
        println("${availablePlugins.size} neue(s) Plugin(s) im Store gefunden:")
        availablePlugins.forEach { println("  - ${it.id} (${it.latestVersion})") }
    } else {
        println("Keine neuen Plugins im Store gefunden.")
    }

    println("\n--- Führe Aktionen aus allen Plugins aus ---")
    val actions = pluginService.getExtensions(Action::class.java)
    if(actions.isEmpty()){
        println("Keine Aktionen gefunden.")
    } else {
       actions.forEach { it.execute() }
    }
    
    println("\n--- Sende Events an Plugins ---")
    EventBus.publish(AppEvent.DocumentOpened("MeinDokument.txt"))
    delay(100)
    EventBus.publish(AppEvent.UserLoggedIn("Benutzer-123"))
    delay(1000) // Warten, damit Plugins die Events verarbeiten können

    // System herunterfahren
    pluginService.shutdown()
    println("### Host-Anwendung beendet ###")
}
