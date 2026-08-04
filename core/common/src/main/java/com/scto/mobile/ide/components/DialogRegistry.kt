package com.scto.mobile.ide.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf

object DialogRegistry {
    val dialogs = mutableStateListOf<@Composable () -> Unit>()

    fun register(dialog: @Composable () -> Unit) {
        if (!dialogs.contains(dialog)) {
            dialogs.add(dialog)
        }
    }

    fun unregister(dialog: @Composable () -> Unit) {
        dialogs.remove(dialog)
    }

    fun clear() {
        dialogs.clear()
    }
}
