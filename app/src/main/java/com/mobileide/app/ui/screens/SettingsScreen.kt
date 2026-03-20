package com.mobileide.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: IDEViewModel) {
    val currentTheme by vm.currentThemeName.collectAsState()
    val autoSave     by vm.autoSave.collectAsState()
    val lineNums     by vm.lineNumbers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.HOME) }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Editor ────────────────────────────────────────────────────────
            SSection("Editor")
            ActionCard(Icons.Default.Tune, "Editor Settings",
                "Font size, theme, autocomplete, brackets, indent, word wrap") {
                vm.navigate(Screen.EDITOR_SETTINGS)
            }
            ActionCard(Icons.Default.Palette, "App Theme",
                "Material3 colour theme, AMOLED mode, Material You") {
                vm.navigate(Screen.APP_THEME)
            }

            // ── Tools ─────────────────────────────────────────────────────────
            SSection("Development")
            ActionCard(Icons.Default.Code, "Developer Settings",
                "Debug log, build info, Logger architecture") {
                vm.navigate(Screen.DEV_SETTINGS)
            }

                        SSection("Tools & Environment")
            ActionCard(Icons.Default.AutoFixHigh, "Setup Wizard",
                "First-run guided Termux environment setup") {
                vm.navigate(Screen.SETUP_WIZARD)
            }
            ActionCard(Icons.Default.Inventory2, "Package Manager",
                "Browse and install Termux packages") {
                vm.navigate(Screen.PACKAGE_MANAGER)
            }
            ActionCard(Icons.Default.Keyboard, "Keyboard Shortcuts",
                "All keyboard shortcuts and gestures reference") {
                vm.navigate(Screen.KEYBOARD_HELP)
            }
            ActionCard(Icons.Default.Analytics, "Project Statistics",
                "Lines of code, file types, quality metrics") {
                vm.navigate(Screen.PROJECT_STATS)
            }
            ActionCard(Icons.Default.Build, "Gradle Tasks",
                "Browse and run all available Gradle tasks") {
                vm.navigate(Screen.GRADLE_TASKS)
            }
            ActionCard(Icons.Default.CheckCircle, "Check Environment",
                "Verify Java, Gradle, Git & ADB installation") {
                vm.checkEnvironment()
                vm.navigate(Screen.TERMINAL)
            }
            ActionCard(Icons.Default.OpenInNew, "Open Termux",
                "Launch the Termux terminal application") {
                vm.termux.openTermux()
            }

            // ── About ─────────────────────────────────────────────────────────
            SSection("About MobileIDE v5")
            Card(colors = CardDefaults.cardColors(containerColor = IDESurface),
                border = BorderStroke(1.dp, IDEOutline)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, null, tint = IDEPrimary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("MobileIDE v5.0", fontWeight = FontWeight.Bold,
                                color = IDEOnBackground)
                            Text("Full-featured Android IDE for Termux",
                                style = MaterialTheme.typography.bodySmall, color = IDEOnSurface)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    // Current theme display
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Active theme:", fontSize = 12.sp, color = IDEOnSurface,
                            modifier = Modifier.width(100.dp))
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = IDEPrimary.copy(alpha = 0.15f)) {
                            Text(currentTheme, fontSize = 12.sp, color = IDEPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    val stats = listOf(
                        "44 Kotlin source files",
                        "9,000+ lines of Compose UI",
                        "18 Screens",
                        "12 Components",
                        "10 Utility classes",
                        "Sora Editor + TextMate grammars",
                        "7 color themes (Catppuccin, Dracula, One Dark, Monokai, Nord, Solarized, GitHub)",
                        "6 syntax grammars (Kotlin, Java, XML, JSON, Groovy, Markdown)",
                        "Git integration with Diff Viewer",
                        "Gradle Task Runner",
                        "LogCat viewer",
                        "30+ dependency catalog",
                        "Code Formatter + Import Organizer",
                        "Project-wide Search (Regex)",
                        "Code Outline + TODO Scanner",
                        "DataStore workspace persistence",
                        "Onboarding with permission flow",
                        "Splash Screen",
                        "Split Editor",
                        "Run Configurations"
                    )
                    stats.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, Modifier.size(12.dp), tint = IDESecondary)
                            Spacer(Modifier.width(6.dp))
                            Text(s, style = MaterialTheme.typography.bodySmall, color = IDEOnSurface)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("compileSdk 36 · minSdk 26 · Kotlin 2.2.0 · AGP 8.11.1",
                        fontSize = 10.sp, color = IDEOutline, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun SSection(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall,
        color = IDEPrimary, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = IDESurface),
        border = BorderStroke(1.dp, IDEOutline)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = IDEPrimary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = IDEOnBackground, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = IDEOnSurface)
            }
            Icon(Icons.Default.ChevronRight, null, tint = IDEOutline)
        }
    }
}
