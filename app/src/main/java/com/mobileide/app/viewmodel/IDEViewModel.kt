package com.mobileide.app.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobileide.app.data.*
import com.mobileide.app.ui.components.CursorPos
import com.mobileide.app.ui.theme.ActiveTheme
import com.mobileide.app.ui.theme.Themes
import com.mobileide.app.utils.*
import com.mobileide.app.utils.SessionManager
import com.mobileide.app.ui.theme.ThemePreferences
import com.mobileide.app.ui.theme.amoledM3Theme
import com.mobileide.app.ui.theme.currentM3Theme
import com.mobileide.app.ui.theme.dynamicM3Theme
import com.mobileide.app.ui.theme.m3Themes
import com.mobileide.app.editor.EditorThemeManager
import com.mobileide.app.editor.LanguageManager
import com.mobileide.app.utils.commands.Command
import com.mobileide.app.utils.commands.CommandRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import java.lang.ref.WeakReference
import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LogTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import com.mobileide.feature.settings.app.EditorPreference
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Central ViewModel for the MobileIDE application.
 *
 * Holds all reactive state via [kotlinx.coroutines.flow.StateFlow] and coordinates
 * between the UI layer, the editor engine, the file system and Termux.
 *
 * ## Navigation
 * The current screen is exposed as [currentScreen]. Navigate with [navigate].
 *
 * ## Architecture diagram
 * ```mermaid
 * graph LR
 *     MainActivity --> IDEViewModel
 *     IDEViewModel --> WorkspaceManager
 *     IDEViewModel --> GitManager
 *     IDEViewModel --> TermuxBridge
 *     IDEViewModel --> ProjectManager
 * ```
 *
 * @see Screen
 * @see com.mobileide.app.utils.WorkspaceManager
 */
class IDEViewModel(application: Application) : AndroidViewModel(application) {

    val termux    = TermuxBridge(application)
    val git       = GitManager(termux)
    val workspace = WorkspaceManager(application)

    // ── Navigation ────────────────────────────────────────────────────────────
    private val _screen = MutableStateFlow(Screen.ONBOARDING)
    val currentScreen: StateFlow<Screen> = _screen
    fun navigate(screen: Screen) {
        Logger.info(LogTag.VIEW_MODEL, "navigate → ${screen.name}")
        _screen.value = screen
    }

    // ── Onboarding ────────────────────────────────────────────────────────────
    private val _onboardingComplete = MutableStateFlow(false)
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete

    fun completeOnboarding() {
        _onboardingComplete.value = true
        viewModelScope.launch { workspace.setOnboardingComplete() }
        navigate(Screen.HOME)
    }

    fun saveSession(projectPath: String, tabs: List<com.mobileide.app.data.EditorTab>, activeIdx: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = SessionManager.EditorSession(
                allFiles = tabs.map { SessionManager.FileSession(it.file.absolutePath) },
                selectedFile = tabs.getOrNull(activeIdx)?.file?.absolutePath ?: ""
            )
            SessionManager.save(projectPath, session)
        }
    }

    fun savePermissions(perms: com.mobileide.app.utils.PermissionState) {
        viewModelScope.launch { workspace.savePermissions(perms) }
    }

    // ── Projects ──────────────────────────────────────────────────────────────
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject

    private val _showNewProjectDialog = MutableStateFlow(false)
    val showNewProjectDialog: StateFlow<Boolean> = _showNewProjectDialog

    // ── Editor Tabs ───────────────────────────────────────────────────────────
    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs: StateFlow<List<EditorTab>> = _openTabs

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex

    val activeTab: StateFlow<EditorTab?> = combine(_openTabs, _activeTabIndex) { tabs, idx ->
        tabs.getOrNull(idx)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Split editor
    private val _splitEnabled  = MutableStateFlow(false)
    val splitEnabled: StateFlow<Boolean> = _splitEnabled
    private val _splitTabIndex = MutableStateFlow<Int?>(null)
    val splitTabIndex: StateFlow<Int?> = _splitTabIndex
    val splitTab: StateFlow<EditorTab?> = combine(_openTabs, _splitTabIndex) { tabs, idx ->
        idx?.let { tabs.getOrNull(it) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── Cursor position (reported by SoraEditor) ──────────────────────────────
    private val _cursorPos = MutableStateFlow(CursorPos(1, 1))
    val cursorPos: StateFlow<CursorPos> = _cursorPos
    fun updateCursorPos(pos: CursorPos) { _cursorPos.value = pos }

    // ── File Tree ─────────────────────────────────────────────────────────────
    private val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    val fileTree: StateFlow<List<FileNode>> = _fileTree

    // ── Terminal ──────────────────────────────────────────────────────────────
    private val _terminalLines = MutableStateFlow(listOf(
        TerminalLine("MobileIDE v5.0 – Sora Editor Edition", LineType.INFO),
        TerminalLine("Termux: ${if (TermuxBridge(application).isTermuxInstalled()) "✓ installed" else "✗ not found"}", LineType.INFO),
        TerminalLine("", LineType.OUTPUT)
    ))
    val terminalLines: StateFlow<List<TerminalLine>> = _terminalLines

    private val _commandHistory = MutableStateFlow<List<String>>(emptyList())
    val commandHistory: StateFlow<List<String>> = _commandHistory

    private val _isBuilding = MutableStateFlow(false)
    val isBuilding: StateFlow<Boolean> = _isBuilding

    private val _builtApkPath = MutableStateFlow<String?>(null)
    val builtApkPath: StateFlow<String?> = _builtApkPath

    // ── Build errors ──────────────────────────────────────────────────────────
    val buildErrors: StateFlow<List<BuildError>> = _terminalLines.map { lines ->
        com.mobileide.app.ui.components.BuildErrorParser.parse(lines)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Snackbar ──────────────────────────────────────────────────────────────
    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbar
    fun showSnackbar(msg: String) { _snackbar.value = msg }
    fun clearSnackbar()           { _snackbar.value = null }

    // ── Command Palette ───────────────────────────────────────────────────────
    private val _cpVisible      = MutableStateFlow(false)
    private val _cpCommands     = MutableStateFlow<List<Command>>(emptyList())
    private val _cpLastUsed     = MutableStateFlow<Command?>(null)
    private val _cpChildren     = MutableStateFlow<List<Command>?>(null)
    private val _cpPlaceholder  = MutableStateFlow<String?>(null)

    val commandPaletteVisible:             StateFlow<Boolean>       = _cpVisible
    val commandPaletteCommands:            StateFlow<List<Command>> = _cpCommands
    val commandPaletteLastUsed:            StateFlow<Command?>      = _cpLastUsed
    val commandPaletteInitialChildren:     StateFlow<List<Command>?> = _cpChildren
    val commandPaletteInitialPlaceholder:  StateFlow<String?>       = _cpPlaceholder

    /** Weak reference to the active Sora editor — set by SoraCodeEditor. */
    var activeEditorRef: WeakReference<com.mobileide.app.editor.Editor>? = null

    fun showCommandPalette(placeholder: String?, children: List<Command>?) {
        _cpChildren.value    = children
        _cpPlaceholder.value = placeholder
        _cpCommands.value    = CommandRegistry.allCommands
        _cpVisible.value     = true
    }


    // ── Editor preference ─────────────────────────────────────────────────────
    private val _preferredEditor = MutableStateFlow(EditorPreference.MOBILEIDE)
    val preferredEditor: StateFlow<EditorPreference> = _preferredEditor

    fun setPreferredEditor(pref: EditorPreference) { _preferredEditor.value = pref }

    fun dismissCommandPalette() {
        _cpVisible.value = false
    }

    /** Trigger jump-to-line dialog — used by JumpToLineCommand. */
    private val _showJumpToLine = MutableStateFlow(false)
    val showJumpToLine: StateFlow<Boolean> = _showJumpToLine

    fun showJumpToLine() { _showJumpToLine.value = true }
    fun dismissJumpToLine() { _showJumpToLine.value = false }


    // ── Settings ──────────────────────────────────────────────────────────────
    private val _fontSize      = MutableStateFlow(14f)
    val fontSize: StateFlow<Float> = _fontSize

    private val _autoSave      = MutableStateFlow(true)
    val autoSave: StateFlow<Boolean> = _autoSave

    private val _lineNumbers   = MutableStateFlow(true)
    val lineNumbers: StateFlow<Boolean> = _lineNumbers

    private val _wordWrap      = MutableStateFlow(false)
    val wordWrap: StateFlow<Boolean> = _wordWrap

    private val _tabSize       = MutableStateFlow(4)
    val tabSize: StateFlow<Int> = _tabSize

    private val _themeName     = MutableStateFlow("Catppuccin Mocha")
    val currentThemeName: StateFlow<String> = _themeName

    // Editor-specific settings (passed to SoraEditor)
    private val _editorSettings = MutableStateFlow(EditorSettings())
    val editorSettings: StateFlow<EditorSettings> = _editorSettings

    // ── Recent files ──────────────────────────────────────────────────────────
    private val _recentFiles    = MutableStateFlow<List<String>>(emptyList())
    val recentFiles: StateFlow<List<String>> = _recentFiles

    private val _recentProjects = MutableStateFlow<List<String>>(emptyList())
    val recentProjects: StateFlow<List<String>> = _recentProjects

    // ── Code Outline ──────────────────────────────────────────────────────────
    val codeOutline: StateFlow<List<OutlineSymbol>> = activeTab.map { tab ->
        if (tab == null || tab.language !in listOf(Language.KOTLIN, Language.JAVA, Language.GRADLE))
            emptyList()
        else
            SyntaxHighlighter.extractOutline(tab.content)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── TODOs ─────────────────────────────────────────────────────────────────
    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos

    // ─────────────────────────────────────────────────────────────────────────
    init {
        Logger.info(LogTag.VIEW_MODEL, "IDEViewModel created")
        loadProjects()
        viewModelScope.launch {
            val ws = workspace.load()
            _onboardingComplete.value = ws.onboardingComplete
            _fontSize.value    = ws.fontSize
            _autoSave.value    = ws.autoSave
            _lineNumbers.value = ws.lineNumbers
            _wordWrap.value    = ws.wordWrap
            _tabSize.value     = ws.tabSize
            _recentFiles.value    = ws.recentFiles
            _recentProjects.value = ws.recentProjects
            _themeName.value      = ws.themeName
            ActiveTheme.set(Themes.byName(ws.themeName))
            _editorSettings.value = EditorSettings(
                fontSize             = ws.editorFontSize,
                tabSize              = ws.tabSize,
                showLineNumbers      = ws.lineNumbers,
                wordWrap             = ws.wordWrap,
                autoComplete         = ws.editorAutoComplete,
                bracketAutoClose     = ws.editorBracketAutoClose,
                autoIndent           = ws.editorAutoIndent,
                stickyScroll         = ws.editorStickyScroll,
                highlightCurrentLine = ws.editorHighlightLine,
                fontPath             = ws.editorFontPath,
                lineSpacing          = ws.editorLineSpacing,
                deleteMultiSpaces    = ws.editorDeleteMultiSpaces,
                cursorAnimation      = ws.editorCursorAnimation,
                showWhitespace       = ws.editorShowWhitespace,
                autoCloseTag         = ws.editorAutoCloseTag,
                bulletContinuation   = ws.editorBulletContinuation,
                autoSave             = ws.editorAutoSave,
                formatOnSave         = ws.editorFormatOnSave,
            )
            // Navigate: show onboarding if not complete OR required permissions missing
            val requiredOk = ws.permissions.allGranted
            _screen.value = when {
                !ws.onboardingComplete -> Screen.ONBOARDING
                !requiredOk           -> Screen.ONBOARDING  // re-show if permissions revoked
                else                  -> Screen.HOME
            }
        }
    }

    // ── Projects ──────────────────────────────────────────────────────────────
    fun loadProjects() {
        Logger.info(LogTag.VIEW_MODEL, "loadProjects")
        viewModelScope.launch(Dispatchers.IO) { _projects.value = ProjectManager.listProjects() }
    }

    fun openProject(project: Project) {
        Logger.info(LogTag.VIEW_MODEL, "openProject: ${project.name}")
        _currentProject.value = project
        _openTabs.value = emptyList()
        navigate(Screen.EDITOR)
        viewModelScope.launch(Dispatchers.IO) {
            buildFileTree(File(project.path))
            workspace.saveSession(project.path, emptyList(), 0)
        }
    }

    fun createProject(
        name: String,
        packageName: String,
        minSdk: Int         = 26,
        targetSdk: Int      = 35,
        templateId: String  = "empty_compose"
    ) {
        Logger.info(LogTag.VIEW_MODEL, "createProject: $name", "pkg=$packageName min=$minSdk")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val p = ProjectManager.createProject(name, packageName, minSdk, targetSdk, templateId)
                loadProjects()
                withContext(Dispatchers.Main) {
                    openProject(p)
                    showSnackbar("Project '${p.name}' created!")
                }
            } catch (e: Exception) {
                Logger.error(LogTag.VIEW_MODEL, "createProject failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    showSnackbar("Error: ${e.message ?: "Could not create project"}")
                }
            }
        }
    }

    fun deleteProject(project: Project) {
        Logger.warn(LogTag.VIEW_MODEL, "deleteProject: ${project.name}")
        viewModelScope.launch(Dispatchers.IO) { ProjectManager.deleteProject(project); loadProjects() }
    }

    fun showNewProjectDialog(show: Boolean) { _showNewProjectDialog.value = show }

    // ── File Tree ─────────────────────────────────────────────────────────────
    fun buildFileTree(root: File, depth: Int = 0): List<FileNode> {
        val nodes = mutableListOf<FileNode>()
        try {
            root.listFiles()
                ?.filter {
                    !it.name.startsWith(".") &&
                    it.name != "build" &&
                    it.name != ".gradle" &&
                    it.name != ".idea" &&
                    it.name != "captures"
                }
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?.forEach { file ->
                    val exp = _fileTree.value.firstOrNull {
                        it.file.absolutePath == file.absolutePath
                    }?.isExpanded ?: false
                    nodes += FileNode(file, depth, exp)
                    if (file.isDirectory && exp) nodes += buildFileTree(file, depth + 1)
                }
        } catch (_: Exception) {}
        if (depth == 0) _fileTree.value = nodes
        return nodes
    }

    /** Rebuild file tree on IO thread — safe to call from anywhere. */
    fun refreshFileTree() {
        val root = _currentProject.value?.path?.let { java.io.File(it) } ?: return
        viewModelScope.launch(Dispatchers.IO) { buildFileTree(root) }
    }

    fun toggleFolder(node: FileNode) {
        val list = _fileTree.value.toMutableList()
        val i    = list.indexOfFirst { it.file.absolutePath == node.file.absolutePath }
        if (i != -1) {
            list[i] = list[i].copy(isExpanded = !list[i].isExpanded)
            _fileTree.value = list
            _currentProject.value?.let { buildFileTree(File(it.path)) }
        }
    }

    // ── File Operations ───────────────────────────────────────────────────────
    fun createFile(parentDir: File, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val f = File(parentDir, name)
            if (!f.exists()) { f.createNewFile(); openFile(f) }
            _currentProject.value?.let { buildFileTree(File(it.path)) }
            showSnackbar("Created: $name")
        }
    }

    fun createFolder(parentDir: File, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            File(parentDir, name).mkdirs()
            _currentProject.value?.let { buildFileTree(File(it.path)) }
        }
    }

    fun renameFile(file: File, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newFile = File(file.parent, newName)
            if (file.renameTo(newFile)) {
                val tabs = _openTabs.value.toMutableList()
                val i = tabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
                if (i != -1) tabs[i] = tabs[i].copy(file = newFile)
                _openTabs.value = tabs
                _currentProject.value?.let { buildFileTree(File(it.path)) }
            }
        }
    }

    fun deleteFile(file: File) {
        Logger.warn(LogTag.FILE_OPS, "deleteFile: ${file.name}")
        viewModelScope.launch(Dispatchers.IO) {
            val tabs = _openTabs.value.toMutableList()
            tabs.removeAll { it.file.absolutePath == file.absolutePath }
            _openTabs.value = tabs
            if (file.isDirectory) file.deleteRecursively() else file.delete()
            _currentProject.value?.let { buildFileTree(File(it.path)) }
        }
    }

    fun copyPathToClipboard(file: File) {
        val cb = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("path", file.absolutePath))
        showSnackbar("Path copied")
    }

    // ── Editor Tabs ───────────────────────────────────────────────────────────
    fun openFile(file: File) {
        Logger.info(LogTag.FILE_OPS, "openFile: ${file.name}")
        val existing = _openTabs.value.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existing != -1) { _activeTabIndex.value = existing; return }
        viewModelScope.launch(Dispatchers.IO) {
            val content = try { file.readText() } catch (e: Exception) { "// Error: ${e.message}" }
            withContext(Dispatchers.Main) {
                _openTabs.value = _openTabs.value + EditorTab(file, content)
                _activeTabIndex.value = _openTabs.value.lastIndex
            }
            workspace.addRecentFile(file.absolutePath)
            _recentFiles.value = (listOf(file.absolutePath) +
                _recentFiles.value.filter { it != file.absolutePath }).take(20)
        }
    }

    fun closeTab(index: Int) {
        val tabs = _openTabs.value.toMutableList()
        if (index < 0 || index >= tabs.size) return
        tabs.removeAt(index)
        _openTabs.value = tabs
        _activeTabIndex.value = minOf(_activeTabIndex.value, maxOf(0, tabs.lastIndex))
        if (_splitTabIndex.value == index) { _splitTabIndex.value = null; _splitEnabled.value = false }
    }

    fun closeOtherTabs(keepIndex: Int) {
        val tabs = _openTabs.value
        val keep = tabs.getOrNull(keepIndex) ?: return
        _openTabs.value = listOf(keep)
        _activeTabIndex.value = 0
        _splitTabIndex.value = null; _splitEnabled.value = false
    }

    fun closeAllTabs() {
        _openTabs.value = emptyList()
        _activeTabIndex.value = 0
        _splitTabIndex.value = null; _splitEnabled.value = false
    }

    fun selectTab(index: Int) { _activeTabIndex.value = index }

    fun toggleSplit() {
        if (_splitEnabled.value) {
            _splitEnabled.value = false; _splitTabIndex.value = null
        } else if (_openTabs.value.size > 1) {
            _splitEnabled.value = true
            _splitTabIndex.value = _openTabs.value.indices.firstOrNull { it != _activeTabIndex.value } ?: 0
        }
    }

    fun updateTabContent(index: Int, content: String) {
        val tabs = _openTabs.value.toMutableList()
        if (index < 0 || index >= tabs.size) return
        tabs[index] = tabs[index].copy(content = content, isModified = !_autoSave.value)
        _openTabs.value = tabs
        if (_autoSave.value) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    tabs[index].file.writeText(content)
                    val updated = _openTabs.value.toMutableList()
                    if (index < updated.size) updated[index] = updated[index].copy(isModified = false)
                    withContext(Dispatchers.Main) { _openTabs.value = updated }
                } catch (_: Exception) {}
            }
        }
    }

    fun saveCurrentFile() {
        val tab = activeTab.value ?: return
        Logger.info(LogTag.FILE_OPS, "saveCurrentFile: ${tab.file.name}")
        val idx = _activeTabIndex.value
        viewModelScope.launch(Dispatchers.IO) {
            tab.file.writeText(tab.content)
            val tabs = _openTabs.value.toMutableList()
            if (idx < tabs.size) tabs[idx] = tabs[idx].copy(isModified = false)
            withContext(Dispatchers.Main) { _openTabs.value = tabs }
            showSnackbar("Saved: ${tab.file.name}")
        }
    }

    fun saveAllFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            var count = 0
            val tabs = _openTabs.value.toMutableList()
            tabs.forEachIndexed { i, tab ->
                if (tab.isModified) { tab.file.writeText(tab.content); tabs[i] = tab.copy(isModified = false); count++ }
            }
            withContext(Dispatchers.Main) { _openTabs.value = tabs }
            if (count > 0) showSnackbar("Saved $count file${if (count > 1) "s" else ""}")
        }
    }

    fun formatCurrentFile() {
        Logger.info(LogTag.FORMATTER, "formatCurrentFile: ${activeTab.value?.name ?: "none"}")
        val tab = activeTab.value ?: return
        val idx = _activeTabIndex.value
        updateTabContent(idx, CodeFormatter.format(tab.content, tab.language, _tabSize.value))
        showSnackbar("Formatted: ${tab.file.name}")
    }

    fun organizeImports() {
        val tab = activeTab.value ?: return
        updateTabContent(_activeTabIndex.value, CodeFormatter.organizeImports(tab.content))
        showSnackbar("Imports organized")
    }

    fun scanTodos() {
        Logger.info(LogTag.SCREEN_TODO, "scanTodos: ${_currentProject.value?.name ?: "none"}")
        val root = _currentProject.value?.path ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _todos.value = CodeFormatter.findTodos(File(root))
        }
    }

    // ── Terminal ──────────────────────────────────────────────────────────────
    fun addTerminalLine(line: TerminalLine) {
        _terminalLines.value = (_terminalLines.value + line).takeLast(5000)
    }

    fun clearTerminal() {
        _terminalLines.value = listOf(TerminalLine("Terminal cleared.", LineType.INFO))
    }

    fun runCommand(command: String) {
        Logger.info(LogTag.TERMUX, "runCommand: $command")
        if (command.isBlank()) return
        _commandHistory.value = (listOf(command) + _commandHistory.value.filter { it != command }).take(50)
        viewModelScope.launch {
            termux.executeStream(command).collect { addTerminalLine(it) }
        }
    }

    // ── Git ───────────────────────────────────────────────────────────────────
    fun runGitCommand(projectPath: String, action: suspend (GitManager) -> Flow<TerminalLine>) {
        navigate(Screen.TERMINAL)
        viewModelScope.launch { action(git).collect { addTerminalLine(it) } }
    }

    // ── Build ─────────────────────────────────────────────────────────────────
    fun buildProject() {
        Logger.info(LogTag.BUILD, "buildProject: ${_currentProject.value?.name ?: "none"}")
        val project = _currentProject.value ?: return
        if (_isBuilding.value) return
        _isBuilding.value = true; _builtApkPath.value = null
        saveAllFiles()
        navigate(Screen.TERMINAL)
        addTerminalLine(TerminalLine("══════════════════════════", LineType.INFO))
        addTerminalLine(TerminalLine("  🔨 Gradle Build…", LineType.INFO))
        addTerminalLine(TerminalLine("══════════════════════════", LineType.INFO))
        viewModelScope.launch {
            termux.gradleBuild(project.path).collect { line ->
                addTerminalLine(line)
                Regex("""(/[^\s]+\.apk)""").find(line.text)?.let { _builtApkPath.value = it.value }
            }
            _isBuilding.value = false
            withContext(Dispatchers.IO) {
                File(project.path, "app/build/outputs/apk/debug")
                    .listFiles()?.firstOrNull { it.extension == "apk" }
                    ?.let { _builtApkPath.value = it.absolutePath }
            }
        }
    }

    fun installApk() {
        Logger.info(LogTag.INSTALL, "installApk: ${_builtApkPath.value}")
        val apk = _builtApkPath.value ?: return
        navigate(Screen.TERMINAL)
        viewModelScope.launch { termux.installApk(apk).collect { addTerminalLine(it) } }
    }

    fun cleanProject() {
        Logger.info(LogTag.BUILD, "cleanProject: ${_currentProject.value?.name ?: "none"}")
        val project = _currentProject.value ?: return
        navigate(Screen.TERMINAL)
        viewModelScope.launch { termux.gradleClean(project.path).collect { addTerminalLine(it) } }
    }

    fun checkEnvironment() {
        navigate(Screen.TERMINAL)
        viewModelScope.launch {
            addTerminalLine(TerminalLine("=== Environment Check ===", LineType.INFO))
            termux.checkJava().collect { addTerminalLine(it) }
            termux.checkGradle().collect { addTerminalLine(it) }
            termux.checkGit().collect { addTerminalLine(it) }
            termux.checkSdk().collect { addTerminalLine(it) }
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────────
    fun setTheme(name: String) {
        Logger.info(LogTag.SCHEME, "setTheme: $name")
        _themeName.value = name
        ActiveTheme.set(Themes.byName(name))
        EditorThemeManager.invalidate()       // invalidate new editor theme cache
        LanguageManager.invalidateCache()     // language cache (token colours changed)
        viewModelScope.launch { workspace.saveTheme(name) }
    }

    fun saveSettings(fontSize: Float, autoSave: Boolean, lineNumbers: Boolean, wordWrap: Boolean, tabSize: Int) {
        _fontSize.value = fontSize; _autoSave.value = autoSave
        _lineNumbers.value = lineNumbers; _wordWrap.value = wordWrap; _tabSize.value = tabSize
        viewModelScope.launch { workspace.saveSettings(fontSize, autoSave, lineNumbers, wordWrap, tabSize) }
        showSnackbar("Settings saved")
    }

    fun saveEditorSettings(settings: EditorSettings) {
        Logger.info(LogTag.SCREEN_DEV, "saveEditorSettings fontSize=${settings.fontSize}")
        _editorSettings.value = settings
        viewModelScope.launch { workspace.saveEditorSettings(settings) }
        showSnackbar("Editor settings saved")
    }

    // ── Material3 Theme ───────────────────────────────────────────────────────
    fun setM3Theme(themeId: String) {
        Logger.info(LogTag.SCHEME, "setM3Theme: $themeId")
        val holder = m3Themes.find { it.id == themeId } ?: return
        currentM3Theme.value = holder
        ThemePreferences.themeId = themeId
        EditorThemeManager.invalidate()
        LanguageManager.invalidateCache()
    }

    fun setMonet(enabled: Boolean) {
        dynamicM3Theme.value = enabled
        ThemePreferences.monet = enabled
    }

    fun setAmoled(enabled: Boolean) {
        amoledM3Theme.value = enabled
        ThemePreferences.amoled = enabled
    }
}

enum class Screen {
    ONBOARDING, HOME,
    EDITOR, TERMINAL, SETTINGS, EDITOR_SETTINGS, DEV_SETTINGS,
    GIT, LOGCAT, DEPENDENCIES, RUN_CONFIG,
    PROJECT_SEARCH, GRADLE_TASKS, DIFF_VIEWER, PROJECT_STATS,
    TODO_PANEL, PACKAGE_MANAGER, KEYBOARD_HELP, SETUP_WIZARD,
    LOG_VIEWER, APP_THEME,
    // ── Settings sub-screens ──────────────────────────────────────────────────
    SETTINGS_EDITOR, SETTINGS_THEME, SETTINGS_KEYBINDS,
    SETTINGS_LANGUAGE, SETTINGS_LSP, SETTINGS_RUNNERS,
    SETTINGS_GIT, SETTINGS_TERMINAL, SETTINGS_EXTENSION,
    SETTINGS_DEBUG, SETTINGS_SUPPORT, SETTINGS_ABOUT,
    SETTINGS_EDITOR_SELECT,  // choose between editors
    FEATURE_EDITOR,          // :feature:editor standalone screen
}
