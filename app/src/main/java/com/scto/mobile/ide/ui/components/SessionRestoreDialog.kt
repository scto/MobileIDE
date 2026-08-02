package com.scto.mobile.ide.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.scto.mobile.ide.R
import com.scto.mobile.ide.utils.SavedSessionState

@Composable
fun SessionRestoreDialog(
    sessionState: SavedSessionState,
    onRestore: () -> Unit,
    onDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDiscard,
        title = { Text(text = "Vorherige Sitzung wiederherstellen?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "Es wurde eine zuvor gespeicherte Sitzung mit ${sessionState.openTabs.size} geöffneten Datei(en) gefunden. Möchten Sie die Sitzung wiederherstellen?"
            )
        },
        confirmButton = {
            Button(onClick = onRestore) {
                Text("Wiederherstellen")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDiscard) {
                Text("Neu beginnen")
            }
        },
    )
}

@Composable
fun AppExitConfirmDialog(
    onConfirmExitWithSave: () -> Unit,
    onConfirmExitWithoutSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "MobileIDE beenden?", fontWeight = FontWeight.Bold) },
        text = {
            Text(text = "Möchten Sie MobileIDE beenden? Geöffnete Dateien und der App-Zustand können vor dem Beenden automatisch gespeichert werden.")
        },
        confirmButton = {
            Button(onClick = onConfirmExitWithSave) {
                Text("Speichern & Beenden")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onConfirmExitWithoutSave) {
                Text("Ohne Speichern beenden")
            }
        },
    )
}
