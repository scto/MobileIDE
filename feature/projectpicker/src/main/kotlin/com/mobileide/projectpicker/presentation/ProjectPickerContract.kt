package com.mobileide.projectpicker.presentation

/**
 * Repräsentiert einen kürzlich geöffneten Projektpfad.
 */
data class RecentProject(
    val name: String,
    val path: String
)

/**
 * Der Zustand des Projekt-Auswahl-Bildschirms.
 */
data class ProjectPickerState(
    val recentProjects: List<RecentProject> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Aktionen, die vom Bildschirm ausgelöst werden können.
 */
sealed interface ProjectPickerEvent {
    data class OpenProject(val path: String) : ProjectPickerEvent
    object OpenProjectViaPicker : ProjectPickerEvent
    object CreateNewProject : ProjectPickerEvent
}
