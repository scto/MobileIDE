package com.scto.mobile.ide.features.pluginstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scto.mobile.ide.features.pluginstore.manager.PluginStoreManager
import com.scto.mobile.ide.features.pluginstore.model.PluginStatus
import com.scto.mobile.ide.features.pluginstore.model.PluginType
import com.scto.mobile.ide.features.pluginstore.model.StorePluginItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MainTab {
    DISCOVER,
    INSTALLED
}

enum class CategoryFilter {
    ALL,
    LANGUAGES,
    THEMES,
    FORMATTERS,
    TOOLS
}

data class PluginStoreUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val mainTab: MainTab = MainTab.DISCOVER,
    val searchQuery: String = "",
    val selectedCategory: CategoryFilter = CategoryFilter.ALL,
    val plugins: List<StorePluginItem> = emptyList(),
    val selectedDetailPlugin: StorePluginItem? = null,
    val errorMessage: String? = null
) {
    val discoverPlugins: List<StorePluginItem>
        get() = plugins.filter { item ->
            val matchesCat = when (selectedCategory) {
                CategoryFilter.ALL -> true
                CategoryFilter.LANGUAGES -> item.type == PluginType.LSP
                CategoryFilter.THEMES -> item.type == PluginType.THEME
                CategoryFilter.FORMATTERS -> item.type == PluginType.FORMATTER
                CategoryFilter.TOOLS -> item.type == PluginType.TOOL || item.type == PluginType.UNKNOWN
            }

            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                item.name.lowercase().contains(q) ||
                        item.description.lowercase().contains(q) ||
                        item.id.lowercase().contains(q) ||
                        item.tags.any { it.lowercase().contains(q) }
            }

            matchesCat && matchesSearch
        }

    val installedPlugins: List<StorePluginItem>
        get() = plugins.filter { item ->
            val isInst = item.status == PluginStatus.INSTALLED || item.status == PluginStatus.UPDATE_AVAILABLE
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                item.name.lowercase().contains(q) ||
                        item.description.lowercase().contains(q) ||
                        item.id.lowercase().contains(q) ||
                        item.tags.any { it.lowercase().contains(q) }
            }
            isInst && matchesSearch
        }

    val groupedDiscoverPlugins: Map<String, List<StorePluginItem>>
        get() = discoverPlugins.groupBy { item ->
            when (item.type) {
                PluginType.LSP -> "Sprachen & LSP Server"
                PluginType.THEME -> "Themes & Farben"
                PluginType.FORMATTER -> "Formatter & Code Style"
                PluginType.TOOL -> "Tools & Debugger"
                else -> "Weitere Erweiterungen"
            }
        }
}

class PluginStoreViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = PluginStoreManager(application)

    private val _uiState = MutableStateFlow(PluginStoreUiState())
    val uiState: StateFlow<PluginStoreUiState> = _uiState.asStateFlow()

    init {
        loadPlugins()
    }

    fun loadPlugins(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !isRefresh,
                isRefreshing = isRefresh,
                errorMessage = null
            )

            try {
                val list = manager.fetchCatalog()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    plugins = list
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = e.message ?: "Katalog konnte nicht geladen werden"
                )
            }
        }
    }

    fun setMainTab(tab: MainTab) {
        _uiState.value = _uiState.value.copy(mainTab = tab)
    }

    fun setSelectedCategory(cat: CategoryFilter) {
        _uiState.value = _uiState.value.copy(selectedCategory = cat)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectDetailPlugin(plugin: StorePluginItem?) {
        _uiState.value = _uiState.value.copy(selectedDetailPlugin = plugin)
    }

    fun togglePluginEnabled(plugin: StorePluginItem) {
        val updated = _uiState.value.plugins.map { item ->
            if (item.id == plugin.id) {
                item.copy(isEnabled = !item.isEnabled)
            } else item
        }
        _uiState.value = _uiState.value.copy(
            plugins = updated,
            selectedDetailPlugin = if (_uiState.value.selectedDetailPlugin?.id == plugin.id) {
                _uiState.value.selectedDetailPlugin?.copy(isEnabled = !plugin.isEnabled)
            } else _uiState.value.selectedDetailPlugin
        )
    }

    fun installPlugin(item: StorePluginItem) {
        viewModelScope.launch {
            val depCheck = manager.checkDependencies(item)
            if (depCheck.hasMissingDependencies) {
                val warning = "Hinweis: Benötigte Runtimes fehlen in der Sandbox: ${depCheck.missingRuntimes.joinToString(", ")}"
                updatePluginStatus(item.id, PluginStatus.ERROR, errorMsg = warning)
                return@launch
            }

            updatePluginStatus(item.id, PluginStatus.DOWNLOADING, progress = 0.05f)

            val result = manager.installPlugin(item) { progress ->
                updatePluginStatus(item.id, PluginStatus.DOWNLOADING, progress = progress)
            }

            if (result.isSuccess) {
                updatePluginStatus(item.id, PluginStatus.INSTALLED, installedVer = item.version)
            } else {
                updatePluginStatus(
                    item.id,
                    PluginStatus.ERROR,
                    errorMsg = result.exceptionOrNull()?.message ?: "Installation fehlgeschlagen"
                )
            }
        }
    }

    fun updatePlugin(item: StorePluginItem) {
        viewModelScope.launch {
            updatePluginStatus(item.id, PluginStatus.DOWNLOADING, progress = 0.05f)

            val result = manager.updatePlugin(item) { progress ->
                updatePluginStatus(item.id, PluginStatus.DOWNLOADING, progress = progress)
            }

            if (result.isSuccess) {
                updatePluginStatus(item.id, PluginStatus.INSTALLED, installedVer = item.version)
            } else {
                updatePluginStatus(
                    item.id,
                    PluginStatus.ERROR,
                    errorMsg = result.exceptionOrNull()?.message ?: "Update fehlgeschlagen"
                )
            }
        }
    }

    fun uninstallPlugin(item: StorePluginItem) {
        viewModelScope.launch {
            val result = manager.uninstallPlugin(item)
            if (result.isSuccess) {
                updatePluginStatus(item.id, PluginStatus.NOT_INSTALLED, installedVer = null)
            } else {
                updatePluginStatus(
                    item.id,
                    PluginStatus.ERROR,
                    errorMsg = result.exceptionOrNull()?.message ?: "Deinstallation fehlgeschlagen"
                )
            }
        }
    }

    private fun updatePluginStatus(
        pluginId: String,
        status: PluginStatus,
        progress: Float = 0f,
        installedVer: String? = null,
        errorMsg: String? = null
    ) {
        val updated = _uiState.value.plugins.map { plugin ->
            if (plugin.id == pluginId) {
                plugin.copy(
                    status = status,
                    downloadProgress = progress,
                    installedVersion = installedVer ?: plugin.installedVersion,
                    errorMessage = errorMsg
                )
            } else plugin
        }
        val updatedDetail = if (_uiState.value.selectedDetailPlugin?.id == pluginId) {
            _uiState.value.selectedDetailPlugin?.copy(
                status = status,
                downloadProgress = progress,
                installedVersion = installedVer ?: _uiState.value.selectedDetailPlugin?.installedVersion,
                errorMessage = errorMsg
            )
        } else _uiState.value.selectedDetailPlugin

        _uiState.value = _uiState.value.copy(plugins = updated, selectedDetailPlugin = updatedDetail)
    }
}
