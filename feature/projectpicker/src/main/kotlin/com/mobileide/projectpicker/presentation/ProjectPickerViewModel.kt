package com.mobileide.projectpicker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileide.data.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectPickerViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectPickerState())
    val state = _state.asStateFlow()

    init {
        projectRepository.getRecentProjects()
            .onEach { paths ->
                val projects = paths.map { RecentProject(name = it.substringAfterLast('/'), path = it) }
                _state.update { it.copy(recentProjects = projects, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ProjectPickerEvent) {
        when (event) {
            is ProjectPickerEvent.OpenProject -> {
                viewModelScope.launch {
                    projectRepository.setCurrentProject(event.path)
                }
            }
            // Die Navigation für die anderen Events wird in der UI behandelt.
            else -> {}
        }
    }
}
