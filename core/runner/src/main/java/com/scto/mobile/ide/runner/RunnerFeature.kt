package com.scto.mobile.ide.runner

import android.app.Application
import com.scto.mobile.ide.commands.CommandProvider
import com.scto.mobile.ide.commands.editor.RunCommand
import com.scto.mobile.ide.feature.Feature
import com.scto.mobile.ide.feature.SettingsRegistry
import com.scto.mobile.ide.feature.SettingsCategory
import com.scto.mobile.ide.feature.SettingsRoute
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.components.DialogRegistry
import com.scto.mobile.ide.resources.drawables
import com.scto.mobile.ide.resources.strings
import com.scto.mobile.ide.settings.runners.RunnerSettings
import com.scto.mobile.ide.settings.runners.HtmlRunnerSettings

class RunnerFeature : Feature {
    override fun init(application: Application) {
        // Register RunnerSheet overlay
        DialogRegistry.dialogs.add {
            if (RunnerUI.showRunnerDialog) {
                RunnerSheet()
            }
        }
        // Register settings category
        SettingsRegistry.registerCategory(
            SettingsCategory(
                labelRes = strings.runners,
                descriptionRes = strings.runners_desc,
                iconRes = drawables.run,
                route = SettingsRoutes.Runners.route
            )
        )

        // Register settings routes
        SettingsRegistry.registerRoute(
            SettingsRoute(SettingsRoutes.Runners.route) { navController ->
                RunnerSettings(navController = navController)
            }
        )
        SettingsRegistry.registerRoute(
            SettingsRoute(SettingsRoutes.HtmlRunner.route) {
                HtmlRunnerSettings()
            }
        )

        // Register Run command
        val runCommand = RunCommand()
        CommandProvider.registerCommand(runCommand)
    }
}
