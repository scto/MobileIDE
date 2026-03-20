package com.mobileide.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

data class SetupStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val command: String,
    val isOptional: Boolean = false
)

val SETUP_STEPS = listOf(
    SetupStep("Update Termux",
        "Update package lists and upgrade installed packages to latest versions.",
        Icons.Default.SystemUpdate,
        "pkg update -y && pkg upgrade -y"),
    SetupStep("Install OpenJDK 17",
        "Java Development Kit required to compile Android apps.",
        Icons.Default.Star,
        "pkg install -y openjdk-17"),
    SetupStep("Install Gradle",
        "Build system used by all Android projects.",
        Icons.Default.Build,
        "pkg install -y gradle"),
    SetupStep("Install Git",
        "Version control system for your projects.",
        Icons.Default.Source,
        "pkg install -y git"),
    SetupStep("Install Android Tools",
        "ADB (Android Debug Bridge) to install APKs directly on device.",
        Icons.Default.PhoneAndroid,
        "pkg install -y android-tools"),
    SetupStep("Configure Environment",
        "Set JAVA_HOME and PATH environment variables in ~/.bashrc.",
        Icons.Default.Settings,
        // Use single-quotes inside the command so \$ is literal in the shell
        "echo 'export JAVA_HOME=\$PREFIX/opt/openjdk' >> ~/.bashrc && " +
        "echo 'export PATH=\$JAVA_HOME/bin:\$PATH' >> ~/.bashrc && " +
        "source ~/.bashrc"),
    SetupStep("Storage Access (optional)",
        "Grant Termux access to device storage for project files.",
        Icons.Default.Storage,
        "termux-setup-storage",
        isOptional = true),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(vm: IDEViewModel) {
    var currentStep by remember { mutableStateOf(0) }
    var completedSteps by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val progress = completedSteps.size.toFloat() / SETUP_STEPS.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Wizard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.HOME) }) {
                        Icon(Icons.Default.Close, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Progress header
            Surface(color = IDESurface) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Setting up your environment",
                                fontWeight = FontWeight.Bold, color = IDEOnBackground)
                            Text("${completedSteps.size} of ${SETUP_STEPS.size} steps complete",
                                fontSize = 12.sp, color = IDEOnSurface)
                        }
                        Text("${(progress * 100).toInt()}%",
                            fontWeight = FontWeight.Bold, color = IDEPrimary, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = IDEPrimary,
                        trackColor = IDESurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = IDEOutline)

            Column(
                modifier = Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SETUP_STEPS.forEachIndexed { index, step ->
                    val isDone    = index in completedSteps
                    val isCurrent = index == currentStep && !isDone

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isDone    -> IDESecondary.copy(alpha = 0.07f)
                                isCurrent -> IDEPrimary.copy(alpha = 0.07f)
                                else      -> IDESurface
                            }
                        ),
                        border = BorderStroke(
                            if (isDone || isCurrent) 1.5.dp else 1.dp,
                            when { isDone -> IDESecondary.copy(alpha = 0.4f)
                                   isCurrent -> IDEPrimary.copy(alpha = 0.4f)
                                   else -> IDEOutline }
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Step indicator
                                Box(
                                    modifier = Modifier.size(36.dp)
                                        .background(
                                            when { isDone -> IDESecondary; isCurrent -> IDEPrimary; else -> IDESurfaceVariant },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone)
                                        Icon(Icons.Default.Check, null, Modifier.size(20.dp), tint = IDEBackground)
                                    else
                                        Text("${index + 1}", color = IDEOnBackground, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(step.title, fontWeight = FontWeight.SemiBold, color = IDEOnBackground)
                                        if (step.isOptional) {
                                            Spacer(Modifier.width(8.dp))
                                            Surface(shape = RoundedCornerShape(4.dp),
                                                color = IDEOutline.copy(alpha = 0.2f)) {
                                                Text("optional", fontSize = 9.sp, color = IDEOnSurface,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Text(step.description, fontSize = 12.sp, color = IDEOnSurface)
                                }
                                Icon(step.icon, null, tint = when {
                                    isDone -> IDESecondary; isCurrent -> IDEPrimary; else -> IDEOutline
                                })
                            }

                            if (isCurrent || isDone) {
                                Spacer(Modifier.height(12.dp))
                                Surface(shape = RoundedCornerShape(6.dp), color = IDEBackground) {
                                    Text(
                                        "$ ${step.command}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(10.dp),
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = IDESecondary
                                        )
                                    )
                                }

                                if (!isDone) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                vm.navigate(Screen.TERMINAL)
                                                vm.runCommand(step.command)
                                                completedSteps = completedSteps + index
                                                currentStep = (index + 1).coerceAtMost(SETUP_STEPS.lastIndex)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = IDEPrimary)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp), tint = IDEBackground)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Run in Terminal", color = IDEBackground)
                                        }
                                        if (step.isOptional) {
                                            OutlinedButton(onClick = {
                                                completedSteps = completedSteps + index
                                                currentStep = (index + 1).coerceAtMost(SETUP_STEPS.lastIndex)
                                            }) { Text("Skip") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Completion card
                if (completedSteps.size >= SETUP_STEPS.count { !it.isOptional }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IDESecondary.copy(alpha = 0.1f)),
                        border = BorderStroke(1.5.dp, IDESecondary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(40.dp), tint = IDESecondary)
                            Spacer(Modifier.height(8.dp))
                            Text("Environment Ready!", fontWeight = FontWeight.Bold,
                                fontSize = 18.sp, color = IDESecondary)
                            Text("You can now build Android apps!",
                                color = IDEOnSurface, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { vm.navigate(Screen.HOME) },
                                colors = ButtonDefaults.buttonColors(containerColor = IDESecondary)
                            ) {
                                Icon(Icons.Default.FolderOpen, null, Modifier.size(16.dp), tint = IDEBackground)
                                Spacer(Modifier.width(8.dp))
                                Text("Start Coding", color = IDEBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
