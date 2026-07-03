package com.scto.mobile.ide.ui.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProjectSettingsDialog(projectPath: String, onDismiss: () -> Unit) {
    var appName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("1.0") }
    var versionCode by remember { mutableStateOf("1") }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    val propertiesFile = File(projectPath, "gradle.properties")
    val stringsFile = File(projectPath, "app/src/main/res/values/strings.xml")

    LaunchedEffect(projectPath) {
        withContext(Dispatchers.IO) {
            // Load properties
            if (propertiesFile.exists()) {
                val props = Properties()
                FileInputStream(propertiesFile).use { props.load(it) }
                packageName = props.getProperty("APP_PACKAGE_NAME", "")
                versionName = props.getProperty("APP_VERSION_NAME", "1.0")
                versionCode = props.getProperty("APP_VERSION_CODE", "1")
            }

            // Load app name from strings.xml
            if (stringsFile.exists()) {
                val content = stringsFile.readText()
                val regex = """<string name="app_name">([^<]+)</string>""".toRegex()
                val match = regex.find(content)
                if (match != null) {
                    appName = match.groupValues[1]
                }
            }
            isLoading = false
        }
    }

    if (isLoading) {
        return // or show a loading spinner
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Project Settings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = { Text("Version Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = versionCode,
                    onValueChange = { versionCode = it },
                    label = { Text("Version Code") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        // Save properties
                        val props = Properties()
                        if (propertiesFile.exists()) {
                            FileInputStream(propertiesFile).use { props.load(it) }
                        } else {
                            propertiesFile.parentFile?.mkdirs()
                        }
                        props.setProperty("APP_PACKAGE_NAME", packageName)
                        props.setProperty("APP_VERSION_NAME", versionName)
                        props.setProperty("APP_VERSION_CODE", versionCode)

                        FileOutputStream(propertiesFile).use { props.store(it, "MobileIDE Project Properties") }

                        // Save app name
                        if (stringsFile.exists()) {
                            var content = stringsFile.readText()
                            val regex = """<string name="app_name">([^<]+)</string>""".toRegex()
                            if (content.contains(regex)) {
                                content = content.replace(regex, """<string name="app_name">$appName</string>""")
                            } else {
                                // Basic fallback insertion before </resources>
                                content =
                                    content.replace(
                                        "</resources>",
                                        """    <string name="app_name">$appName</string>
</resources>""",
                                    )
                            }
                            stringsFile.writeText(content)
                        }

                        withContext(Dispatchers.Main) { onDismiss() }
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
