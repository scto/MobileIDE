package com.scto.mobile.ide.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf

object DialogRegistry {
    val dialogs = mutableStateListOf<@Composable () -> Unit>()
}
