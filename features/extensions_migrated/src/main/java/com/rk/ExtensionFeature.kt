package com.scto.mobile.ide

import android.app.Application
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.features.extensions.ActivityProvider
import com.scto.mobile.ide.features.extensions.api.DynamicRoute
import com.scto.mobile.ide.features.extensions.extensionManager
import com.scto.mobile.ide.features.extensions.loader.loadAllExtensions
import com.scto.mobile.ide.features.extensions.manager.ExtensionAPIManager
import com.scto.mobile.ide.features.extensions.manager.ExtensionManager
import com.scto.mobile.ide.feature.Feature
import com.scto.mobile.ide.feature.FeatureToggle
import com.scto.mobile.ide.resources.drawables
import com.scto.mobile.ide.resources.strings
import com.scto.mobile.ide.settings.SettingsCategory
import com.scto.mobile.ide.settings.SettingsRegistry
import com.scto.mobile.ide.settings.extension.ExtensionDetail
import com.scto.mobile.ide.settings.extension.ExtensionScreen
import com.scto.mobile.ide.settings.extension.ExtensionSettings
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ExtensionFeature : Feature {
    override val toggle =
        FeatureToggle(
            nameRes = strings.ext,
            key = "enable_extension",
            default = true,
            iconRes = drawables.extension,
        )

    private var settingsCategory: SettingsCategory? = null
    private val routes = mutableListOf<DynamicRoute>()

    @OptIn(DelicateCoroutinesApi::class)
    override fun init(application: Application) {
        extensionManager = ExtensionManager(application)

        // Initialize and load extensions
        GlobalScope.launch(Dispatchers.IO) {
            extensionManager.indexLocalExtensions()
            extensionManager.loadAllExtensions()
            application.registerActivityLifecycleCallbacks(ExtensionAPIManager)
            application.registerActivityLifecycleCallbacks(ActivityProvider)
        }

        // Register settings category
        settingsCategory =
            SettingsCategory(
                    labelRes = strings.store,
                    descriptionRes = strings.store_desc,
                    iconRes = drawables.store,
                    route = SettingsRoutes.Extensions.route,
                )
                .also { SettingsRegistry.registerCategory(it) }

        // Register settings routes
        routes.add(
            DynamicRoute(
                "${SettingsRoutes.Extensions.route}?query={query}",
                arguments =
                    listOf(
                        navArgument(
                            "query",
                            builder = {
                                nullable = true
                                type = NavType.StringType
                            },
                        )
                    ),
            ) { navController, backStackEntry ->
                val query = backStackEntry.arguments?.getString("query")
                ExtensionScreen(navController = navController, query)
            }
        )
        routes.add(
            DynamicRoute("${SettingsRoutes.ExtensionDetail.route}/{extensionId}") { navController, backStackEntry ->
                val extensionId = backStackEntry.arguments?.getString("extensionId")
                val extension = extensionId?.let { extensionManager.getExtension(it) }
                ExtensionDetail(extension, navController)
            }
        )
        routes.add(
            DynamicRoute("${SettingsRoutes.ExtensionSettings.route}/{extensionId}") { _, backStackEntry ->
                val extensionId = backStackEntry.arguments?.getString("extensionId")
                val extension = extensionId?.let { extensionManager.getExtension(it) }
                ExtensionSettings(extension)
            }
        )

        routes.forEach { SettingsRegistry.registerRoute(it) }
    }

    override fun dispose(application: Application) {
        extensionManager.unloadAllExtensions()
        application.unregisterActivityLifecycleCallbacks(ExtensionAPIManager)
        application.unregisterActivityLifecycleCallbacks(ActivityProvider)

        settingsCategory?.let { SettingsRegistry.unregisterCategory(it) }
        routes.forEach { SettingsRegistry.unregisterRoute(it) }
        routes.clear()
    }
}
