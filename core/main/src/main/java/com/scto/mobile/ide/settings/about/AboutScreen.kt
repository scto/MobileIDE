package com.scto.mobile.ide.settings.about





import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.components.SettingsItem
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceGroup
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceLayout
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceTemplate
import com.scto.mobile.ide.utils.copyToClipboard
import com.scto.mobile.ide.utils.okHttpClient
import com.scto.mobile.ide.core.main.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject











@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val pm = context.packageManager
    val appIcon = pm.getApplicationIcon(context.packageName)
    val packageInfo = pm.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)

    PreferenceLayout(label = stringResource(id = com.scto.mobile.ide.core.main.R.string.about), backArrowVisible = true) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = appIcon,
                contentDescription = null,
                modifier = Modifier.size(96.dp).padding(bottom = 8.dp),
            )

            Text(
                text = stringResource(com.scto.mobile.ide.core.main.R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = versionName.toString().uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.build_info)) {
            PreferenceTemplate(
                modifier =
                    Modifier.combinedClickable(
                        enabled = true,
                        onClick = {},
                        onLongClick = { copyToClipboard(versionName.toString()) },
                    ),
                title = {
                    Text(text = stringResource(id = com.scto.mobile.ide.core.main.R.string.version), style = MaterialTheme.typography.titleMedium)
                },
                description = { Text(text = versionName.toString(), style = MaterialTheme.typography.titleSmall) },
            )

            PreferenceTemplate(
                modifier =
                    Modifier.combinedClickable(
                        enabled = true,
                        onClick = {},
                        onLongClick = { copyToClipboard(versionCode.toString()) },
                    ),
                title = {
                    Text(text = stringResource(id = com.scto.mobile.ide.core.main.R.string.version_code), style = MaterialTheme.typography.titleMedium)
                },
                description = { Text(text = versionCode.toString(), style = MaterialTheme.typography.titleSmall) },
            )

            PreferenceTemplate(
                modifier =
                    Modifier.combinedClickable(
                        enabled = true,
                        onClick = {},
                        onLongClick = { copyToClipboard(BuildConfig.GIT_SHORT_COMMIT_HASH) },
                    ),
                title = {
                    Text(text = stringResource(id = com.scto.mobile.ide.core.main.R.string.git_commit), style = MaterialTheme.typography.titleMedium)
                },
                description = {
                    Text(text = BuildConfig.GIT_SHORT_COMMIT_HASH, style = MaterialTheme.typography.titleSmall)
                },
            )
        }

        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.community)) {
            val stars = remember { mutableStateOf(com.scto.mobile.ide.core.main.R.string.loading.getString()) }

            LaunchedEffect(Unit) {
                val client = okHttpClient
                val url = "https://api.github.com/repos/Xed-Editor/Xed-Editor"
                val request = Request.Builder().url(url).build()

                withContext(Dispatchers.IO) {
                    try {
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val jsonBody = response.body.string()
                                val json = JSONObject(jsonBody)
                                val count = json.getInt("stargazers_count")

                                withContext(Dispatchers.Main) { stars.value = count.toString() }
                            } else {
                                stars.value = com.scto.mobile.ide.core.main.R.string.error.getString()
                            }
                        }
                    } catch (e: Exception) {
                        stars.value = e.message ?: com.scto.mobile.ide.core.main.R.string.api_error.getString()
                    }
                }
            }

            PreferenceTemplate(
                title = {
                    Text(text = stringResource(com.scto.mobile.ide.core.main.R.string.github_stars), style = MaterialTheme.typography.titleMedium)
                },
                description = { Text(text = stars.value, style = MaterialTheme.typography.titleSmall) },
            )

            SettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.github),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.github_desc),
                isEnabled = true,
                showSwitch = false,
                default = false,
                endWidget = {
                    Icon(
                        modifier = Modifier.padding(16.dp),
                        painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.open_in_new),
                        contentDescription = null,
                    )
                },
                sideEffect = {
                    val url = "https://github.com/Xed-Editor/Xed-Editor"
                    val intent = Intent(Intent.ACTION_VIEW).apply { data = url.toUri() }
                    context.startActivity(intent)
                },
            )

            SettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.telegram),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.telegram_desc),
                isEnabled = true,
                showSwitch = false,
                default = false,
                endWidget = {
                    Icon(
                        modifier = Modifier.padding(16.dp),
                        painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.open_in_new),
                        contentDescription = null,
                    )
                },
                sideEffect = {
                    val url = "https://t.me/XedEditor"
                    val intent = Intent(Intent.ACTION_VIEW).apply { data = url.toUri() }
                    context.startActivity(intent)
                },
            )

            SettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.discord),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.telegram_desc),
                isEnabled = true,
                showSwitch = false,
                default = false,
                endWidget = {
                    Icon(
                        modifier = Modifier.padding(16.dp),
                        painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.open_in_new),
                        contentDescription = null,
                    )
                },
                sideEffect = {
                    val url = "https://discord.gg/6bKzcQRuef"
                    val intent = Intent(Intent.ACTION_VIEW).apply { data = url.toUri() }
                    context.startActivity(intent)
                },
            )
        }
    }
}
