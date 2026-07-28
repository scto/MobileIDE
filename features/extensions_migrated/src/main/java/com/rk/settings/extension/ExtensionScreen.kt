package com.scto.mobile.ide.settings.extension

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import com.scto.mobile.ide.components.MobileIDEDropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.GsonBuilder
import com.scto.mobile.ide.App
import com.scto.mobile.ide.App.Companion.iconPackManager
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceGroup
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceTemplate
import com.scto.mobile.ide.components.compose.preferences.base.RefreshablePreferenceLayoutLazyColumn
import com.scto.mobile.ide.features.extensions.Extension
import com.scto.mobile.ide.features.extensions.UpdatableExtension
import com.scto.mobile.ide.features.extensions.extensionManager
import com.scto.mobile.ide.features.extensions.manager.ExtensionRegistry
import com.scto.mobile.ide.features.extensions.manager.IconPackEntry
import com.scto.mobile.ide.features.extensions.manager.ThemeEntry
import com.scto.mobile.ide.file.child
import com.scto.mobile.ide.file.copyToTempDir
import com.scto.mobile.ide.file.themeDir
import com.scto.mobile.ide.file.toFileObject
import com.scto.mobile.ide.icons.Download
import com.scto.mobile.ide.icons.MobileIDEIcons
import com.scto.mobile.ide.resources.drawables
import com.scto.mobile.ide.resources.strings
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.settings.theme.themes
import com.scto.mobile.ide.theme.ThemeConfig
import com.scto.mobile.ide.theme.Typography
import com.scto.mobile.ide.theme.installFromFile
import com.scto.mobile.ide.theme.installTheme
import com.scto.mobile.ide.utils.LoadingPopup
import com.scto.mobile.ide.utils.errorDialog
import com.scto.mobile.ide.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ExtensionSortOptions(val stringRes: Int) {
    NAME(strings.name),
    RATING(strings.rating),
    DOWNLOAD_COUNT(strings.download_count),
    PUBLISH_DATE(strings.publish_date),
    UPDATE_DATE(strings.update_date),
}

private enum class ExtensionFilterOptions(val stringRes: Int) {
    ALL(strings.all),
    SUPPORTED(strings.supported),
    CRASHED(strings.status_crashed),
}

private enum class StoreCategory(val stringRes: Int, val drawableRes: Int) {
    EXTENSIONS(strings.ext, drawables.extension),
    THEMES(strings.themes, drawables.palette),
    ICON_PACKS(strings.icon_packs, drawables.widgets),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionScreen(navController: NavController, query: String?) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? AppCompatActivity
    val scope = rememberCoroutineScope()

    var isRefreshing by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    var selectedCategory by remember { mutableStateOf(StoreCategory.EXTENSIONS) }

    var currentSortOption by remember { mutableStateOf(ExtensionSortOptions.DOWNLOAD_COUNT) }
    var currentFilterOption by remember { mutableStateOf(ExtensionFilterOptions.ALL) }
    val searchQuery = rememberTextFieldState(query ?: "")

    val dialogManager = remember { ExtensionDialogManager() }

    var isIndexing by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }

    var storeThemes by remember { mutableStateOf<List<ThemeEntry>>(emptyList()) }
    var storeIconPacks by remember { mutableStateOf<List<IconPackEntry>>(emptyList()) }

    LaunchedEffect(refreshKey) {
        val shouldLoad =
            refreshKey > 0 ||
                extensionManager.localExtensions.isEmpty() ||
                extensionManager.storeExtension.isEmpty() ||
                storeThemes.isEmpty() ||
                storeIconPacks.isEmpty()

        if (shouldLoad) {
            isIndexing = true
            isFetching = true

            val localJob =
                launch(Dispatchers.IO) {
                    runCatching { extensionManager.indexLocalExtensions() }
                    isIndexing = false
                }
            val storeJob =
                launch(Dispatchers.IO) {
                    runCatching { extensionManager.indexStoreExtensions() }
                    isFetching = false
                }
            val themesJob =
                launch(Dispatchers.IO) {
                    runCatching {
                        val list = ExtensionRegistry.fetchThemes()
                        withContext(Dispatchers.Main) {
                            storeThemes = list
                        }
                    }
                }
            val iconPacksJob =
                launch(Dispatchers.IO) {
                    runCatching {
                        val list = ExtensionRegistry.fetchIconPacks()
                        withContext(Dispatchers.Main) {
                            storeIconPacks = list
                        }
                    }
                }

            localJob.join()
            storeJob.join()
            themesJob.join()
            iconPacksJob.join()
            isRefreshing = false
        }
    }

    val filePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
            installExtensionFromUri(scope, uri, activity)
        }

    val themeFilePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    val loading = LoadingPopup(activity, null)
                    loading.show()
                    runCatching {
                        installFromFile(uri.toFileObject(expectedIsFile = true))
                    }
                    loading.hide()
                }
            }
        }

    val iconPackFilePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    val loading = LoadingPopup(activity, null)
                    loading.show()
                    runCatching {
                        val file = uri.toFileObject(expectedIsFile = true).copyToTempDir()
                        iconPackManager.installIconPack(file)
                    }
                    loading.hide()
                }
            }
        }

    val localExtensions by remember {
        derivedStateOf {
            val allLocal = extensionManager.getLocalExtensions()
            val filtered = applyFilter(searchQuery, allLocal, currentFilterOption)
            applySort(currentSortOption, filtered)
        }
    }
    val hasLocalExtensions by remember {
        derivedStateOf {
            extensionManager.getLocalExtensions().isNotEmpty()
        }
    }

    val storeExtensions by remember {
        derivedStateOf {
            val allStore = extensionManager.getStoreExtensions()
            val filtered = applyFilter(searchQuery, allStore, currentFilterOption)
            applySort(currentSortOption, filtered)
        }
    }

    val sortedThemes by remember {
        derivedStateOf {
            val query = searchQuery.text
            val filtered =
                if (query.isEmpty()) {
                    storeThemes
                } else {
                    storeThemes.filter { theme ->
                        theme.manifest.name.contains(query, ignoreCase = true)
                    }
                }
            filtered.sortedBy { theme ->
                theme.manifest.name
            }
        }
    }

    val sortedIconPacks by remember {
        derivedStateOf {
            val query = searchQuery.text
            val filtered =
                if (query.isEmpty()) {
                    storeIconPacks
                } else {
                    storeIconPacks.filter { pack ->
                        pack.manifest.name.contains(query, ignoreCase = true)
                    }
                }
            filtered.sortedBy { it.manifest.name }
        }
    }

    RefreshablePreferenceLayoutLazyColumn(
        label = stringResource(strings.store),
        isExpandedScreen = false,
        backArrowVisible = true,
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            refreshKey++
        },
        fab = {
            ExtendedFloatingActionButton(
                onClick = {
                    when (selectedCategory) {
                        StoreCategory.EXTENSIONS -> {
                            if (Settings.warn_extensions) {
                                dialogManager.showWarning {
                                    filePickerLauncher.launch(arrayOf("application/zip"))
                                }
                            } else {
                                filePickerLauncher.launch(arrayOf("application/zip"))
                            }
                        }
                        StoreCategory.THEMES -> themeFilePickerLauncher.launch(arrayOf("application/json"))
                        StoreCategory.ICON_PACKS -> iconPackFilePickerLauncher.launch(arrayOf("application/zip"))
                    }
                },
                icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(strings.install_from_storage)) },
            )
        },
    ) {
        item {
            ExtensionDialogRenderer(dialogManager)
        }

        when (selectedCategory) {
            StoreCategory.EXTENSIONS -> {
                item {
                    ExtensionSearchBar(
                        searchQuery = searchQuery,
                        currentSortOption = currentSortOption,
                        currentFilterOption = currentFilterOption,
                        onSortOptionChange = { currentSortOption = it },
                        onFilterOptionChange = { currentFilterOption = it },
                    )
                }
            }
            StoreCategory.THEMES -> {
                item {
                    StoreSearchBar(
                        searchQuery = searchQuery,
                        placeholderText = stringResource(strings.search_themes),
                    )
                }
            }
            StoreCategory.ICON_PACKS -> {
                item {
                    StoreSearchBar(
                        searchQuery = searchQuery,
                        placeholderText = stringResource(strings.search_icon_packs),
                    )
                }
            }
        }

        item {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedCategory.ordinal,
                modifier = Modifier.padding(bottom = 8.dp),
                edgePadding = 16.dp,
            ) {
                StoreCategory.entries.forEach { category ->
                    LeadingIconTab(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        text = {
                            Text(stringResource(category.stringRes))
                        },
                        icon = {
                            Icon(
                                painter = painterResource(category.drawableRes),
                                contentDescription = null,
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        when (selectedCategory) {
            StoreCategory.EXTENSIONS -> {
                if (storeExtensions.isNotEmpty() || localExtensions.isNotEmpty() || isIndexing || isFetching) {
                    if (localExtensions.isNotEmpty()) {
                        item {
                            PreferenceGroup(heading = stringResource(strings.local)) {
                                localExtensions.forEach { extension ->
                                    key(extension.id) {
                                        val installState = rememberInstallState(extension)

                                        ExtensionCard(
                                            extension = extension,
                                            installState = installState,
                                            onInstallClick = {
                                                val action = {
                                                    val missing = getMissingDependencies(extension)
                                                    if (missing.isNotEmpty()) {
                                                        dialogManager.showDependencies(extension, missing) {
                                                            runExtensionInstallAction(extension, {}, context, activity)
                                                        }
                                                    } else {
                                                        runExtensionInstallAction(extension, {}, context, activity)
                                                    }
                                                }

                                                if (Settings.warn_extensions) {
                                                    dialogManager.showWarning(action)
                                                } else {
                                                    action()
                                                }
                                            },
                                            onUninstallClick = {
                                                runExtensionUninstallAction(extension, {}, scope, activity)
                                            },
                                            onUpdateClick = {
                                                if (extension !is UpdatableExtension) return@ExtensionCard
                                                val missing = getMissingDependencies(extension)
                                                if (missing.isNotEmpty()) {
                                                    dialogManager.showDependencies(extension, missing) {
                                                        runExtensionUpdateAction(extension, {}, context, activity)
                                                    }
                                                } else {
                                                    runExtensionUpdateAction(extension, {}, context, activity)
                                                }
                                            },
                                            onClick = {
                                                navController.navigate(
                                                    "${SettingsRoutes.ExtensionDetail.route}/${it.id}"
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (storeExtensions.isNotEmpty()) {
                        item {
                            PreferenceGroup(
                                heading =
                                    stringResource(strings.store).takeIf {
                                        hasLocalExtensions
                                    }
                            ) {
                                storeExtensions.forEach { extension ->
                                    key(extension.id) {
                                        val installState = rememberInstallState(extension)

                                        ExtensionCard(
                                            extension = extension,
                                            installState = installState,
                                            onInstallClick = {
                                                val action = {
                                                    val missing = getMissingDependencies(extension)
                                                    if (missing.isNotEmpty()) {
                                                        dialogManager.showDependencies(extension, missing) {
                                                            runExtensionInstallAction(extension, {}, context, activity)
                                                        }
                                                    } else {
                                                        runExtensionInstallAction(extension, {}, context, activity)
                                                    }
                                                }

                                                if (Settings.warn_extensions) {
                                                    dialogManager.showWarning(action)
                                                } else {
                                                    action()
                                                }
                                            },
                                            onUninstallClick = {
                                                runExtensionUninstallAction(extension, {}, scope, activity)
                                            },
                                            onUpdateClick = {
                                                if (extension !is UpdatableExtension) return@ExtensionCard
                                                val missing = getMissingDependencies(extension)
                                                if (missing.isNotEmpty()) {
                                                    dialogManager.showDependencies(extension, missing) {
                                                        runExtensionUpdateAction(extension, {}, context, activity)
                                                    }
                                                } else {
                                                    runExtensionUpdateAction(extension, {}, context, activity)
                                                }
                                            },
                                            onClick = {
                                                navController.navigate(
                                                    "${SettingsRoutes.ExtensionDetail.route}/${it.id}"
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isIndexing || isFetching) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement =
                                    Arrangement.spacedBy(16.dp, alignment = Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(text = stringResource(strings.loading))
                            }
                        }
                    }
                } else {
                    item {
                        PreferenceGroup {
                            Text(
                                text = stringResource(strings.no_ext),
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
            StoreCategory.THEMES -> {
                if (sortedThemes.isNotEmpty() || isRefreshing) {
                    item {
                        PreferenceGroup {
                            sortedThemes.forEach { themeEntry ->
                                key(themeEntry.id) {
                                    val isInstalled = themes.any { it.id == themeEntry.id }
                                    ThemeStoreCard(
                                        themeEntry = themeEntry,
                                        isInstalled = isInstalled,
                                        onInstallClick = {
                                            val manifestJsonString = themeEntry.manifest.toString()
                                            val gson =
                                                GsonBuilder()
                                                    .excludeFieldsWithModifiers(java.lang.reflect.Modifier.STATIC)
                                                    .create()
                                            val themeConfig = gson.fromJson(manifestJsonString, ThemeConfig::class.java)
                                            DefaultScope.launch(Dispatchers.IO) {
                                                runCatching {
                                                    themeConfig.installTheme()
                                                }
                                                    .onSuccess {
                                                        withContext(Dispatchers.Main) {
                                                            toast(strings.installed)
                                                        }
                                                    }
                                                    .onFailure { err ->
                                                        withContext(Dispatchers.Main) {
                                                            errorDialog(activity, err)
                                                        }
                                                    }
                                            }
                                        },
                                        onUninstallClick = {
                                            val installedTheme = themes.find { it.id == themeEntry.id }
                                            if (installedTheme != null) {
                                                DefaultScope.launch(Dispatchers.IO) {
                                                    runCatching {
                                                        themeDir().child(installedTheme.name).delete()
                                                        withContext(Dispatchers.Main) {
                                                            themes.remove(installedTheme)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        PreferenceGroup {
                            Text(
                                text = stringResource(strings.no_themes),
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
            StoreCategory.ICON_PACKS -> {
                if (sortedIconPacks.isNotEmpty() || isRefreshing) {
                    item {
                        PreferenceGroup {
                            sortedIconPacks.forEach { iconPackEntry ->
                                key(iconPackEntry.id) {
                                    val isInstalled = iconPackManager.iconPacks.containsKey(iconPackEntry.id)
                                    IconPackStoreCard(
                                        iconPackEntry = iconPackEntry,
                                        isInstalled = isInstalled,
                                        onInstallClick = {
                                            runIconPackInstallAction(
                                                iconPackEntry.id,
                                                iconPackEntry.manifest.name,
                                                context,
                                                activity,
                                            )
                                        },
                                        onUninstallClick = {
                                            DefaultScope.launch(Dispatchers.IO) {
                                                runCatching {
                                                    iconPackManager.uninstallIconPack(iconPackEntry.id)
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        PreferenceGroup {
                            Text(
                                text = stringResource(strings.no_icon_packs),
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }

        item {
            // Add extra space so that FAB doesn't cover content
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StoreSearchBar(
    searchQuery: TextFieldState,
    placeholderText: String,
) {
    SearchBarDefaults.InputField(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        state = searchQuery,
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = {
            IconButton({ searchQuery.clearText() }) {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = stringResource(strings.close))
            }
        },
        onSearch = {},
        expanded = false,
        onExpandedChange = {},
        placeholder = { Text(placeholderText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
fun ThemeStoreCard(
    themeEntry: ThemeEntry,
    isInstalled: Boolean,
    onInstallClick: () -> Unit,
    onUninstallClick: () -> Unit,
) {
    val name = themeEntry.manifest.name
    val progress = ExtensionRegistry.downloadProgress[themeEntry.id]
    PreferenceTemplate(
        modifier = Modifier.fillMaxWidth(),
        startWidget = {
            Icon(
                painter = painterResource(drawables.palette),
                contentDescription = null,
                modifier = Modifier.size(48.dp).padding(8.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(text = name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        description = {
            Column {
                Text(
                    text = themeEntry.id,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = Typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (progress != null) {
                    Spacer(Modifier.height(4.dp))
                    if (progress >= 0f) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        endWidget = {
            if (isInstalled) {
                IconButton(
                    onClick = onUninstallClick,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(strings.delete))
                }
            } else {
                IconButton(onClick = onInstallClick) {
                    Icon(MobileIDEIcons.Download, contentDescription = null)
                }
            }
        },
    )
}

@Composable
fun IconPackStoreCard(
    iconPackEntry: IconPackEntry,
    isInstalled: Boolean,
    onInstallClick: () -> Unit,
    onUninstallClick: () -> Unit,
) {
    val name = iconPackEntry.manifest.name
    val id = iconPackEntry.manifest.id
    val progress = ExtensionRegistry.downloadProgress[id]
    PreferenceTemplate(
        modifier = Modifier.fillMaxWidth(),
        startWidget = {
            Icon(
                painter = painterResource(drawables.widgets),
                contentDescription = null,
                modifier = Modifier.size(48.dp).padding(8.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
        },
        title = {
            Text(text = name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        description = {
            Column {
                Text(
                    text = id,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = Typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (progress != null) {
                    Spacer(Modifier.height(4.dp))
                    if (progress >= 0f) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        endWidget = {
            if (isInstalled) {
                IconButton(
                    onClick = onUninstallClick,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(strings.delete))
                }
            } else {
                IconButton(onClick = onInstallClick) {
                    Icon(MobileIDEIcons.Download, contentDescription = null)
                }
            }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExtensionSearchBar(
    searchQuery: TextFieldState,
    currentSortOption: ExtensionSortOptions,
    currentFilterOption: ExtensionFilterOptions,
    onSortOptionChange: (ExtensionSortOptions) -> Unit,
    onFilterOptionChange: (ExtensionFilterOptions) -> Unit,
) {
    var searchOptionsExpanded by remember { mutableStateOf(false) }

    SearchBarDefaults.InputField(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        state = searchQuery,
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton({ searchOptionsExpanded = true }) {
                        Icon(painter = painterResource(drawables.filter), contentDescription = null)
                    }

                    DropdownMenu(searchOptionsExpanded, { searchOptionsExpanded = false }) {
                        Text(
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp, end = 16.dp),
                            text = stringResource(strings.sort_options),
                            style = Typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        ExtensionSortOptions.entries.forEach { sortOption ->
                            MobileIDEDropdownMenuItem(
                                text = { Text(stringResource(sortOption.stringRes)) },
                                onClick = {
                                    onSortOptionChange(sortOption)
                                    searchOptionsExpanded = false
                                },
                                leadingIcon = { RadioButton(currentSortOption == sortOption, null) },
                            )
                        }

                        HorizontalDivider()

                        Text(
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp, end = 16.dp),
                            text = stringResource(strings.filter_options),
                            style = Typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        ExtensionFilterOptions.entries.forEach { filterOption ->
                            MobileIDEDropdownMenuItem(
                                text = { Text(stringResource(filterOption.stringRes)) },
                                onClick = {
                                    onFilterOptionChange(filterOption)
                                    searchOptionsExpanded = false
                                },
                                leadingIcon = { RadioButton(currentFilterOption == filterOption, null) },
                            )
                        }
                    }
                }

                IconButton({ searchQuery.clearText() }) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = stringResource(strings.close))
                }
            }
        },
        onSearch = {},
        expanded = false,
        onExpandedChange = {},
        placeholder = {
            Text(stringResource(strings.search_extensions), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
    )
}

private fun applySort(
    currentSortOption: ExtensionSortOptions,
    filtered: List<Extension>,
): List<Extension> =
    when (currentSortOption) {
        ExtensionSortOptions.NAME -> filtered.sortedBy { it.name }
        ExtensionSortOptions.RATING -> filtered.sortedByDescending { it.rating }
        ExtensionSortOptions.DOWNLOAD_COUNT -> filtered.sortedByDescending { it.downloads }
        ExtensionSortOptions.PUBLISH_DATE -> filtered.sortedByDescending { it.createdAt }
        ExtensionSortOptions.UPDATE_DATE -> filtered.sortedByDescending { it.updatedAt }
    }

private fun applyFilter(
    searchQuery: TextFieldState,
    extensions: List<Extension>,
    currentFilterOption: ExtensionFilterOptions,
): List<Extension> {
    val query = searchQuery.text
    val filteredBySearchQuery =
        if (query.isEmpty()) {
            extensions
        } else {
            extensions.filter { extension ->
                val labelMatch = extension.name.contains(query, ignoreCase = true)
                val descriptionMatch = extension.description?.contains(query, ignoreCase = true) == true
                val tagMatch = extension.tags.any { it.contains(query, ignoreCase = true) }
                val authorMatch = extension.author.displayName.contains(query, ignoreCase = true)
                val authorGithubMatch = extension.author.github?.contains(query, ignoreCase = true) == true
                labelMatch || descriptionMatch || tagMatch || authorMatch || authorGithubMatch
            }
        }

    val mobileideVersionCode = App.versionCode
    return filteredBySearchQuery.filter {
        val minAppVersion = it.minAppVersion
        val outdatedClient = minAppVersion != null && mobileideVersionCode < minAppVersion

        val currentArchitecture = Build.SUPPORTED_ABIS.firstOrNull()
        val supportedArchitecture =
            currentArchitecture == null || it.supportedArchitectures?.contains(currentArchitecture) ?: true

        when (currentFilterOption) {
            ExtensionFilterOptions.ALL -> true
            ExtensionFilterOptions.SUPPORTED -> !outdatedClient && supportedArchitecture
            ExtensionFilterOptions.CRASHED -> extensionManager.isExtensionCrashed(it)
        }
    }
}
