package com.scto.mobile.ide.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.scto.mobile.ide.R
import com.scto.mobile.ide.ui.terminal.DistroManager
import java.io.File
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BuildToolInfo(
    val id: String,
    val name: String,
    val description: String,
    val isInstalled: Boolean,
    val version: String?,
    val installCmd: String,
    val uninstallCmd: String,
    val updateCmd: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val generalPrefs = remember {
        context.getSharedPreferences("MobileIDE_Settings", android.content.Context.MODE_PRIVATE)
    }
    val selectedDistro = generalPrefs.getString("selected_distro", "ubuntu") ?: "ubuntu"

    var isJdk17Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isJdk21Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isGradleInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isAndroidSdkInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isPlatform34Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isPlatform35Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isCmakeInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isNdkInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isBaseUtilsInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }

    var jdk17Version by remember(refreshTrigger, selectedDistro) { mutableStateOf<String?>(null) }
    var jdk21Version by remember(refreshTrigger, selectedDistro) { mutableStateOf<String?>(null) }
    var gradleVersion by remember(refreshTrigger, selectedDistro) { mutableStateOf<String?>(null) }
    var cmakeVersion by remember(refreshTrigger, selectedDistro) { mutableStateOf<String?>(null) }
    var sdkVersion by remember(refreshTrigger, selectedDistro) { mutableStateOf<String?>(null) }
    var ndkVersion by remember(refreshTrigger, selectedDistro) { mutableStateOf<String?>(null) }

    var activeJobName by remember { mutableStateOf<String?>(null) }
    var activeJobAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshTrigger, selectedDistro) {
        withContext(Dispatchers.IO) {
            val prefixDir = context.filesDir.parentFile!!
            val distroDir = File(prefixDir, "local/$selectedDistro")
            fun getDistroFile(path: String) = File(distroDir, path)

            val hostSdk = File("/data/data/com.termux/files/home/android-sdk")
            val distroSdk = getDistroFile("root/android-sdk")
            val actualSdkDir = if (hostSdk.exists()) hostSdk else distroSdk

            // Check JDK 17
            getDistroFile("usr/lib/jvm").let { jvm ->
                val dir = jvm.listFiles()?.find { it.name.startsWith("java-17-openjdk") && File(it, "bin/java").exists() }
                isJdk17Installed = dir != null
                jdk17Version = if (dir != null) "17" else null
            }

            // Check JDK 21
            getDistroFile("usr/lib/jvm").let { jvm ->
                val dir = jvm.listFiles()?.find { it.name.startsWith("java-21-openjdk") && File(it, "bin/java").exists() }
                isJdk21Installed = dir != null
                jdk21Version = if (dir != null) "21" else null
            }

            // Check Gradle
            isGradleInstalled = getDistroFile("usr/bin/gradle").exists()
            gradleVersion = if (isGradleInstalled) "Standard" else null

            // Check Android SDK
            isAndroidSdkInstalled = actualSdkDir.exists()
            sdkVersion = if (isAndroidSdkInstalled) "Command-line Tools" else null

            // Check Platforms
            isPlatform34Installed = File(actualSdkDir, "platforms/android-34").exists()
            isPlatform35Installed = File(actualSdkDir, "platforms/android-35").exists()

            // Check CMake
            isCmakeInstalled = getDistroFile("usr/bin/cmake").exists()
            cmakeVersion = if (isCmakeInstalled) "Standard" else null

            // Check NDK
            val ndkDir = File(actualSdkDir, "ndk")
            val ndkBundleDir = File(actualSdkDir, "ndk-bundle")
            isNdkInstalled = ndkDir.exists() || ndkBundleDir.exists()
            ndkVersion = if (isNdkInstalled) {
                val versions = ndkDir.listFiles()?.map { it.name } ?: emptyList()
                if (versions.isNotEmpty()) versions.joinToString(", ") else "ndk-bundle"
            } else null

            // Check Base Build Utilities
            isBaseUtilsInstalled = getDistroFile("usr/bin/make").exists()
        }
    }

    fun runJob(jobName: String, actionName: String, command: String) {
        if (activeJobName != null) {
            Toast.makeText(context, "Ein anderer Job läuft bereits.", Toast.LENGTH_SHORT).show()
            return
        }
        activeJobName = jobName
        activeJobAction = actionName
        Toast.makeText(context, "$actionName für $jobName gestartet...", Toast.LENGTH_SHORT).show()

        val fullCommand = DistroManager.buildProotCommand(context, arrayOf("sh", "-c", command))
        val env = DistroManager.getProotEnv(context)

        thread {
            try {
                val process = ProcessBuilder(fullCommand).apply {
                    environment().putAll(env)
                    redirectErrorStream(true)
                }.start()
                process.waitFor()
                val success = process.exitValue() == 0
                (context as? Activity)?.runOnUiThread {
                    activeJobName = null
                    activeJobAction = null
                    if (success) {
                        Toast.makeText(context, "$jobName: $actionName erfolgreich abgeschlossen.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "$jobName: $actionName fehlgeschlagen (Exit Code ${process.exitValue()}).", Toast.LENGTH_LONG).show()
                    }
                    refreshTrigger++
                }
            } catch (e: Exception) {
                (context as? Activity)?.runOnUiThread {
                    activeJobName = null
                    activeJobAction = null
                    Toast.makeText(context, "$jobName: $actionName fehlgeschlagen: ${e.localizedMessage ?: "Unbekannter Fehler"}", Toast.LENGTH_LONG).show()
                    refreshTrigger++
                }
            }
        }
    }

    val tools = listOf(
        BuildToolInfo(
            id = "jdk17",
            name = "OpenJDK 17",
            description = "Java Development Kit v17, erforderlich für grundlegende Gradle-Projekte.",
            isInstalled = isJdk17Installed,
            version = jdk17Version,
            installCmd = "apk add openjdk17 || apt update && apt install -y openjdk-17-jdk",
            uninstallCmd = "apk del openjdk17 || apt remove -y openjdk-17-jdk",
            updateCmd = "apk add --upgrade openjdk17 || apt install --only-upgrade -y openjdk-17-jdk",
        ),
        BuildToolInfo(
            id = "jdk21",
            name = "OpenJDK 21",
            description = "Java Development Kit v21, empfohlen für moderne Android Gradle-Builds.",
            isInstalled = isJdk21Installed,
            version = jdk21Version,
            installCmd = "apk add openjdk21 || apt update && apt install -y openjdk-21-jdk",
            uninstallCmd = "apk del openjdk21 || apt remove -y openjdk-21-jdk",
            updateCmd = "apk add --upgrade openjdk21 || apt install --only-upgrade -y openjdk-21-jdk",
        ),
        BuildToolInfo(
            id = "gradle",
            name = "Gradle Build System",
            description = "Automatisierungstool zur Kompilierung von Android- und Kotlin-Projekten.",
            isInstalled = isGradleInstalled,
            version = gradleVersion,
            installCmd = "apk add gradle || apt update && apt install -y gradle",
            uninstallCmd = "apk del gradle || apt remove -y gradle",
            updateCmd = "apk add --upgrade gradle || apt install --only-upgrade -y gradle",
        ),
        BuildToolInfo(
            id = "android_sdk",
            name = "Android SDK Command-line Tools",
            description = "Android SDK Kommandozeilenwerkzeuge (sdkmanager) zur Verwaltung von SDK-Paketen.",
            isInstalled = isAndroidSdkInstalled,
            version = sdkVersion,
            installCmd = "mkdir -p \$HOME/android-sdk && wget -O /tmp/sdk.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && unzip -o /tmp/sdk.zip -d \$HOME/android-sdk && rm /tmp/sdk.zip",
            uninstallCmd = "rm -rf \$HOME/android-sdk",
            updateCmd = "yes | \$HOME/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=\$HOME/android-sdk --update",
        ),
        BuildToolInfo(
            id = "platform34",
            name = "Android Platform API 34",
            description = "Android SDK Platform API für Level 34 (Android 14 SDK). Erfordert Android SDK.",
            isInstalled = isPlatform34Installed,
            version = if (isPlatform34Installed) "API 34" else null,
            installCmd = "yes | \$HOME/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=\$HOME/android-sdk \"platforms;android-34\"",
            uninstallCmd = "\$HOME/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=\$HOME/android-sdk --uninstall \"platforms;android-34\"",
            updateCmd = "yes | \$HOME/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=\$HOME/android-sdk \"platforms;android-34\"",
        ),
        BuildToolInfo(
            id = "platform35",
            name = "Android Platform API 35",
            description = "Android SDK Platform API für Level 35 (Android 15 SDK). Erfordert Android SDK.",
            isInstalled = isPlatform35Installed,
            version = if (isPlatform35Installed) "API 35" else null,
            installCmd = "yes | \$HOME/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=\$HOME/android-sdk \"platforms;android-35\"",
            uninstallCmd = "\$HOME/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=\$HOME/android-sdk --uninstall \"platforms;android-35\"",
            updateCmd = "yes | \$HOME/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=\$HOME/android-sdk \"platforms;android-35\"",
        ),
        BuildToolInfo(
            id = "cmake",
            name = "CMake",
            description = "Kompilierungssystem für C/C++ (NDK) Android App Entwicklung.",
            isInstalled = isCmakeInstalled,
            version = cmakeVersion,
            installCmd = "apk add cmake || apt update && apt install -y cmake",
            uninstallCmd = "apk del cmake || apt remove -y cmake",
            updateCmd = "apk add --upgrade cmake || apt install --only-upgrade -y cmake",
        ),
        BuildToolInfo(
            id = "ndk",
            name = "Android NDK",
            description = "Native Development Kit für C/C++ Unterstützung in Android Apps. Erfordert Android SDK.",
            isInstalled = isNdkInstalled,
            version = ndkVersion,
            installCmd = "yes | \$HOME/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=\$HOME/android-sdk \"ndk-bundle\"",
            uninstallCmd = "\$HOME/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=\$HOME/android-sdk --uninstall \"ndk-bundle\"",
            updateCmd = "yes | \$HOME/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=\$HOME/android-sdk \"ndk-bundle\"",
        ),
        BuildToolInfo(
            id = "base_utils",
            name = "Base Build Utilities",
            description = "Basis-Werkzeuge (GNU Make, GCC, Git) zum Bauen von nativen Modulen.",
            isInstalled = isBaseUtilsInstalled,
            version = if (isBaseUtilsInstalled) "Make, GCC, Git" else null,
            installCmd = "apk add make gcc g++ git || apt update && apt install -y build-essential git",
            uninstallCmd = "apk del make gcc g++ git || apt remove -y build-essential git",
            updateCmd = "apk add --upgrade make gcc g++ git || apt install --only-upgrade -y build-essential git",
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_build_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Active Job Progress Bar Overlay
                AnimatedVisibility(
                    visible = activeJobName != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut(),
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${activeJobAction}: ${activeJobName}...",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                ) {
                    // Info card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Hier kannst du SDKs, SDK-Platform-APIs, CMake, NDK sowie die GNU-Build-Tools und Compiler direkt in deinem PRoot Linux-Terminal installieren und verwalten.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    // Build Tools List
                    items(tools) { tool ->
                        BuildToolCard(
                            tool = tool,
                            isJobRunning = activeJobName != null,
                            onInstall = { runJob(tool.name, "Installation", tool.installCmd) },
                            onUninstall = { runJob(tool.name, "Deinstallation", tool.uninstallCmd) },
                            onUpdate = { runJob(tool.name, "Update", tool.updateCmd) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BuildToolCard(
    tool: BuildToolInfo,
    isJobRunning: Boolean,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onUpdate: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(isInstalled = tool.isInstalled)
            }

            // Description
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Version number if installed
            if (tool.isInstalled && tool.version != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version: ${tool.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Action buttons row
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!tool.isInstalled) {
                    Button(
                        onClick = onInstall,
                        enabled = !isJobRunning,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Installieren")
                    }
                } else {
                    // Update Button
                    TextButton(
                        onClick = onUpdate,
                        enabled = !isJobRunning,
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Update")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Uninstall Button
                    Button(
                        onClick = onUninstall,
                        enabled = !isJobRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deinstallieren")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(isInstalled: Boolean) {
    val bgColor = if (isInstalled) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFF44336).copy(alpha = 0.15f)
    val textColor = if (isInstalled) Color(0xFF4CAF50) else Color(0xFFF44336)
    val text = if (isInstalled) "Installiert" else "Nicht installiert"
    val icon = if (isInstalled) Icons.Default.CheckCircle else Icons.Default.Cancel

    Surface(
        color = bgColor,
        shape = CircleShape,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
