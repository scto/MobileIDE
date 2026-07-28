package com.scto.mobile.ide.features.runner

import android.app.Application
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.commands.CommandProvider
import com.scto.mobile.ide.commands.ToolbarConfiguration
import com.scto.mobile.ide.commands.editor.RunCommand
import com.scto.mobile.ide.components.DialogProvider
import com.scto.mobile.ide.components.DialogRegistry
import com.scto.mobile.ide.features.extensions.api.DynamicRoute
import com.scto.mobile.ide.feature.Feature
import com.scto.mobile.ide.resources.drawables
import com.scto.mobile.ide.resources.strings
import com.scto.mobile.ide.settings.SettingsCategory
import com.scto.mobile.ide.settings.SettingsRegistry
import com.scto.mobile.ide.settings.runners.HtmlRunnerSettings
import com.scto.mobile.ide.settings.runners.RunnerSettings

class RunnerFeature : Feature {
    private var dialogProvider: DialogProvider? = null
    private var settingsCategory: SettingsCategory? = null
    private var runnersRoute: DynamicRoute? = null
    private var htmlRunnersRoute: DynamicRoute? = null

    override fun init(application: Application) {
        // Register RunnerSheet overlay
        dialogProvider =
            DialogProvider {
                if (RunnerUI.showRunnerDialog) {
                    RunnerSheet()
                }
            }
                .also { DialogRegistry.register(it) }

        // Register settings category
        settingsCategory =
            SettingsCategory(
                    labelRes = strings.runners,
                    descriptionRes = strings.runners_desc,
                    iconRes = drawables.run,
                    route = SettingsRoutes.Runners.route,
                )
                .also { SettingsRegistry.registerCategory(it) }

        // Register settings routes
        runnersRoute =
            DynamicRoute(SettingsRoutes.Runners.route) { navController, _ ->
                    RunnerSettings(navController = navController)
                }
                .also { SettingsRegistry.registerRoute(it) }

        htmlRunnersRoute =
            DynamicRoute(SettingsRoutes.HtmlRunner.route) { _, _ -> HtmlRunnerSettings() }
                .also {
                    SettingsRegistry.registerRoute(it)
                }

        // Register Run command
        CommandProvider.registerCommand(RunCommand)
        ToolbarConfiguration.addGlobalToolbarCommand(RunCommand, 0)
    }

    override fun dispose(application: Application) {
        dialogProvider?.let { DialogRegistry.unregister(it) }
        settingsCategory?.let { SettingsRegistry.unregisterCategory(it) }
        runnersRoute?.let { SettingsRegistry.unregisterRoute(it) }
        htmlRunnersRoute?.let { SettingsRegistry.unregisterRoute(it) }

        CommandProvider.unregisterCommand(RunCommand)
        ToolbarConfiguration.removeGlobalToolbarCommand(RunCommand)
    }
}
