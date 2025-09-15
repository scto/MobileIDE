package com.mobileide.templates.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(viewModel: TemplatesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.generationResult) {
        state.generationResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(TemplatesEvent.ResultMessageDismissed)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("New Project From Template", style = MaterialTheme.typography.headlineMedium)
            }

            item {
                OutlinedTextField(
                    value = state.projectName,
                    onValueChange = { viewModel.onEvent(TemplatesEvent.ProjectNameChanged(it)) },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = state.packageName,
                    onValueChange = { viewModel.onEvent(TemplatesEvent.PackageNameChanged(it)) },
                    label = { Text("Package Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Text("Select a Template", style = MaterialTheme.typography.titleLarge)
            }
            
            val groupedTemplates = state.templates.groupBy { it.category }
            groupedTemplates.forEach { (category, templates) ->
                item {
                    Text(category, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                }
                items(templates) { template ->
                    TemplateItem(
                        template = template,
                        isSelected = state.selectedTemplateId == template.id,
                        onClick = { viewModel.onEvent(TemplatesEvent.TemplateSelected(template.id)) }
                    )
                }
            }

            item {
                Button(
                    onClick = { viewModel.onEvent(TemplatesEvent.GenerateProjectClicked) },
                    enabled = state.selectedTemplateId != null && !state.isGenerating,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    if (state.isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Generate Project")
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateItem(template: com.mobileide.templates.model.Template, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(template.name, style = MaterialTheme.typography.titleMedium)
            Text(template.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
