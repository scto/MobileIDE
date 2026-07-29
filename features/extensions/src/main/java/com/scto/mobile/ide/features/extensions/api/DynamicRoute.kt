package com.scto.mobile.ide.features.extensions.api

import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

@MobileIDEExtensionPoint
data class DynamicRoute(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    val content: @Composable (NavController, NavBackStackEntry) -> Unit,
)
