package com.mobileide.app.ui.screens.settings.debugOptions

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mobileide.app.ui.screens.settings.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugOptionsScreen(vm: IDEViewModel) {
    val context = LocalContext.current
    var strictMode    by remember { mutableStateOf(true) }
    var anrMonitor    by remember { mutableStateOf(true) }
    var detailedErrors by remember { mutableStateOf(true) }
    var desktopMode   by remember { mutableStateOf(false) }
    var themeSwitcher by remember { mutableStateOf(false) }

    val actMgr = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo().also { actMgr.getMemoryInfo(it) }
    val usedMb  = (memInfo.totalMem - memInfo.availMem) / 1024 / 1024
    val totalMb = memInfo.totalMem / 1024 / 1024

    Scaffold(
        topBar = { SettingsTopBar("Debug-Optionen", onBack = { vm.navigate(Screen.SETTINGS) }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsGroup {
                SettingsItem("Absturz erzwingen", "Eine Runtime-Exception werfen")
                SettingsItem("Arbeitsspeicher-Nutzung", "$usedMb/${totalMb}MB")
                SettingsToggle("Strikter Modus", "Festplatten- oder Netzwerkzugriff im Hauptthread erkennen", strictMode) { strictMode = it }
                SettingsToggle("ANR-Überwachung", "MobileIDE beenden, wenn es länger als 5 Sekunden nicht reagiert", anrMonitor) { anrMonitor = it }
                SettingsToggle("Detaillierte Fehlermeldungen", "Stacktrace in Fehlermeldungen anzeigen", detailedErrors) { detailedErrors = it }
                SettingsToggle("Desktop-Modus (experimentell)", "Erleichtert die Verwendung der App auf Tablets und Desktop-Bildschirmen", desktopMode) { desktopMode = it }
                SettingsToggle("Themen-Wechsler", "Den Thema-Modus ständig alle 7s ändern", themeSwitcher) { themeSwitcher = it }
                SettingsItem("Zustimmungsstatus zurücksetzen", "Nutzungsbedingungen beim nächsten Start anzeigen")
                SettingsItem("Logs anzeigen", "Anwendungs-Logs anzeigen")
            }
        }
    }
}
