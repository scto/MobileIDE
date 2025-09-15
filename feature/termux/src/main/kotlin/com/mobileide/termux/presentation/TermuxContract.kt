package com.mobileide.termux.presentation

import com.mobileide.data.TermuxPackage

/**
 * Represents the state of the Termux screen.
 * @param packages The list of installed Termux packages.
 * @param isLoading True if data is currently being loaded or a process is running.
 * @param error A description of an error that has occurred.
 * @param userMessage A message to be shown to the user (e.g., in a Snackbar).
 * @param showInstallDialog True if the dialog to install a new package should be shown.
 */
data class TermuxState(
    val packages: List<TermuxPackage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val userMessage: String? = null,
    val showInstallDialog: Boolean = false
)

/**
 * Defines the events that can be triggered from the Termux UI.
 */
sealed interface TermuxEvent {
    object LoadPackages : TermuxEvent
    data class InstallPackage(val packageName: String) : TermuxEvent
    data class UninstallPackage(val pkg: TermuxPackage) : TermuxEvent
    object ShowInstallDialog : TermuxEvent
    object DismissInstallDialog : TermuxEvent
    object UserMessageShown : TermuxEvent
}
