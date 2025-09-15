package com.mobileide.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWrapper(
    content: @Composable () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
        val context = LocalContext.current
        if (Environment.isExternalStorageManager()) {
            content()
        } else {
            ManageStoragePermissionScreen {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        }
    } else { // Android 10 und darunter
        val storagePermissionState = rememberPermissionState(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (storagePermissionState.hasPermission) {
            content()
        } else {
            RuntimePermissionScreen(
                onPermissionRequested = { storagePermissionState.launchPermissionRequest() }
            )
        }
    }
}

@Composable
fun ManageStoragePermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Zugriff auf alle Dateien benötigt",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Um Projekte zu verwalten und Dateien zu bearbeiten, benötigt diese App die Berechtigung, auf alle Dateien zuzugreifen. Bitte aktiviere diese in den Einstellungen.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequest) {
            Text("Zu den Einstellungen")
        }
    }
}

@Composable
fun RuntimePermissionScreen(onPermissionRequested: () -> Unit) {
     Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Speicherzugriff erforderlich",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Diese App benötigt Zugriff auf deinen Speicher, um Dateien lesen und anzeigen zu können.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onPermissionRequested) {
            Text("Berechtigung erteilen")
        }
    }
}
