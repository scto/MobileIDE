package com.mobileide.sidepanel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileide.data.FileRepository
import com.mobileide.data.GitRepository
import com.mobileide.data.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SidePanelViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val gitRepository: GitRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SidePanelState())
    val state = _state.asStateFlow()

    private var projectRootPath: String? = null

    init {
        // Beobachtet das aktuell ausgewählte Projekt.
        // Jedes Mal, wenn sich der Pfad ändert, wird die Ansicht aktualisiert.
        projectRepository.getCurrentProject()
            .onEach { path ->
                projectRootPath = path
                if (path != null) {
                    // Lädt die Daten für die aktuell ausgewählte Ansicht (Explorer, Git, etc.)
                    loadDataForCurrentView(path)
                } else {
                    // Setzt den Zustand zurück, wenn kein Projekt geöffnet ist.
                    _state.update { it.copy(isLoading = false, treeItems = emptyList(), error = null) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SidePanelEvent) {
        when (event) {
            is SidePanelEvent.ViewChanged -> {
                if (_state.value.currentView != event.view) {
                    _state.update { it.copy(currentView = event.view) }
                    // Lädt die Daten für die neu ausgewählte Ansicht.
                    projectRootPath?.let { loadDataForCurrentView(it) }
                }
            }
            is SidePanelEvent.ItemToggled -> toggleItem(event.itemId)
            is SidePanelEvent.ItemClicked -> {
                 if (event.item.type != ItemType.FILE) {
                    toggleItem(event.item.id)
                }
                // Die Navigation wird in der UI behandelt.
            }
            is SidePanelEvent.PackageNameChangeRequested -> {
                _state.update { it.copy(projectPackageName = event.newName) }
                if (_state.value.currentView == SidePanelView.PROJECT_STRUCTURE) {
                    projectRootPath?.let { loadDataForCurrentView(it) }
                }
            }
        }
    }

    /**
     * Lädt die Daten basierend auf der aktuellen Ansicht für den gegebenen Projektpfad.
     * Diese Funktion wird jetzt direkt mit dem Pfad aufgerufen, was sie zustandsloser macht.
     */
    private fun loadDataForCurrentView(rootPath: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val items = when (_state.value.currentView) {
                    SidePanelView.FILE_EXPLORER -> createFileExplorerTree(rootPath)
                    SidePanelView.PROJECT_STRUCTURE -> createProjectStructureTree(rootPath)
                    SidePanelView.GIT_CHANGES -> createGitChangesTree(rootPath)
                }
                _state.update { it.copy(isLoading = false, treeItems = items) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Fehler: ${e.message}") }
            }
        }
    }

    private fun createFileExplorerTree(rootPath: String): List<TreeItem> {
        return listOf(
            TreeItem(
                id = rootPath,
                name = rootPath.substringAfterLast('/'),
                path = rootPath,
                type = ItemType.DIRECTORY,
                level = 0,
                isExpanded = false
            )
        )
    }

    private fun toggleItem(itemId: String) {
        viewModelScope.launch {
            val updatedItems = toggleRecursively(_state.value.treeItems, itemId)
            _state.update { it.copy(treeItems = updatedItems) }
        }
    }

    private suspend fun toggleRecursively(items: List<TreeItem>, id: String): List<TreeItem> {
        return items.map { item ->
            if (item.id == id) {
                val isExpanded = !item.isExpanded
                val children = if (isExpanded && item.children.isEmpty() && item.type == ItemType.DIRECTORY) {
                    loadFileExplorerChildren(item.path, item.level + 1)
                } else if (!isExpanded) {
                    emptyList()
                } else {
                    item.children
                }
                item.copy(isExpanded = isExpanded, children = children)
            } else if (item.children.isNotEmpty()) {
                item.copy(children = toggleRecursively(item.children, id))
            } else {
                item
            }
        }
    }

    private suspend fun loadFileExplorerChildren(path: String, level: Int): List<TreeItem> {
        return fileRepository.getFiles(path).getOrDefault(emptyList()).map { fileItem ->
            TreeItem(
                id = fileItem.path,
                name = fileItem.name,
                path = fileItem.path,
                type = if (fileItem.isDirectory) ItemType.DIRECTORY else ItemType.FILE,
                level = level
            )
        }
    }

    private suspend fun createGitChangesTree(rootPath: String): List<TreeItem> {
        val statusResult = gitRepository.getStatus(rootPath).getOrNull() ?: return emptyList()
        val changes = mutableListOf<TreeItem>()

        val staged = (statusResult.added + statusResult.changed).map { path ->
            TreeItem(id = "staged_$path", name = path.substringAfterLast('/'), path = "$rootPath/$path", type = ItemType.FILE, level = 1, gitStatus = GitStatus.MODIFIED)
        }
        if (staged.isNotEmpty()) {
            changes.add(TreeItem("staged_root", "Staged Changes", "", ItemType.DIRECTORY, 0, isExpanded = true, children = staged))
        }

        val unstaged = (statusResult.modified + statusResult.untracked).map { path ->
             TreeItem(id = "unstaged_$path", name = path.substringAfterLast('/'), path = "$rootPath/$path", type = ItemType.FILE, level = 1, gitStatus = GitStatus.MODIFIED)
        }
         if (unstaged.isNotEmpty()) {
            changes.add(TreeItem("unstaged_root", "Changes", "", ItemType.DIRECTORY, 0, isExpanded = true, children = unstaged))
        }

        val conflicting = statusResult.conflicting.map { path ->
            TreeItem(id = "conflict_$path", name = path.substringAfterLast('/'), path = "$rootPath/$path", type = ItemType.FILE, level = 1, gitStatus = GitStatus.CONFLICT)
        }
         if (conflicting.isNotEmpty()) {
            changes.add(TreeItem("conflicting_root", "Conflicting", "", ItemType.DIRECTORY, 0, isExpanded = true, children = conflicting))
        }

        if (changes.isEmpty()) {
            return listOf(TreeItem("no_changes", "Keine Änderungen gefunden.", "", ItemType.FILE, 0))
        }

        return changes
    }

     private fun createProjectStructureTree(rootPath: String): List<TreeItem> {
        return listOf(
            TreeItem("project_root", "MobileIDE (Project)", rootPath, ItemType.PROJECT, 0, isExpanded = true, children = listOf(
                TreeItem("package", _state.value.projectPackageName, "...", ItemType.PACKAGE, 1, isExpanded = true, children = listOf(
                    TreeItem("MainActivity_proj", "MainActivity", "...", ItemType.FILE, 2),
                    TreeItem("ui", "ui", "...", ItemType.PACKAGE, 2, isExpanded = true, children = listOf(
                        TreeItem("MainScreen_proj", "MainScreen", "...", ItemType.FILE, 3, gitStatus = GitStatus.MODIFIED),
                    ))
                )),
                TreeItem("res", "res", "$rootPath/app/src/main/res", ItemType.DIRECTORY, 1),
                TreeItem("build_gradle_proj", "build.gradle.kts (app)", "...", ItemType.FILE, 1),
            ))
        )
    }
}

