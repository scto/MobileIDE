package com.mobileide.settings.presentation

/**
 * Definiert das Datenmodell für die Einstellungen.
 */
data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM
)

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

/**
 * Der Zustand des Einstellungs-Bildschirms.
 */
data class SettingsState(
    val settings: AppSettings = AppSettings()
)

/**
 * Aktionen, die vom Einstellungs-Bildschirm ausgelöst werden können.
 */
sealed interface SettingsEvent {
    data class OnThemeChanged(val theme: AppTheme) : SettingsEvent
}
