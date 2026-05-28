// Copyright 2025 Thomas Schmid
package com.mobile.ide.ui.projects

// Importe korrigieren/ergänzen:
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

import com.mobile.ide.core.resources.R
import com.mobile.ide.core.projects.ProjectTemplates
import com.mobile.ide.core.utils.WorkspaceManager

import java.io.File
import java.util.Locale

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ProjectType {
    NORMAL,
    WEBAPP,
    WEBSITE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(navController: NavController) {
    var projectName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("com.example.myapp") }
    var targetUrl by remember { mutableStateOf("https://") }

    var selectedType by remember { mutableStateOf(ProjectType.NORMAL) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_project_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
                    .animateContentSize(
                        animationSpec =
                            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                    )
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.new_project_select_template),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TemplateSelectionCard(
                    Modifier.weight(1f),
                    stringResource(R.string.new_project_type_web),
                    Icons.Default.Language,
                    selectedType == ProjectType.NORMAL,
                ) {
                    selectedType = ProjectType.NORMAL
                }
                TemplateSelectionCard(
                    Modifier.weight(1f),
                    stringResource(R.string.new_project_type_webapp),
                    Icons.Default.Android,
                    selectedType == ProjectType.WEBAPP,
                ) {
                    selectedType = ProjectType.WEBAPP
                }
                TemplateSelectionCard(
                    Modifier.weight(1f),
                    stringResource(R.string.new_project_type_wrapper),
                    Icons.Default.Public,
                    selectedType == ProjectType.WEBSITE,
                ) {
                    selectedType = ProjectType.WEBSITE
                }
            }

            AnimatedContent(targetState = selectedType, label = "desc") { type ->
                Text(
                    text =
                        when (type) {
                            ProjectType.NORMAL -> stringResource(R.string.new_project_desc_normal)
                            ProjectType.WEBAPP -> stringResource(R.string.new_project_desc_webapp)
                            ProjectType.WEBSITE -> stringResource(R.string.new_project_desc_wrapper)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
            }

            Text(
                stringResource(R.string.new_project_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = projectName,
                onValueChange = {
                    projectName = it
                    if (selectedType != ProjectType.NORMAL) {
                        val cleanName = it.replace(Regex("[^a-zA-Z0-9]"), "").lowercase(Locale.ROOT)
                        if (cleanName.isNotEmpty()) packageName = "com.example.$cleanName"
                    }
                },
                label = { Text(stringResource(R.string.new_project_name_label)) },
                placeholder = { Text("") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            AnimatedVisibility(visible = selectedType != ProjectType.NORMAL) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text(stringResource(R.string.new_project_package_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            AnimatedVisibility(visible = selectedType == ProjectType.WEBSITE) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = targetUrl,
                        onValueChange = { targetUrl = it },
                        label = { Text(stringResource(R.string.new_project_url_label)) },
                        placeholder = { Text(stringResource(R.string.new_project_url_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val errNoName = context.getString(R.string.new_project_err_no_name)
                    if (projectName.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(errNoName) }
                        return@Button
                    }

                    val errNoPackage = context.getString(R.string.new_project_err_no_package)
                    if (selectedType != ProjectType.NORMAL && packageName.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(errNoPackage) }
                        return@Button
                    }

                    val errNoUrl = context.getString(R.string.new_project_err_no_url)
                    if (selectedType == ProjectType.WEBSITE && targetUrl.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(errNoUrl) }
                        return@Button
                    }

                    isLoading = true
                    createNewProject(
                        context,
                        projectName,
                        packageName,
                        targetUrl,
                        selectedType,
                        onSuccess = {
                            isLoading = false
                            scope.launch {
                                val successMsg = context.getString(R.string.new_project_success)
                                val job = launch {
                                    snackbarHostState.showSnackbar(
                                        message = successMsg,
                                        duration = SnackbarDuration.Short,
                                    )
                                }
                                kotlinx.coroutines.delay(800)
                                navController.popBackStack()
                                job.cancel()
                            }
                        },
                        onError = { errorMsg ->
                            isLoading = false
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.new_project_failed, errorMsg))
                            }
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !isLoading,
                shape = MaterialTheme.shapes.medium,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.new_project_creating))
                } else {
                    Text(text = stringResource(R.string.new_project_btn_create), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TemplateSelectionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by
        animateColorAsState(
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    val containerColor by
        animateColorAsState(
            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
    ) {
        Column(Modifier.padding(vertical = 16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                null,
                tint =
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun createNewProject(
    context: Context,
    projectName: String,
    packageName: String,
    targetUrl: String,
    type: ProjectType,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
) {
    val projectDir = File(WorkspaceManager.getWorkspacePath(context), projectName)
    GlobalScope.launch(Dispatchers.IO) {
        try {
            val errExists = context.getString(R.string.new_project_err_exists)
            if (projectDir.exists()) {
                withContext(Dispatchers.Main) { onError(errExists) }
                return@launch
            }
            projectDir.mkdirs()

            when (type) {
                ProjectType.NORMAL -> createNormalStructure(projectDir)
                ProjectType.WEBAPP -> createWebAppStructure(projectDir, packageName)
                ProjectType.WEBSITE -> createWebsiteStructure(projectDir, packageName, targetUrl)
            }
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            val errUnknown = context.getString(R.string.new_project_err_unknown)
            withContext(Dispatchers.Main) { onError(e.message ?: errUnknown) }
        }
    }
}

private fun createNormalStructure(projectDir: File) {
    val css = File(projectDir, "css").apply { mkdirs() }
    val js = File(projectDir, "js").apply { mkdirs() }
    File(projectDir, "index.html").writeText(ProjectTemplates.normalIndexHtml)
    File(css, "style.css").writeText(ProjectTemplates.normalCss)
    File(js, "script.js").writeText(ProjectTemplates.normalJs)
}

private fun createWebAppStructure(projectDir: File, packageName: String) {
    val assets = File(projectDir, "src/main/assets").apply { mkdirs() }
    File(assets, "js").mkdirs()
    File(assets, "css").mkdirs()
    File(assets, "index.html").writeText(ProjectTemplates.webAppIndexHtml)
    File(assets, "js/api.js").writeText(ProjectTemplates.apiJs)
    File(assets, "js/index.js").writeText(ProjectTemplates.webAppIndexJs)
    File(assets, "css/style.css").writeText(ProjectTemplates.webAppCss)
    File(projectDir, "webapp.json")
        .writeText(ProjectTemplates.getConfigFile(packageName, projectDir.name, "index.html"))
}

private fun createWebsiteStructure(projectDir: File, packageName: String, targetUrl: String) {
    val assets = File(projectDir, "src/main/assets").apply { mkdirs() }
    File(assets, "index.html")
        .writeText(
            """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"><title>Redirecting...</title></head>
        <body>
            <p>Loading...</p>
            <script>window.location.href = "$targetUrl";</script>
        </body>
        </html>
    """
                .trimIndent()
        )

    File(projectDir, "webapp.json").writeText(ProjectTemplates.getConfigFile(packageName, projectDir.name, targetUrl))
}
