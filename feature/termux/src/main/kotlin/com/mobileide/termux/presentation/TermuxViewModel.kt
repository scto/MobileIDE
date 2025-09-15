package com.mobileide.termux.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileide.data.TermuxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TermuxViewModel @Inject constructor(
    private val termuxRepository: TermuxRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TermuxState())
    val state = _state.asStateFlow()

    init {
        onEvent(TermuxEvent.LoadPackages)
    }

    fun onEvent(event: TermuxEvent) {
        when (event) {
            TermuxEvent.LoadPackages -> loadPackages()
            is TermuxEvent.InstallPackage -> install(event.packageName)
            is TermuxEvent.UninstallPackage -> uninstall(event.pkg)
            TermuxEvent.ShowInstallDialog -> _state.update { it.copy(showInstallDialog = true) }
            TermuxEvent.DismissInstallDialog -> _state.update { it.copy(showInstallDialog = false) }
            TermuxEvent.UserMessageShown -> _state.update { it.copy(userMessage = null) }
        }
    }

    private fun loadPackages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            termuxRepository.listInstalledPackages()
                .onSuccess { packages ->
                    _state.update { it.copy(isLoading = false, packages = packages) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
    
    private fun install(packageName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, userMessage = "Installing ${packageName}...") }
            termuxRepository.installPackage(packageName)
                .onSuccess {
                    _state.update { it.copy(userMessage = "${packageName} installed successfully.") }
                    loadPackages() // Refresh list
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
    
    private fun uninstall(pkg: com.mobileide.data.TermuxPackage) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, userMessage = "Uninstalling ${pkg.name}...") }
            termuxRepository.uninstallPackage(pkg.name)
                .onSuccess {
                    _state.update { it.copy(userMessage = "${pkg.name} uninstalled successfully.") }
                    loadPackages() // Refresh list
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
