package com.mobileide.templates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileide.data.FileRepository
import com.mobileide.templates.data.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TemplatesState())
    val state = _state.asStateFlow()

    init {
        _state.update { it.copy(templates = templateRepository.getTemplates()) }
    }

    fun onEvent(event: TemplatesEvent) {
        when (event) {
            is TemplatesEvent.TemplateSelected -> _state.update { it.copy(selectedTemplateId = event.templateId) }
            is TemplatesEvent.ProjectNameChanged -> _state.update { it.copy(projectName = event.name, packageName = "com.example.${event.name.lowercase().replace(" ", "")}") }
            is TemplatesEvent.PackageNameChanged -> _state.update { it.copy(packageName = event.name) }
            is TemplatesEvent.GenerateProjectClicked -> generateProject()
            is TemplatesEvent.ResultMessageDismissed -> _state.update { it.copy(generationResult = null) }
        }
    }

    private fun generateProject() {
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, generationResult = null) }

            val currentState = _state.value
            val template = currentState.templates.find { it.id == currentState.selectedTemplateId }
            if (template == null) {
                _state.update { it.copy(isGenerating = false, generationResult = "Error: No template selected.") }
                return@launch
            }

            try {
                template.files.forEach { templateFile ->
                    val finalContent = templateFile.content
                        .replace("${'$'}{packageName}", currentState.packageName)
                        .replace("${'$'}{projectName}", currentState.projectName)
                    
                    val packagePath = currentState.packageName.replace('.', '/')
                    val finalPath = templateFile.path
                        .replace("${'$'}{packagePath}", packagePath)

                    val fullPath = "${currentState.saveLocation}/${currentState.projectName}/$finalPath"

                    // Ensure parent directories exist
                    File(fullPath).parentFile?.mkdirs()

                    fileRepository.saveFile(fullPath, finalContent)
                        .onFailure { throw it }
                }
                _state.update { it.copy(isGenerating = false, generationResult = "Project '${currentState.projectName}' generated successfully!") }
            } catch (e: Exception) {
                 _state.update { it.copy(isGenerating = false, generationResult = "Error: ${e.message}") }
            }
        }
    }
}
