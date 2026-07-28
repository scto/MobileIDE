package com.scto.mobile.ide.features.git

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.scto.mobile.ide.activities.main.MainActivity
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.components.DialogProvider
import com.scto.mobile.ide.components.DialogRegistry
import com.scto.mobile.ide.drawer.AddProjectCategory
import com.scto.mobile.ide.drawer.AddProjectOption
import com.scto.mobile.ide.drawer.AddProjectRegistry
import com.scto.mobile.ide.drawer.ServiceTabProvider
import com.scto.mobile.ide.drawer.ServiceTabRegistry
import com.scto.mobile.ide.events.EditorTabEvent
import com.scto.mobile.ide.events.EventSubscription
import com.scto.mobile.ide.events.Events
import com.scto.mobile.ide.events.FileTreeEvent
import com.scto.mobile.ide.features.extensions.api.DynamicRoute
import com.scto.mobile.ide.feature.Feature
import com.scto.mobile.ide.feature.FeatureRegistry
import com.scto.mobile.ide.feature.FeatureToggle
import com.scto.mobile.ide.file.FileDecoration
import com.scto.mobile.ide.file.FileDecorationProvider
import com.scto.mobile.ide.file.FileDecorationRegistry
import com.scto.mobile.ide.file.FileObject
import com.scto.mobile.ide.file.FilePropertiesProvider
import com.scto.mobile.ide.file.FilePropertiesRegistry
import com.scto.mobile.ide.file.FileProperty
import com.scto.mobile.ide.features.git.template.ExtensionTemplate
import com.scto.mobile.ide.features.git.template.IconPackTemplate
import com.scto.mobile.ide.features.git.template.ThemeTemplate
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.project.ProjectCategory
import com.scto.mobile.ide.project.ProjectTemplateRegistry
import com.scto.mobile.ide.resources.drawables
import com.scto.mobile.ide.resources.getString
import com.scto.mobile.ide.resources.strings
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.settings.SettingsCategory
import com.scto.mobile.ide.settings.SettingsRegistry
import com.scto.mobile.ide.settings.git.GitSettings
import com.scto.mobile.ide.theme.gitAdded
import com.scto.mobile.ide.theme.gitConflicted
import com.scto.mobile.ide.theme.gitDeleted
import com.scto.mobile.ide.theme.gitModified
import java.lang.ref.WeakReference

// Global reference for gitViewModel
var gitViewModel = WeakReference<GitViewModel?>(null)

class GitFeature : Feature {
    override val toggle =
        FeatureToggle(
            nameRes = strings.git,
            key = "enable_git",
            default = true,
            iconRes = drawables.git,
        )

    private var settingsCategory: SettingsCategory? = null
    private var settingsRoute: DynamicRoute? = null
    private var serviceTabProvider: ServiceTabProvider? = null
    private var addProjectOption: AddProjectOption? = null
    private var dialogProvider: DialogProvider? = null
    private var projectCategory: ProjectCategory? = null
    private val subscriptions = mutableListOf<EventSubscription>()

    override fun init(application: Application) {
        // Register Git settings category
        settingsCategory =
            SettingsCategory(
                    labelRes = strings.git,
                    descriptionRes = strings.git_desc,
                    iconRes = drawables.git,
                    route = SettingsRoutes.Git.route,
                )
                .also { SettingsRegistry.registerCategory(it) }

        // Register Git settings route
        settingsRoute =
            DynamicRoute(SettingsRoutes.Git.route) { _, _ -> GitSettings() }
                .also {
                    SettingsRegistry.registerRoute(it)
                }

        FileDecorationRegistry.register(GitFileDecorationProvider)
        FilePropertiesRegistry.register(GitProperty)

        serviceTabProvider =
            ServiceTabProvider { owner ->
                val viewModel = ViewModelProvider(owner)[GitViewModel::class.java]
                gitViewModel = WeakReference(viewModel)
                GitTab(viewModel)
            }
                .also { ServiceTabRegistry.register(it) }

        // Register file change notification listeners
        subscriptions.add(
            Events.subscribe<FileTreeEvent.Opened> { event ->
                val gitRoot = findGitRoot(event.projectRoot.getAbsolutePath())
                if (gitRoot != null) {
                    gitViewModel.get()?.loadRepository(gitRoot)
                }
            }
        )

        subscriptions.add(
            Events.subscribe<FileTreeEvent.TreeSynchronized> { event ->
                gitViewModel.get()?.syncChanges(event.parent.getAbsolutePath())
            }
        )

        subscriptions.add(
            Events.subscribe<EditorTabEvent.Saved> { event ->
                gitViewModel.get()?.syncChanges(event.file.getAbsolutePath())
            }
        )

        // Register Git Clone Overlay and Add Project Sheet action
        var showCloneDialog by mutableStateOf(false)
        if (FeatureRegistry.isEnabled("enable_git")) {
            addProjectOption =
                AddProjectOption(
                        icon = Icon.ResourceIcon(drawables.git),
                        title = strings.clone_repo.getString(),
                        description = strings.clone_repo_desc.getString(),
                        category = AddProjectCategory.CREATE,
                        onClick = { onDismiss ->
                            showCloneDialog = true
                            onDismiss()
                        },
                    )
                    .also { AddProjectRegistry.register(it) }
        }

        dialogProvider =
            DialogProvider {
                if (showCloneDialog) {
                    GitCloneDialog(
                        onDismiss = { showCloneDialog = false },
                        onCloneComplete = { destination ->
                            // Add file tree tab on success
                            MainActivity.instance?.drawerViewModel?.addFileTreeTab(destination)
                        },
                    )
                }
            }
                .also { DialogRegistry.register(it) }

        // Register MobileIDE project templates
        projectCategory =
            ProjectCategory(
                    id = "mobileide_editor",
                    label = strings.app_name.getString(),
                    icon = Icon.ResourceIcon(drawables.mobileide_editor),
                )
                .also {
                    ProjectTemplateRegistry.registerCategory(it)
                    val templates = listOf(ExtensionTemplate, ThemeTemplate, IconPackTemplate)
                    templates.forEach { template ->
                        ProjectTemplateRegistry.registerTemplate(it, template)
                    }
                }
    }

    override fun dispose(application: Application) {
        settingsCategory?.let { SettingsRegistry.unregisterCategory(it) }
        settingsRoute?.let { SettingsRegistry.unregisterRoute(it) }
        FileDecorationRegistry.unregister(GitFileDecorationProvider)
        FilePropertiesRegistry.unregister(GitProperty)
        serviceTabProvider?.let { ServiceTabRegistry.unregister(it) }
        subscriptions.forEach { it.unsubscribe() }
        subscriptions.clear()
        addProjectOption?.let { AddProjectRegistry.unregister(it) }
        dialogProvider?.let { DialogRegistry.unregister(it) }
        projectCategory?.let {
            val templates = listOf(ExtensionTemplate, ThemeTemplate, IconPackTemplate)
            templates.forEach { template -> ProjectTemplateRegistry.unregisterTemplate(it, template) }
            ProjectTemplateRegistry.unregisterCategory(it)
        }
    }
}

object GitProperty : FilePropertiesProvider {
    @Composable
    override fun provideProperties(file: FileObject): List<FileProperty> {
        val changeType = gitViewModel.get()?.getChangeType(file.getAbsolutePath()) ?: return emptyList()
        val gitStatus = changeType.name.lowercase().replaceFirstChar { it.uppercase() }
        val color =
            when (changeType) {
                ChangeType.ADDED,
                ChangeType.UNTRACKED -> MaterialTheme.colorScheme.gitAdded
                ChangeType.DELETED -> MaterialTheme.colorScheme.gitDeleted
                ChangeType.CONFLICTING -> MaterialTheme.colorScheme.gitConflicted
                ChangeType.MODIFIED -> MaterialTheme.colorScheme.gitModified
                ChangeType.RENAMED -> MaterialTheme.colorScheme.gitModified
            }
        return listOf(
            FileProperty(
                label = stringResource(strings.git_status),
                value = gitStatus,
                valueColor = color,
            )
        )
    }
}

object GitFileDecorationProvider : FileDecorationProvider {
    @Composable
    override fun provideDecoration(file: FileObject): FileDecoration? {
        if (!FeatureRegistry.isEnabled("enable_git") || !Settings.git_colorize_names) return null
        val changeType = gitViewModel.get()?.getChangeType(file.getAbsolutePath()) ?: return null
        val color =
            when (changeType) {
                ChangeType.ADDED,
                ChangeType.UNTRACKED -> MaterialTheme.colorScheme.gitAdded
                ChangeType.DELETED -> MaterialTheme.colorScheme.gitDeleted
                ChangeType.CONFLICTING -> MaterialTheme.colorScheme.gitConflicted
                ChangeType.MODIFIED -> MaterialTheme.colorScheme.gitModified
                ChangeType.RENAMED -> MaterialTheme.colorScheme.gitModified
            }
        return FileDecoration(color = color)
    }
}
