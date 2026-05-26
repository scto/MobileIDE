#!/usr/bin/env python3
import os
from pathlib import Path

def generate_wizard_screen():
    # Paket und Pfad gemäß deiner Anforderung
    package = "com.scto.mcside.ui.projects.wizard"
    file_path = Path("app/src/main/java") / package.replace(".", "/") / "TemplateWizardScreen.kt"
    file_path.parent.mkdir(parents=True, exist_ok=True)

    # 🔧 FIX: Wir nutzen einen normalen String mit einem Platzhalter,
    # um die f-string-Syntax-Probleme mit Kotlin-Blöcken { } zu umgehen.
    content = """package __KOTLIN_PACKAGE__

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scto.mcside.core.template.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ==================== VIEWMODEL ====================
@HiltViewModel
class TemplateWizardViewModel @Inject constructor(
    private val templateManager: TemplateManager,
    private val queryService: TemplateQueryService
) : ViewModel() {
    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    init { loadTemplates() }

    private fun loadTemplates() {
        viewModelScope.launch {
            try {
                queryService.getAllMetadata().collect { templates ->
                    _uiState.update { it.copy(templates = templates) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Templates konnten nicht geladen werden.") }
            }
        }
    }

    fun selectTemplate(template: TemplateMetadata) {
        _uiState.update { it.copy(selectedTemplate = template, currentStep = 1) }
    }

    fun updateConfig(key: String, value: String) {
        _uiState.update { it.copy(configUpdates = it.configUpdates + (key to value)) }
    }

    fun startGeneration() {
        val state = _uiState.value
        if (state.selectedTemplate == null) return

        _uiState.update { it.copy(currentStep = 2, isGenerating = true, error = null) }
        viewModelScope.launch {
            try {
                val config = ProjectCreationConfig(
                    appName = state.configUpdates["appName"]?.takeIf { it.isNotBlank() } ?: "McsIDEProject",
                    packageName = state.configUpdates["packageName"]?.takeIf { it.isNotBlank() } ?: "com.scto.myapp",
                    minSdk = state.configUpdates["minSdk"]?.toIntOrNull() ?: 26,
                    targetSdk = state.configUpdates["targetSdk"]?.toIntOrNull() ?: 35,
                    language = Language.KOTLIN,
                    useKotlinDsl = true
                )
                templateManager.installTemplates(config)
                _uiState.update { it.copy(isGenerating = false, currentStep = 3) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, error = e.message, currentStep = 2) }
            }
        }
    }

    fun reset() = _uiState.update { WizardUiState() }
    fun goBack() = _uiState.update { if (it.currentStep > 0) it.copy(currentStep = it.currentStep - 1) else it }}

// ==================== STATE ====================
data class WizardUiState(
    val currentStep: Int = 0,
    val templates: List<TemplateMetadata> = emptyList(),
    val selectedTemplate: TemplateMetadata? = null,
    val configUpdates: Map<String, String> = mapOf(
        "appName" to "", "packageName" to "", "minSdk" to "26", "targetSdk" to "35"
    ),
    val isGenerating: Boolean = false,
    val error: String? = null
)

// ==================== MAIN SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateWizardScreen(
    viewModel: TemplateWizardViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onProjectCreated: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neues Projekt") },
                navigationIcon = {
                    IconButton(onClick = { if (state.currentStep > 0) viewModel.goBack() else onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "wizard_step_transition"
            ) { step ->
                when (step) {
                    0 -> TemplateSelectionScreen(state, viewModel::selectTemplate)
                    1 -> ConfigurationScreen(state, viewModel::updateConfig, viewModel::startGeneration)
                    2 -> GenerationScreen(state, viewModel::reset)
                    3 -> SuccessScreen(viewModel::reset, onProjectCreated)
                }
            }
        }    }
}

// ==================== STEP 0: TEMPLATE SELECTION ====================
@Composable
private fun TemplateSelectionScreen(
    state: WizardUiState,
    onSelect: (TemplateMetadata) -> Unit
) {
    if (state.templates.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Templates werden geladen...")
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(state.templates) { template ->
            Card(
                modifier = Modifier.fillMaxWidth().aspectRatio(3f/4f),
                onClick = { onSelect(template) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(template.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(template.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
        }
    }
}

// ==================== STEP 1: CONFIGURATION ====================
@Composable
private fun ConfigurationScreen(
    state: WizardUiState,
    onConfig: (String, String) -> Unit,    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Projekt konfigurieren", style = MaterialTheme.typography.headlineMedium)
        if (state.selectedTemplate != null) {
            Text("Vorlage: ${state.selectedTemplate.name}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }

        OutlinedTextField(
            value = state.configUpdates["appName"] ?: "",
            onValueChange = { onConfig("appName", it) },
            label = { Text("App Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.configUpdates["packageName"] ?: "",
            onValueChange = { onConfig("packageName", it) },
            label = { Text("Package Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.configUpdates["minSdk"] ?: "26",
                onValueChange = { onConfig("minSdk", it) },
                label = { Text("Min SDK") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = state.configUpdates["targetSdk"] ?: "35",
                onValueChange = { onConfig("targetSdk", it) },
                label = { Text("Target SDK") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGenerate,
            enabled = (state.configUpdates["appName"]?.isNotBlank() == true) &&
                      (state.configUpdates["packageName"]?.isNotBlank() == true),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Projekt generieren")        }
    }
}

// ==================== STEP 2: GENERATION PROGRESS ====================
@Composable
private fun GenerationScreen(state: WizardUiState, onCancel: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.isGenerating) CircularProgressIndicator() else LinearProgressIndicator()
        Spacer(Modifier.height(24.dp))
        Text("Projekt wird erstellt...", style = MaterialTheme.typography.titleMedium)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCancel) { Text("Zurück") }
        }
    }
}

// ==================== STEP 3: SUCCESS ====================
@Composable
private fun SuccessScreen(onNew: () -> Unit, onNavigate: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Erfolg!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onNew) { Text("Neues Projekt") }
            Button(onClick = onNavigate) { Text("Öffnen") }
        }
    }
}
"""

    # Ersetze den Platzhalter sicher durch das Paket
    content = content.replace("__KOTLIN_PACKAGE__", package)
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"✅ WizardScreen erfolgreich erstellt: {file_path}")

if __name__ == "__main__":
    generate_wizard_screen()
