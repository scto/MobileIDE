package com.scto.mobile.ide.settings.support





import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.activities.settings.SettingsActivity
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.components.SettingsItem
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceGroup
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceLayout
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.utils.dialogRes
import com.scto.mobile.ide.utils.isDialogShowing
import com.scto.mobile.ide.utils.toast











fun isUPISupported(context: Context): Boolean {
    // 1. Check if the user's region is India (Most reliable indicator for UPI)
    val currentLocale = context.resources.configuration.locales[0]
    val isIndia = currentLocale.country.equals("IN", ignoreCase = true)

    // 2. Check if there is at least one app capable of handling a UPI URI
    val uri = "upi://pay".toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri)
    val packageManager = context.packageManager

    // Check if any app can resolve this intent
    val canHandleUPI =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager
                .queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
                )
                .isNotEmpty()
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
        }

    return isIndia || canHandleUPI
}

@Composable
fun Support(modifier: Modifier = Modifier) {
    PreferenceLayout(label = stringResource(com.scto.mobile.ide.core.main.R.string.support), backArrowVisible = true) {
        val context = LocalContext.current

        PreferenceGroup {
            SettingsItem(
                label = "GitHub Sponsors",
                
                isEnabled = true,
                showSwitch = false,
                default = false,
                startWidget = {
                    Icon(
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                        painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.github),
                        contentDescription = null,
                    )
                },
                endWidget = {
                    Icon(
                        modifier = Modifier.padding(16.dp),
                        painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.open_in_new),
                        contentDescription = null,
                    )
                },
                sideEffect = {
                    val url = "https://github.com/sponsors/RohitKushvaha01"
                    val intent = Intent(Intent.ACTION_VIEW).apply { data = url.toUri() }
                    context.startActivity(intent)
                    Settings.donated = true
                },
            )
            SettingsItem(
                label = "Buy Me a Coffee",
                
                isEnabled = true,
                showSwitch = false,
                default = false,
                startWidget = {
                    Icon(
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                        painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.coffee),
                        contentDescription = null,
                    )
                },
                endWidget = {
                    Icon(
                        modifier = Modifier.padding(16.dp),
                        painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.open_in_new),
                        contentDescription = null,
                    )
                },
                sideEffect = {
                    val url = "https://buymeacoffee.com/rohitkushvaha01"
                    val intent = Intent(Intent.ACTION_VIEW).apply { data = url.toUri() }
                    context.startActivity(intent)
                    Settings.donated = true
                },
            )
            val upiAvailable = remember { isUPISupported(context) }
            if (upiAvailable) {
                SettingsItem(
                    label = "UPI",
                    
                    isEnabled = true,
                    showSwitch = false,
                    default = false,
                    startWidget = {
                        Icon(
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                            painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.upi_pay),
                            contentDescription = null,
                        )
                    },
                    endWidget = {
                        Icon(
                            modifier = Modifier.padding(16.dp),
                            painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.open_in_new),
                            contentDescription = null,
                        )
                    },
                    sideEffect = {
                        val uri =
                            "upi://pay"
                                .toUri()
                                .buildUpon()
                                .appendQueryParameter("pa", "rohitkushwaha01x@axl")
                                .appendQueryParameter("pn", "Rohit Kushwaha")
                                .appendQueryParameter("tn", "Xed-Editor")
                                .appendQueryParameter("cu", "INR")
                                .build()
                        val intent = Intent(Intent.ACTION_VIEW).apply { data = uri }

                        val chooser = Intent.createChooser(intent, com.scto.mobile.ide.core.main.R.string.use.getString())
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(chooser)
                            Settings.donated = true
                        } else {
                            toast(com.scto.mobile.ide.core.main.R.string.no_upi_error)
                        }
                    },
                )
            }
        }
    }
}

fun Activity.handleSupport() {
    if (isDialogShowing) return

    val currentTime = System.currentTimeMillis()

    // Don't ask users who explicitly said they don't find value
    if (Settings.user_declined_value) return

    // Don't ask if they already supported
    if (Settings.user_has_supported) return

    // Calculate cooldown based on last response
    val cooldownPeriod =
        when {
            Settings.user_said_maybe_later -> 7L * 24 * 60 * 60 * 1000 // 1 week
            else -> 14L * 24 * 60 * 60 * 1000 // First time or other: 2 weeks
        }

    if (currentTime - Settings.last_donation_dialog_timestamp < cooldownPeriod) {
        return
    }

    // Wait for meaningful engagement
    val totalEngagement = Settings.saves + Settings.runs
    val threshold =
        when (Settings.donation_ask_count) {
            0 -> 80 // First ask: wait for real usage
            1 -> 200 // Second ask: they're a regular user
            else -> 500 // Third+ ask: power user
        }

    if (totalEngagement < threshold) return

    Settings.last_donation_dialog_timestamp = currentTime
    Settings.donation_ask_count++

    showCombinedDonationDialog()
}

private fun Activity.showCombinedDonationDialog() {
    dialogRes(
        activity = this,
        title = com.scto.mobile.ide.core.main.R.string.enjoying_xed.getString(),
        msg = com.scto.mobile.ide.core.main.R.string.support_message.getFilledString(Settings.saves.toString(), Settings.runs.toString()),
        okRes = com.scto.mobile.ide.core.main.R.string.yes_support,
        cancelRes = com.scto.mobile.ide.core.main.R.string.not_for_me,
        cancelable = false,
        onCancel = {
            // User doesn't find value - stop asking
            Settings.user_declined_value = true
            Settings.user_said_maybe_later = false
        },
        onOk = {
            // User clicked support
            Settings.user_has_supported = true
            Settings.user_said_maybe_later = false
            val intent =
                Intent(this, SettingsActivity::class.java).apply { putExtra("route", SettingsRoutes.Support.route) }
            startActivity(intent)
        },
    )
}
