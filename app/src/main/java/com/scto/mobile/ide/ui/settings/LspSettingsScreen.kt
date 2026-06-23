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
fun LspSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val generalPrefs = remember {
        context.getSharedPreferences("MobileIDE_Settings", android.content.Context.MODE_PRIVATE)
    }
    val selectedDistro = generalPrefs.getString("selected_distro", "ubuntu") ?: "ubuntu"

    var isJdtlsInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isKotlinLsInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isTsLsInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }
    var isWebLsInstalled by remember(refreshTrigger, selectedDistro) { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger, selectedDistro) {
        withContext(Dispatchers.IO) {
            val prefixDir = context.filesDir.parentFile!!
            val distroDir = File(prefixDir, "local/$selectedDistro")
            fun getDistroFile(path: String) = File(distroDir, path)

            isJdtlsInstalled = getDistroFile("usr/bin/jdtls").exists()
            isKotlinLsInstalled = getDistroFile("usr/bin/kotlin-language-server").exists()
            val tsFile1 = getDistroFile("usr/bin/typescript-language-server")
            val tsFile2 = getDistroFile("usr/local/bin/typescript-language-server")
            isTsLsInstalled = tsFile1.exists() || tsFile2.exists()
            val htmlFile1 = getDistroFile("usr/bin/vscode-html-language-server")
            val htmlFile2 = getDistroFile("usr/local/bin/vscode-html-language-server")
            isWebLsInstalled = htmlFile1.exists() || htmlFile2.exists()
        }
    }

    fun runInstall(jobName: String, command: String) {
        Toast.makeText(context, context.getString(R.string.toast_terminal_reinstall_start), Toast.LENGTH_SHORT).show()
        val fullCommand = DistroManager.buildProotCommand(context, arrayOf("sh", "-c", command))
        val env = DistroManager.getProotEnv(context)
        thread {
            try {
                val process =
                    ProcessBuilder(fullCommand)
                        .apply {
                            environment().putAll(env)
                            redirectErrorStream(true)
                        }
                        .start()
                process.waitFor()
                val success = process.exitValue() == 0
                (context as android.app.Activity).runOnUiThread {
                    if (success) {
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_install_success, jobName),
                                Toast.LENGTH_LONG,
                            )
                            .show()
                    } else {
                        Toast.makeText(
                                context,
                                context.getString(
                                    R.string.toast_install_failed,
                                    jobName,
                                    "Exit code " + process.exitValue(),
                                ),
                                Toast.LENGTH_LONG,
                            )
                            .show()
                    }
                    refreshTrigger++
                }
            } catch (e: Exception) {
                (context as android.app.Activity).runOnUiThread {
                    Toast.makeText(
                            context,
                            context.getString(
                                R.string.toast_install_failed,
                                jobName,
                                e.localizedMessage ?: "Unknown Error",
                            ),
                            Toast.LENGTH_LONG,
                        )
                        .show()
                    refreshTrigger++
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_lsp_servers_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            LspSettingsItem(
                isJdtlsInstalled = isJdtlsInstalled,
                isKotlinLsInstalled = isKotlinLsInstalled,
                isTsLsInstalled = isTsLsInstalled,
                isWebLsInstalled = isWebLsInstalled,
                onInstall = { name, cmd -> runInstall(name, cmd) },
            )
        }
    }
}
