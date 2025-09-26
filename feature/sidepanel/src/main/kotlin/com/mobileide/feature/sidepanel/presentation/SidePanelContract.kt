package com.mobileide.sidepanel.presentation

import com.afigaliyev.treeview.TreeViewItem

// Enum for the different views in the side panel
enum class SidePanelView {
    FILE_EXPLORER,
    PROJECT_STRUCTURE,
    GIT_CHANGES
}

// Enum for the different types of items in the tree
enum class ItemType {
    FILE,
    DIRECTORY,
    PROJECT,
    PACKAGE,
    GIT_BRANCH
}

// Enum for the Git status of a file
enum class GitStatus {
    NONE,
    MODIFIED,
    NEW,
    CONFLICT
}

// Data class representing a node in the tree.
// It extends TreeViewItem from the library for compatibility.
data class TreeItem(
    val id: String,
    val name: String,
    val path: String,
    val type: ItemType,
    val level: Int,
    val gitStatus: GitStatus = GitStatus.NONE,
    override val isExpanded: Boolean = false,
    override val children: List<TreeItem> = emptyList()
) : TreeViewItem<TreeItem>

// The UI state for the side panel
data class SidePanelState(
    val currentView: SidePanelView = SidePanelView.FILE_EXPLORER,
    val treeItems: List<TreeItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val projectPackageName: String = "com.mobileide.refactor" // For the project view
)

// Events that can be triggered from the UI
sealed interface SidePanelEvent {
    data class ViewChanged(val view: SidePanelView) : SidePanelEvent
    data class ItemToggled(val itemId: String) : SidePanelEvent
    data class ItemClicked(val item: TreeItem) : SidePanelEvent
    data class PackageNameChangeRequested(val newName: String) : SidePanelEvent
}

