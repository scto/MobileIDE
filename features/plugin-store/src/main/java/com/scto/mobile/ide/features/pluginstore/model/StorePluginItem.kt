package com.scto.mobile.ide.features.pluginstore.model

data class PluginAuthor(
    val displayName: String = "",
    val github: String = ""
)

enum class PluginType {
    LSP,
    THEME,
    FORMATTER,
    TOOL,
    UNKNOWN;

    companion object {
        fun fromString(type: String): PluginType = when (type.lowercase()) {
            "lsp", "language", "language_server" -> LSP
            "theme", "color_theme" -> THEME
            "formatter" -> FORMATTER
            "tool", "debugger" -> TOOL
            else -> UNKNOWN
        }
    }
}

enum class PluginStatus {
    NOT_INSTALLED,
    INSTALLED,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    ERROR
}

data class StorePluginItem(
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val author: PluginAuthor = PluginAuthor(),
    val type: PluginType = PluginType.LSP,
    val downloadUrl: String = "",
    val size: Long = 0L,
    val minAppVersion: Int = 1,
    val tags: List<String> = emptyList(),
    val arch: List<String> = emptyList(),
    val sha256: String? = null,
    val status: PluginStatus = PluginStatus.NOT_INSTALLED,
    val installedVersion: String? = null,
    val downloadProgress: Float = 0f,
    val errorMessage: String? = null
) {
    val sizeFormatted: String
        get() = when {
            size <= 0 -> ""
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        }

    val hasUpdate: Boolean
        get() = status == PluginStatus.UPDATE_AVAILABLE
}
