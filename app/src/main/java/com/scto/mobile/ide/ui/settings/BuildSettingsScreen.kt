package com.scto.mobile.ide.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.scto.mobile.ide.R
import com.scto.mobile.ide.ui.terminal.DistroManager
import java.io.File
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val generalPrefs = remember { context.getSharedPreferences("MobileIDE_Settings", android.content.Context.MODE_PRIVATE) }
    val selectedDistro = generalPrefs.getString("selected_distro", "ubuntu") ?: "ubuntu"

    var isJdk17Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isJdk21Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isGradleInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isAndroidSdkInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isBuildTools35Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isBuildTools36Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isPlatform34Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isPlatform35Installed by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isCmakeInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isNdkInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isBaseUtilsInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger, selectedDistro) {
        withContext(Dispatchers.IO) {
            val prefixDir = context.filesDir.parentFile!!
            val distroDir = File(prefixDir, "local/$selectedDistro")
            fun getDistroFile(path: String) = File(distroDir, path)

            isJdk17Installed = getDistroFile("usr/lib/jvm/java-17-openjdk/bin/java").exists() || getDistroFile("usr/lib/jvm/java-17-openjdk-amd64/bin/java").exists()
            isJdk21Installed = getDistroFile("usr/lib/jvm/java-21-openjdk/bin/java").exists() || getDistroFile("usr/lib/jvm/java-21-openjdk-amd64/bin/java").exists()
            isGradleInstalled = getDistroFile("usr/bin/gradle").exists()
            val hostSdk = File("/data/data/com.termux/files/home/android-sdk")
            val distroSdk = getDistroFile("root/android-sdk")
            isAndroidSdkInstalled = hostSdk.exists() || distroSdk.exists()
            isBuildTools35Installed = File(hostSdk, "build-tools/35.0.0").exists() || getDistroFile("root/android-sdk/build-tools/35.0.0").exists()
            isBuildTools36Installed = File(hostSdk, "build-tools/36.0.0").exists() || getDistroFile("root/android-sdk/build-tools/36.0.0").exists()
            isPlatform34Installed = File(hostSdk, "platforms/android-34").exists() || getDistroFile("root/android-sdk/platforms/android-34").exists()
            isPlatform35Installed = File(hostSdk, "platforms/android-35").exists() || getDistroFile("root/android-sdk/platforms/android-35").exists()
            isCmakeInstalled = getDistroFile("usr/bin/cmake").exists()
            isNdkInstalled = File(hostSdk, "ndk").exists() || getDistroFile("root/android-sdk/ndk").exists() || File(hostSdk, "ndk-bundle").exists()
            isBaseUtilsInstalled = getDistroFile("usr/bin/make").exists()
        }
    }

    fun runInstall(jobName: String, command: String) {
        Toast.makeText(context, context.getString(R.string.toast_terminal_reinstall_start), Toast.LENGTH_SHORT).show()
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
                (context as android.app.Activity).runOnUiThread {
                    if (success) {
                        Toast.makeText(context, context.getString(R.string.toast_install_success, jobName), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.toast_install_failed, jobName, "Exit code " + process.exitValue()), Toast.LENGTH_LONG).show()
                    }
                    refreshTrigger++
                }
            } catch (e: Exception) {
                (context as android.app.Activity).runOnUiThread {
                    Toast.makeText(context, context.getString(R.string.toast_install_failed, jobName, e.localizedMessage ?: "Unknown Error"), Toast.LENGTH_LONG).show()
                    refreshTrigger++
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_build_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            BuildSettingsItem(
                isJdk17Installed = isJdk17Installed,
                isJdk21Installed = isJdk21Installed,
                isGradleInstalled = isGradleInstalled,
                isAndroidSdkInstalled = isAndroidSdkInstalled,
                isBuildTools35Installed = isBuildTools35Installed,
                isBuildTools36Installed = isBuildTools36Installed,
                isPlatform34Installed = isPlatform34Installed,
                isPlatform35Installed = isPlatform35Installed,
                isCmakeInstalled = isCmakeInstalled,
                isNdkInstalled = isNdkInstalled,
                isBaseUtilsInstalled = isBaseUtilsInstalled,
                onInstall = { name, cmd -> runInstall(name, cmd) }
            )
        }
    }
}
