package com.scto.mobile.ide.settings





import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceLayout
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceTemplate
import com.scto.mobile.ide.components.compose.preferences.category.PreferenceCategory
import com.scto.mobile.ide.feature.FeatureRegistry
import com.scto.mobile.ide.icons.XedIcon











@Composable
fun SettingsScreen(navController: NavController) {
    PreferenceLayout(label = stringResource(id = com.scto.mobile.ide.core.main.R.string.settings), backArrowVisible = true) {
        Categories(navController)
    }
}

@Composable
private fun Categories(navController: NavController) {
    PreferenceCategory(
        label = stringResource(id = com.scto.mobile.ide.core.main.R.string.app),
        description = stringResource(id = com.scto.mobile.ide.core.main.R.string.app_desc),
        iconResource = com.scto.mobile.ide.core.main.R.drawable.android,
        onNavigate = { navController.navigate(SettingsRoutes.AppSettings.route) },
    )

    PreferenceCategory(
        label = stringResource(com.scto.mobile.ide.core.main.R.string.themes),
        description = stringResource(com.scto.mobile.ide.core.main.R.string.theme_settings),
        iconResource = com.scto.mobile.ide.core.main.R.drawable.palette,
        onNavigate = { navController.navigate(SettingsRoutes.Themes.route) },
    )

    PreferenceCategory(
        label = stringResource(id = com.scto.mobile.ide.core.main.R.string.editor),
        description = stringResource(id = com.scto.mobile.ide.core.main.R.string.editor_desc),
        iconResource = com.scto.mobile.ide.core.main.R.drawable.edit_note,
        onNavigate = { navController.navigate(SettingsRoutes.EditorSettings.route) },
    )

    PreferenceCategory(
        label = stringResource(com.scto.mobile.ide.core.main.R.string.keybindings),
        description = stringResource(com.scto.mobile.ide.core.main.R.string.keybindings_desc),
        iconResource = com.scto.mobile.ide.core.main.R.drawable.keyboard,
        onNavigate = { navController.navigate(SettingsRoutes.Keybindings.route) },
    )

    SettingsRegistry.categories.forEach { category ->
        PreferenceCategory(
            label = category.label,
            description = category.description,
            startWidget = {
                XedIcon(
                    icon = category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            onNavigate = { navController.navigate(category.route) },
        )
    }

    if (FeatureRegistry.isEnabled("debug_mode")) {
        PreferenceCategory(
            label = stringResource(com.scto.mobile.ide.core.main.R.string.debug_options),
            description = com.scto.mobile.ide.core.main.R.string.debug_options_desc.getFilledString(com.scto.mobile.ide.core.main.R.string.app_name.getString()),
            iconResource = com.scto.mobile.ide.core.main.R.drawable.build,
            onNavigate = { navController.navigate(SettingsRoutes.DeveloperOptions.route) },
        )
    }

    PreferenceTemplate(
        modifier =
            Modifier.padding(horizontal = 16.dp)
                .clip(MaterialTheme.shapes.large)
                .clickable { navController.navigate(SettingsRoutes.About.route) }
                .background(Color.Transparent),
        verticalPadding = 14.dp,
        title = { Text(stringResource(id = com.scto.mobile.ide.core.main.R.string.about)) },
        description = { Text(stringResource(id = com.scto.mobile.ide.core.main.R.string.about_desc)) },
        startWidget = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )

    PreferenceTemplate(
        modifier =
            Modifier.padding(horizontal = 16.dp)
                .clip(MaterialTheme.shapes.large)
                .clickable { navController.navigate(SettingsRoutes.Support.route) }
                .background(Color.Transparent),
        verticalPadding = 14.dp,
        title = { Text(stringResource(com.scto.mobile.ide.core.main.R.string.support)) },
        description = { Text(stringResource(id = com.scto.mobile.ide.core.main.R.string.support_desc)) },
        startWidget = { HeartbeatIcon() },
    )
}

@Composable
fun HeartbeatIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")

    val scale =
        infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "scale",
        )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp).scale(scale.value)) {
        Icon(
            imageVector =
                if (Settings.donated) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Outlined.FavoriteBorder
                },
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
