package com.scto.mobile.ide.settings.language





import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.components.InfoBlock
import com.scto.mobile.ide.components.SettingsItem
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceGroup
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceLayout
import com.scto.mobile.ide.events.AppEvent
import com.scto.mobile.ide.events.Events
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.utils.application
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext











// Data class to hold locale with its availability status
data class LocaleInfo(val locale: Locale, val isInstalled: Boolean, val displayName: String)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LanguageScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Single state for processed locale data
    val localeInfoList = remember { mutableStateOf<List<LocaleInfo>?>(null) }
    val currentLocale = LocalConfiguration.current.locales[0]

    // Load and process locales once
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val supportedLocales = readSupportedLocales(context)
            val installedTags = application?.resources?.assets?.locales?.toSet() ?: emptySet()

            // Process all data at once
            val processed = supportedLocales.map { locale ->
                val tag = locale.toLanguageTag()
                LocaleInfo(
                    locale = locale,
                    isInstalled = installedTags.contains(tag),
                    displayName = "${locale.getDisplayLanguage(locale)} ($tag)",
                )
            }
            localeInfoList.value = processed
        }
    }

    PreferenceLayout(
        label = stringResource(com.scto.mobile.ide.core.main.R.string.lang),
        backArrowVisible = true,
        fab = {
            ExtendedFloatingActionButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, "https://hosted.weblate.org/engage/xed-editor/".toUri())
                    )
                },
                text = { Text(stringResource(com.scto.mobile.ide.core.main.R.string.translate)) },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
            )
        },
    ) {
        InfoBlock(
            icon = { Icon(imageVector = Icons.Outlined.Warning, contentDescription = null) },
            text = stringResource(com.scto.mobile.ide.core.main.R.string.change_lang_warn),
            warning = true,
        )

        PreferenceGroup {
            val locales = localeInfoList.value
            val selectedLocaleInfo =
                remember(currentLocale, locales) {
                    locales?.let { list ->
                        // Exact match (e.g. "en-US")
                        list.find { it.locale.toLanguageTag() == currentLocale.toLanguageTag() }
                            // Fallback to language match (e.g. "en")
                            ?: list.find { it.locale.language == currentLocale.language }
                    }
                }

            if (locales != null) {
                locales.forEach { localeInfo ->
                    SettingsItem(
                        modifier = Modifier,
                        label = localeInfo.displayName,
                        default = false,
                        sideEffect = { setAppLanguage(localeInfo.locale, currentLocale) },
                        showSwitch = false,
                        isEnabled = localeInfo.isInstalled,
                        startWidget = {
                            RadioButton(
                                selected = selectedLocaleInfo == localeInfo,
                                onClick = { setAppLanguage(localeInfo.locale, currentLocale) },
                            )
                        },
                    )
                }
            } else {
                SettingsItem(
                    modifier = Modifier,
                    label = stringResource(com.scto.mobile.ide.core.main.R.string.loading),
                    default = false,
                    sideEffect = {},
                    showSwitch = false,
                    startWidget = {},
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

// Extract function outside composable to avoid recreation
private suspend fun readSupportedLocales(context: Context): List<Locale> =
    withContext(Dispatchers.IO) {
        return@withContext context.assets.open("supported_locales.json").use { stream ->
            val json = stream.bufferedReader().use { it.readText() }
            val localeStrings: List<String> = Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
            localeStrings.map { Locale.forLanguageTag(it) }
        }
    }

fun setAppLanguage(locale: Locale, oldLocale: Locale) {
    val appLocale = LocaleListCompat.create(locale)
    AppCompatDelegate.setApplicationLocales(appLocale)
    Settings.current_lang = locale.toLanguageTag()

    DefaultScope.launch {
        Events.publish(AppEvent.LanguageChanged(locale, oldLocale))
    }
}
