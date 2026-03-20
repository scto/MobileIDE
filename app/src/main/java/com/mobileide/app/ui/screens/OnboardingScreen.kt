package com.mobileide.app.ui.screens

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mobileide.app.AppConstants
import com.mobileide.app.ui.theme.*
import com.mobileide.app.utils.PermissionState
import com.mobileide.app.utils.StoragePermissionHelper
import com.mobileide.app.viewmodel.IDEViewModel
import kotlinx.coroutines.launch

// ── Permission check helpers ──────────────────────────────────────────────────

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun hasStorageAccess(context: Context) =
    StoragePermissionHelper.hasFullStorageAccess(context)

private fun hasNotifications(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 33)
        hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    else true   // pre-API-33 always granted

private fun hasBatteryOptimizationExemption(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun hasUsageStats(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= 29) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun hasInstallPackages(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 26)
        context.packageManager.canRequestPackageInstalls()
    else true

private fun hasTermuxPermission(context: Context): Boolean =
    hasPermission(context, "com.termux.permission.RUN_COMMAND")

// ── Snapshot all permissions ──────────────────────────────────────────────────
private fun snapshotPermissions(context: Context) = PermissionState(
    storage         = hasStorageAccess(context),
    notifications   = hasNotifications(context),
    battery         = hasBatteryOptimizationExemption(context),
    usageStats      = hasUsageStats(context),
    installPackages = hasInstallPackages(context),
    termuxRun       = hasTermuxPermission(context),
)

// ── Permission descriptor ─────────────────────────────────────────────────────
data class PermItem(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val isRequired: Boolean,
    val granted: (PermissionState) -> Boolean,
    val request: () -> Unit
)

// ═════════════════════════════════════════════════════════════════════════════
//  OnboardingScreen
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun OnboardingScreen(vm: IDEViewModel) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Live permission state (refreshed on every resume & after each grant)
    var perms by remember { mutableStateOf(snapshotPermissions(context)) }

    fun refresh() { perms = snapshotPermissions(context) }

    // ── Launchers ──────────────────────────────────────────────────────────

    // Runtime permissions (POST_NOTIFICATIONS, READ/WRITE_EXTERNAL_STORAGE ≤29)
    val runtimeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh() }

    // MANAGE_EXTERNAL_STORAGE → system settings
    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refresh() }

    // Battery optimisation → system settings
    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refresh() }

    // Usage stats → system settings
    val usageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refresh() }

    // Install packages → system settings
    val installLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refresh() }

    // ── Permission item list ───────────────────────────────────────────────
    val permItems = listOf(
        PermItem(
            label       = "Storage Access",
            description = "Read & write project files in MobileIDEProjects",
            icon        = Icons.Default.FolderOpen,
            isRequired  = true,
            granted     = { it.storage }
        ) {
            if (Build.VERSION.SDK_INT >= 30) {
                val intent = StoragePermissionHelper.buildManageStorageIntent(context)
                if (intent != null) manageStorageLauncher.launch(intent)
            } else {
                runtimeLauncher.launch(arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ))
            }
        },
        PermItem(
            label       = "Notifications",
            description = "Build status and progress notifications",
            icon        = Icons.Default.Notifications,
            isRequired  = true,
            granted     = { it.notifications }
        ) {
            if (Build.VERSION.SDK_INT >= 33) {
                runtimeLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        },
        PermItem(
            label       = "Battery Optimisation",
            description = "Keep Gradle builds running in the background",
            icon        = Icons.Default.BatteryFull,
            isRequired  = true,
            granted     = { it.battery }
        ) {
            try {
                val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                batteryLauncher.launch(i)
            } catch (_: Exception) {
                batteryLauncher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        },
        PermItem(
            label       = "Install Packages",
            description = "Install the compiled APK directly on this device",
            icon        = Icons.Default.InstallMobile,
            isRequired  = true,
            granted     = { it.installPackages }
        ) {
            if (Build.VERSION.SDK_INT >= 26) {
                installLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            }
        },
        PermItem(
            label       = "Usage Statistics",
            description = "Analyse build times and app performance",
            icon        = Icons.Default.Analytics,
            isRequired  = false,
            granted     = { it.usageStats }
        ) {
            usageLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        },
        PermItem(
            label       = "Termux: Run Command",
            description = "Execute Gradle & shell commands via Termux",
            icon        = Icons.Default.Terminal,
            isRequired  = false,
            granted     = { it.termuxRun }
        ) {
            // Open Termux app settings so the user can grant it manually
            try {
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:com.termux")
                }
                context.startActivity(i)
            } catch (_: Exception) {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
            }
            refresh()
        },
    )

    val requiredAll = perms.storage && perms.notifications && perms.battery && perms.installPackages

    // Save permissions to DataStore whenever they change
    LaunchedEffect(perms) {
        vm.savePermissions(perms)
    }

    // ── UI ─────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(IDEBackground, Color(0xFF0F0F1A))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── App logo + title ─────────────────────────────────────────
            val pulse = rememberInfiniteTransition(label = "p")
            val s by pulse.animateFloat(1f, 1.05f,
                infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "s")

            Surface(
                shape  = CircleShape,
                color  = IDEPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(90.dp).scale(s)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Code, null, Modifier.size(48.dp), tint = IDEPrimary)
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Welcome to\nMobileIDE",
                fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                color = IDEOnBackground, textAlign = TextAlign.Center, lineHeight = 34.sp)

            Spacer(Modifier.height(6.dp))

            Text("Grant the following permissions to continue",
                fontSize = 14.sp, color = IDEOnSurface, textAlign = TextAlign.Center)

            Spacer(Modifier.height(28.dp))

            // ── Overall status banner ─────────────────────────────────────
            if (requiredAll) {
                Surface(
                    shape  = RoundedCornerShape(14.dp),
                    color  = IDESecondary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, IDESecondary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null,
                            Modifier.size(22.dp), tint = IDESecondary)
                        Column {
                            Text("All required permissions granted",
                                fontWeight = FontWeight.SemiBold, color = IDESecondary)
                            Text("Optional permissions can still be granted below.",
                                fontSize = 11.sp, color = IDEOnSurface)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Permission rows ───────────────────────────────────────────
            Surface(
                shape  = RoundedCornerShape(18.dp),
                color  = IDESurface,
                border = BorderStroke(1.dp, IDEOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    permItems.forEachIndexed { idx, item ->
                        PermissionRow(
                            item    = item,
                            granted = item.granted(perms)
                        )
                        if (idx < permItems.lastIndex) {
                            HorizontalDivider(
                                color = IDEOutline.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Termux install hint ───────────────────────────────────────
            Surface(
                shape  = RoundedCornerShape(14.dp),
                color  = Color(0xFFCBA6F7).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color(0xFFCBA6F7).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Terminal, null,
                        Modifier.size(22.dp), tint = Color(0xFFCBA6F7))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Install Termux from F-Droid",
                            fontWeight = FontWeight.SemiBold,
                            color = IDEOnBackground, fontSize = 13.sp)
                        Text("Required for Gradle builds and shell commands.",
                            fontSize = 11.sp, color = IDEOnSurface)
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                val i = Intent(Intent.ACTION_VIEW,
                                    Uri.parse(AppConstants.TERMUX_FDROID_URL))
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(i)
                            } catch (_: Exception) {}
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, null, Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Get", fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Continue / Skip ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { vm.completeOnboarding() },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, IDEOutline)
                ) { Text("Skip for now", color = IDEOnSurface) }

                Button(
                    onClick = { vm.completeOnboarding() },
                    enabled = requiredAll,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IDEPrimary,
                        disabledContainerColor = IDEOutline
                    )
                ) {
                    Text(
                        if (requiredAll) "Continue" else "Grant required",
                        fontWeight = FontWeight.SemiBold,
                        color = if (requiredAll) IDEBackground else IDEOnSurface
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Single permission row ─────────────────────────────────────────────────────
@Composable
private fun PermissionRow(item: PermItem, granted: Boolean) {
    val statusColor = if (granted) IDESecondary else IDETertiary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!granted) item.request() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Permission icon
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = statusColor.copy(alpha = 0.12f),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(item.icon, null, Modifier.size(22.dp), tint = statusColor)
            }
        }

        // Label + description
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item.label,
                    fontWeight = FontWeight.SemiBold,
                    color = IDEOnBackground, fontSize = 14.sp)
                if (!item.isRequired) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = IDEOutline.copy(alpha = 0.25f)
                    ) {
                        Text("optional", fontSize = 9.sp, color = IDEOnSurface,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
            }
            Text(item.description, fontSize = 11.sp, color = IDEOnSurface)
        }

        // Status indicator — green check or red X
        if (granted) {
            Icon(Icons.Default.CheckCircle, "Granted",
                Modifier.size(26.dp), tint = IDESecondary)
        } else {
            Surface(
                shape = CircleShape,
                color = IDETertiary.copy(alpha = 0.15f),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, "Not granted",
                        Modifier.size(16.dp), tint = IDETertiary)
                }
            }
        }
    }
}
