package com.mobileide.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LogTag
import kotlinx.coroutines.flow.*
import org.json.JSONArray

private val Context.workspaceStore: DataStore<Preferences> by preferencesDataStore(name = "workspace_v2")

// ── Per-permission grant state ────────────────────────────────────────────────
data class PermissionState(
    val storage:          Boolean = false,   // MANAGE_EXTERNAL_STORAGE
    val notifications:    Boolean = false,   // POST_NOTIFICATIONS
    val battery:          Boolean = false,   // REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    val usageStats:       Boolean = false,   // PACKAGE_USAGE_STATS
    val installPackages:  Boolean = false,   // REQUEST_INSTALL_PACKAGES
    val termuxRun:        Boolean = false,   // com.termux.permission.RUN_COMMAND
) {
    /** All required permissions are granted. */
    val allGranted: Boolean get() =
        storage && notifications && battery && installPackages
        // usageStats + termuxRun are nice-to-have but not blocking
}

data class WorkspaceState(
    val lastProjectPath: String           = "",
    val openFilePaths: List<String>       = emptyList(),
    val activeFileIndex: Int              = 0,
    val recentFiles: List<String>         = emptyList(),
    val recentProjects: List<String>      = emptyList(),
    val themeName: String                 = "Catppuccin Mocha",
    val fontSize: Float                   = 14f,
    val autoSave: Boolean                 = true,
    val lineNumbers: Boolean              = true,
    val wordWrap: Boolean                 = false,
    val tabSize: Int                      = 4,
    val editorFontSize: Float             = 14f,
    val editorAutoComplete: Boolean       = true,
    val editorBracketAutoClose: Boolean   = true,
    val editorAutoIndent: Boolean         = true,
    val editorStickyScroll: Boolean       = false,
    val editorHighlightLine: Boolean      = true,
    val editorFontPath: String            = "fonts/JetBrainsMono-Regular.ttf",
    val editorLineSpacing: Float          = 1.2f,
    val editorDeleteMultiSpaces: Boolean  = true,
    val editorCursorAnimation: Boolean    = true,
    val editorShowWhitespace: Boolean     = false,
    val editorAutoCloseTag: Boolean       = true,
    val editorBulletContinuation: Boolean = true,
    val editorAutoSave: Boolean           = true,
    val editorFormatOnSave: Boolean       = false,
    // Onboarding — only true when ALL required permissions are granted
    val onboardingComplete: Boolean       = false,
    // Per-permission saved state
    val permissions: PermissionState      = PermissionState()
)

class WorkspaceManager(private val context: Context) {
    companion object {
        private val K_LAST_PROJECT    = stringPreferencesKey("last_project")
        private val K_OPEN_FILES      = stringPreferencesKey("open_files")
        private val K_ACTIVE_INDEX    = intPreferencesKey("active_index")
        private val K_RECENT_FILES    = stringPreferencesKey("recent_files")
        private val K_RECENT_PROJECTS = stringPreferencesKey("recent_projects")
        private val K_THEME           = stringPreferencesKey("theme")
        private val K_FONT_SIZE       = floatPreferencesKey("font_size")
        private val K_AUTO_SAVE       = booleanPreferencesKey("auto_save")
        private val K_LINE_NUMBERS    = booleanPreferencesKey("line_numbers")
        private val K_WORD_WRAP       = booleanPreferencesKey("word_wrap")
        private val K_TAB_SIZE        = intPreferencesKey("tab_size")
        private val K_ED_FONT         = floatPreferencesKey("ed_font")
        private val K_ED_AUTOCOMPLETE = booleanPreferencesKey("ed_autocomplete")
        private val K_ED_BRACKET      = booleanPreferencesKey("ed_bracket")
        private val K_ED_INDENT       = booleanPreferencesKey("ed_indent")
        private val K_ED_STICKY       = booleanPreferencesKey("ed_sticky")
        private val K_ED_HIGHLIGHT    = booleanPreferencesKey("ed_highlight")
        private val K_ED_FONT_PATH      = stringPreferencesKey("ed_font_path")
        private val K_ED_LINE_SPACING   = floatPreferencesKey("ed_line_spacing")
        private val K_ED_DELETE_MULTI   = booleanPreferencesKey("ed_delete_multi")
        private val K_ED_CURSOR_ANIM    = booleanPreferencesKey("ed_cursor_anim")
        private val K_ED_WHITESPACE     = booleanPreferencesKey("ed_whitespace")
        private val K_ED_AUTO_CLOSE_TAG = booleanPreferencesKey("ed_auto_close_tag")
        private val K_ED_BULLET_CONT    = booleanPreferencesKey("ed_bullet_cont")
        private val K_ED_AUTO_SAVE      = booleanPreferencesKey("ed_auto_save")
        private val K_ED_FORMAT_SAVE    = booleanPreferencesKey("ed_format_save")
        private val K_ONBOARDING      = booleanPreferencesKey("onboarding_done")
        // Per-permission keys
        private val K_PERM_STORAGE    = booleanPreferencesKey("perm_storage")
        private val K_PERM_NOTIF      = booleanPreferencesKey("perm_notifications")
        private val K_PERM_BATTERY    = booleanPreferencesKey("perm_battery")
        private val K_PERM_USAGE      = booleanPreferencesKey("perm_usage_stats")
        private val K_PERM_INSTALL    = booleanPreferencesKey("perm_install_packages")
        private val K_PERM_TERMUX     = booleanPreferencesKey("perm_termux_run")
        private const val MAX_RECENT  = 20
    }

    val workspaceFlow: Flow<WorkspaceState> = context.workspaceStore.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
            val perms = PermissionState(
                storage         = p[K_PERM_STORAGE]  ?: false,
                notifications   = p[K_PERM_NOTIF]    ?: false,
                battery         = p[K_PERM_BATTERY]  ?: false,
                usageStats      = p[K_PERM_USAGE]     ?: false,
                installPackages = p[K_PERM_INSTALL]   ?: false,
                termuxRun       = p[K_PERM_TERMUX]    ?: false,
            )
            WorkspaceState(
                lastProjectPath        = p[K_LAST_PROJECT]    ?: "",
                openFilePaths          = p[K_OPEN_FILES]?.let { decodeList(it) } ?: emptyList(),
                activeFileIndex        = p[K_ACTIVE_INDEX]    ?: 0,
                recentFiles            = p[K_RECENT_FILES]?.let { decodeList(it) } ?: emptyList(),
                recentProjects         = p[K_RECENT_PROJECTS]?.let { decodeList(it) } ?: emptyList(),
                themeName              = p[K_THEME]           ?: "Catppuccin Mocha",
                fontSize               = p[K_FONT_SIZE]       ?: 14f,
                autoSave               = p[K_AUTO_SAVE]       ?: true,
                lineNumbers            = p[K_LINE_NUMBERS]    ?: true,
                wordWrap               = p[K_WORD_WRAP]       ?: false,
                tabSize                = p[K_TAB_SIZE]        ?: 4,
                editorFontSize         = p[K_ED_FONT]         ?: 14f,
                editorAutoComplete     = p[K_ED_AUTOCOMPLETE] ?: true,
                editorBracketAutoClose = p[K_ED_BRACKET]      ?: true,
                editorAutoIndent       = p[K_ED_INDENT]       ?: true,
                editorStickyScroll     = p[K_ED_STICKY]       ?: false,
                editorHighlightLine    = p[K_ED_HIGHLIGHT]    ?: true,
                editorFontPath         = p[K_ED_FONT_PATH]      ?: "fonts/JetBrainsMono-Regular.ttf",
                editorLineSpacing      = p[K_ED_LINE_SPACING]   ?: 1.2f,
                editorDeleteMultiSpaces = p[K_ED_DELETE_MULTI]  ?: true,
                editorCursorAnimation  = p[K_ED_CURSOR_ANIM]    ?: true,
                editorShowWhitespace   = p[K_ED_WHITESPACE]     ?: false,
                editorAutoCloseTag     = p[K_ED_AUTO_CLOSE_TAG] ?: true,
                editorBulletContinuation = p[K_ED_BULLET_CONT]  ?: true,
                editorAutoSave         = p[K_ED_AUTO_SAVE]      ?: true,
                editorFormatOnSave     = p[K_ED_FORMAT_SAVE]    ?: false,
                onboardingComplete     = p[K_ONBOARDING]      ?: false,
                permissions            = perms
            )
        }

    suspend fun savePermissions(perms: PermissionState) {
        Logger.info(LogTag.WORKSPACE, "savePermissions: $perms")
        context.workspaceStore.edit { p ->
            p[K_PERM_STORAGE]  = perms.storage
            p[K_PERM_NOTIF]    = perms.notifications
            p[K_PERM_BATTERY]  = perms.battery
            p[K_PERM_USAGE]    = perms.usageStats
            p[K_PERM_INSTALL]  = perms.installPackages
            p[K_PERM_TERMUX]   = perms.termuxRun
        }
    }

    suspend fun saveSession(projectPath: String, openFiles: List<String>, activeIndex: Int) {
        Logger.info(LogTag.WORKSPACE, "saveSession", projectPath, "${openFiles.size} files open")
        context.workspaceStore.edit { p ->
            p[K_LAST_PROJECT]  = projectPath
            p[K_OPEN_FILES]    = encodeList(openFiles)
            p[K_ACTIVE_INDEX]  = activeIndex
        }
    }

    suspend fun addRecentFile(path: String) {
        context.workspaceStore.edit { p ->
            val list = (listOf(path) + (p[K_RECENT_FILES]?.let { decodeList(it) } ?: emptyList()))
                .distinct().take(MAX_RECENT)
            p[K_RECENT_FILES] = encodeList(list)
        }
    }

    suspend fun saveTheme(name: String) {
        Logger.info(LogTag.WORKSPACE, "saveTheme: $name")
        context.workspaceStore.edit { it[K_THEME] = name }
    }

    suspend fun saveSettings(
        fontSize: Float, autoSave: Boolean,
        lineNumbers: Boolean, wordWrap: Boolean, tabSize: Int
    ) {
        context.workspaceStore.edit { p ->
            p[K_FONT_SIZE]    = fontSize
            p[K_AUTO_SAVE]    = autoSave
            p[K_LINE_NUMBERS] = lineNumbers
            p[K_WORD_WRAP]    = wordWrap
            p[K_TAB_SIZE]     = tabSize
        }
    }

    suspend fun saveEditorSettings(settings: EditorSettings) {
        context.workspaceStore.edit { p ->
            p[K_ED_FONT]         = settings.fontSize
            p[K_ED_AUTOCOMPLETE] = settings.autoComplete
            p[K_ED_BRACKET]      = settings.bracketAutoClose
            p[K_ED_INDENT]       = settings.autoIndent
            p[K_ED_STICKY]       = settings.stickyScroll
            p[K_ED_HIGHLIGHT]    = settings.highlightCurrentLine
            p[K_ED_FONT_PATH]      = settings.fontPath
            p[K_ED_LINE_SPACING]   = settings.lineSpacing
            p[K_ED_DELETE_MULTI]   = settings.deleteMultiSpaces
            p[K_ED_CURSOR_ANIM]    = settings.cursorAnimation
            p[K_ED_WHITESPACE]     = settings.showWhitespace
            p[K_ED_AUTO_CLOSE_TAG] = settings.autoCloseTag
            p[K_ED_BULLET_CONT]    = settings.bulletContinuation
            p[K_ED_AUTO_SAVE]      = settings.autoSave
            p[K_ED_FORMAT_SAVE]    = settings.formatOnSave
        }
    }

    suspend fun setOnboardingComplete() {
        Logger.success(LogTag.WORKSPACE, "onboarding complete")
        context.workspaceStore.edit { it[K_ONBOARDING] = true }
    }

    suspend fun load(): WorkspaceState = workspaceFlow.first()

    private fun encodeList(list: List<String>): String {
        val arr = JSONArray(); list.forEach { arr.put(it) }; return arr.toString()
    }
    private fun decodeList(json: String): List<String> = try {
        val arr = JSONArray(json); (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) { emptyList() }
}
