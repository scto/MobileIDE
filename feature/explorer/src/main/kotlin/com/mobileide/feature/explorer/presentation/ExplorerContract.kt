package com.mobileide.explorer.presentation

// FileItem Model aus einem früheren Schritt
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val lastModified: Long
)

data class ExplorerState(
    val currentPath: String = "/storage/emulated/0/", // Startverzeichnis
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed interface ExplorerEvent {
    data class OpenDirectory(val path: String) : ExplorerEvent
    data class OpenFile(val path: String) : ExplorerEvent
    object NavigateUp : ExplorerEvent
}
