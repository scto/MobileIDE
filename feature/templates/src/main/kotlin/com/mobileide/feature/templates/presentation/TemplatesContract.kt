package com.mobileide.templates.presentation

import com.mobileide.templates.model.Template

enum class ProjectLanguage { KOTLIN, JAVA }

data class ProjectConfiguration(
    val projectName: String = "My Application",
    val packageName: String = "com.example.myapplication",
    val saveLocation: String = "/storage/emulated/0/MobileIDEProjects",
    val versionCode: String = "1",
    val versionName: String = "1.0",
    val minSdk: String = "26",
    val targetSdk: String = "34",
    val language: ProjectLanguage = ProjectLanguage.KOTLIN
)

data class TemplatesState(
    val templates: List<Template> = emptyList(),
    val configuration: ProjectConfiguration = ProjectConfiguration(),
    val showConfigurationDialogForTemplate: Template? = null,
    val isGenerating: Boolean = false,
    val generationResult: String? = null // Success or error message
)

sealed interface TemplatesEvent {
    data class ConfigureTemplate(val templateId: String) : TemplatesEvent
    object DismissConfigurationDialog : TemplatesEvent
    object GenerateProjectClicked : TemplatesEvent
    object ResultMessageDismissed : TemplatesEvent

    // Events for the configuration dialog
    data class ProjectNameChanged(val name: String) : TemplatesEvent
    data class PackageNameChanged(val name: String) : TemplatesEvent
    data class VersionCodeChanged(val code: String) : TemplatesEvent
    data class VersionNameChanged(val name: String) : TemplatesEvent
    data class MinSdkChanged(val sdk: String) : TemplatesEvent
    data class TargetSdkChanged(val sdk: String) : TemplatesEvent
    data class LanguageChanged(val language: ProjectLanguage) : TemplatesEvent
}

