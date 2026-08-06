package com.scto.mobile.ide.ui.editor.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class DependencyUpdate(
    val alias: String,
    val group: String,
    val name: String,
    val currentVersion: String,
    val latestVersion: String,
    var isSelected: Boolean = true
)

object DependencyUpdateChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(projectPath: String): List<DependencyUpdate> = withContext(Dispatchers.IO) {
        val updates = mutableListOf<DependencyUpdate>()
        val tomlFile = File(projectPath, "gradle/libs.versions.toml")
        if (!tomlFile.exists()) return@withContext updates

        val lines = tomlFile.readLines()
        val versionsMap = mutableMapOf<String, String>()
        var currentSection = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed.substring(1, trimmed.length - 1)
                continue
            }
            if (currentSection == "versions" && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                val key = parts[0].trim()
                val valStr = parts[1].trim().replace("\"", "").replace("'", "")
                versionsMap[key] = valStr
            }
        }

        currentSection = ""
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed.substring(1, trimmed.length - 1)
                continue
            }

            if (currentSection == "libraries" && trimmed.contains("=")) {
                try {
                    val parts = trimmed.split("=", limit = 2)
                    val alias = parts[0].trim()
                    val value = parts[1].trim()

                    var group = ""
                    var name = ""
                    var currentVer = ""

                    if (value.startsWith("{") && value.endsWith("}")) {
                        val body = value.substring(1, value.length - 1)
                        val pairs = body.split(",")
                        for (pair in pairs) {
                            val kv = pair.split("=", limit = 2)
                            if (kv.size == 2) {
                                val k = kv[0].trim()
                                val v = kv[1].trim().replace("\"", "").replace("'", "")
                                when (k) {
                                    "group" -> group = v
                                    "name" -> name = v
                                    "version.ref" -> currentVer = versionsMap[v] ?: ""
                                    "version" -> currentVer = v
                                }
                            }
                        }
                    } else if (value.contains(":")) {
                        val raw = value.replace("\"", "").replace("'", "")
                        val triple = raw.split(":")
                        if (triple.size >= 3) {
                            group = triple[0]
                            name = triple[1]
                            currentVer = triple[2]
                        }
                    }

                    if (group.isNotEmpty() && name.isNotEmpty() && currentVer.isNotEmpty()) {
                        val latestVer = fetchLatestMavenVersion(group, name)
                        if (latestVer != null && isNewerVersion(currentVer, latestVer)) {
                            updates.add(DependencyUpdate(alias, group, name, currentVer, latestVer))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return@withContext updates
    }

    private fun fetchLatestMavenVersion(group: String, name: String): String? {
        return try {
            val url = "https://search.maven.org/solrsearch/select?q=g:%22$group%22+AND+a:%22$name%22&rows=1&wt=json"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val docs = json.getJSONObject("response").getJSONArray("docs")
                if (docs.length() > 0) {
                    docs.getJSONObject(0).getString("latestVersion")
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        if (current == latest) return false
        val cParts = current.split(".", "-", "+").mapNotNull { it.toIntOrNull() }
        val lParts = latest.split(".", "-", "+").mapNotNull { it.toIntOrNull() }
        val length = maxOf(cParts.size, lParts.size)
        for (i in 0 until length) {
            val c = cParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (c > l) return false
        }
        return false
    }

    suspend fun applyUpdates(projectPath: String, selectedUpdates: List<DependencyUpdate>): Boolean = withContext(Dispatchers.IO) {
        try {
            val tomlFile = File(projectPath, "gradle/libs.versions.toml")
            if (!tomlFile.exists()) return@withContext false

            var content = tomlFile.readText()
            for (update in selectedUpdates) {
                content = content.replace("\"${update.currentVersion}\"", "\"${update.latestVersion}\"")
                content = content.replace("'${update.currentVersion}'", "'${update.latestVersion}'")
            }
            tomlFile.writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

@Composable
fun DependencyUpdateDialog(
    updates: List<DependencyUpdate>,
    onDismiss: () -> Unit,
    onConfirm: (List<DependencyUpdate>) -> Unit
) {
    var updateList by remember { mutableStateOf(updates) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Update, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Text(
                "Bibliotheken-Updates verfügbar",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Es wurden ${updates.size} neuere Versionen für deine Abhängigkeiten in libs.versions.toml gefunden:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(updateList) { item ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Checkbox(
                                    checked = item.isSelected,
                                    onCheckedChange = { checked ->
                                        updateList = updateList.map {
                                            if (it.alias == item.alias) it.copy(isSelected = checked) else it
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                    Text(
                                        item.alias,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "${item.group}:${item.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            item.currentVersion,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            " → ",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            item.latestVersion,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selected = updateList.filter { it.isSelected }
                    onConfirm(selected)
                },
                enabled = updateList.any { it.isSelected }
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ausgewählte aktualisieren")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
