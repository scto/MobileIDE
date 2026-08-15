package com.scto.mobile.ide.features.pluginstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scto.mobile.ide.features.pluginstore.model.PluginStatus
import com.scto.mobile.ide.features.pluginstore.model.PluginType
import com.scto.mobile.ide.features.pluginstore.model.StorePluginItem
import com.scto.mobile.ide.features.pluginstore.repository.PluginStoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class FilterTab {
    ALL,
    LSP,
    THEMES,
    INSTALLED
}

data class PluginStoreUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val selectedTab: FilterTab = FilterTab.ALL,
    val plugins: List<StorePluginItem> = emptyList(),
    val errorMessage: String? = null
) {
    val filteredPlugins: List<StorePluginItem>
        get() = plugins.filter { item ->
            val matchesTab = when (selectedTab) {
                FilterTab.ALL -> true
                FilterTab.LSP -> item.type == PluginType.LSP
                FilterTab.THEMES -> item.type == PluginType.THEME
                FilterTab.INSTALLED -> item.status == PluginStatus.INSTALLED || item.status == PluginStatus.UPDATE_AVAILABLE
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

            matchesTab && matchesSearch
        }
}

class PluginStoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PluginStoreRepository(application)

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
                val list = repository.fetchPluginList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    plugins = list
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = e.message ?: "Failed to load plugins catalog"
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setSelectedTab(tab: FilterTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun installPlugin(item: StorePluginItem) {
        viewModelScope.launch {
            updatePluginStatus(item.id, PluginStatus.DOWNLOADING, progress = 0.05f)

            val result = repository.installPlugin(item) { progress ->
                updatePluginStatus(item.id, PluginStatus.DOWNLOADING, progress = progress)
            }

            if (result.isSuccess) {
                updatePluginStatus(item.id, PluginStatus.INSTALLED, installedVer = item.version)
            } else {
                updatePluginStatus(
                    item.id,
                    PluginStatus.ERROR,
                    errorMsg = result.exceptionOrNull()?.message ?: "Installation failed"
                )
            }
        }
    }

    fun uninstallPlugin(item: StorePluginItem) {
        viewModelScope.launch {
            val result = repository.uninstallPlugin(item)
            if (result.isSuccess) {
                updatePluginStatus(item.id, PluginStatus.NOT_INSTALLED, installedVer = null)
            } else {
                updatePluginStatus(
                    item.id,
                    PluginStatus.ERROR,
                    errorMsg = result.exceptionOrNull()?.message ?: "Uninstall failed"
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
        _uiState.value = _uiState.value.copy(plugins = updated)
    }
}
