package com.scto.mobile.ide.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun UpdateCheckerDialog(context: Context) {
    var hasUpdate by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf("") }
    var releaseNotes by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersion = pInfo.versionName?.substringBefore("-") ?: "0.0.0"

            withContext(Dispatchers.IO) {
                val url = URL("https://api.github.com/repos/scto/MobileIDE/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val tagName = json.getString("tag_name").removePrefix("v").substringBefore("-")

                    if (isNewerVersion(currentVersion, tagName)) {
                        latestVersion = tagName
                        releaseNotes = json.optString("body", "No release notes provided.")
                        downloadUrl = json.optString("html_url", "https://github.com/scto/MobileIDE/releases/latest")
                        hasUpdate = true
                    }
                }
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Failed to check for updates", e)
        }
    }

    if (hasUpdate) {
        AlertDialog(
            onDismissRequest = { hasUpdate = false },
            icon = { Icon(Icons.Default.SystemUpdate, contentDescription = "Update Available") },
            title = { Text("Update Available") },
            text = {
                Column {
                    Text("A new version ($latestVersion) is available!", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 10,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        hasUpdate = false
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                        context.startActivity(intent)
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = { TextButton(onClick = { hasUpdate = false }) { Text("Later") } },
        )
    }
}

private fun isNewerVersion(current: String, latest: String): Boolean {
    val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
    val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

    val length = maxOf(currentParts.size, latestParts.size)
    for (i in 0 until length) {
        val curr = currentParts.getOrElse(i) { 0 }
        val lat = latestParts.getOrElse(i) { 0 }
        if (lat > curr) return true
        if (lat < curr) return false
    }
    return false
}
