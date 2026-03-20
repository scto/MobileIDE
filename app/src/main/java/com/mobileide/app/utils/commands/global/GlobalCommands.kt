package com.mobileide.app.utils.commands.global

import android.view.KeyEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.mobileide.app.ui.icons.AppIconType
import com.mobileide.app.utils.commands.*
import com.mobileide.app.viewmodel.Screen

// ── Open Settings ─────────────────────────────────────────────────────────────
class SettingsCommand(ctx: CommandContext) : GlobalCommand(ctx) {
    override val id = "global.settings"
    override fun getLabel() = "Open Settings"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.Settings)
    override fun action(actionContext: ActionContext) { ctx.vm.navigate(Screen.SETTINGS) }
}

// ── Open Terminal ─────────────────────────────────────────────────────────────
class TerminalCommand(ctx: CommandContext) : GlobalCommand(ctx) {
    override val id = "global.terminal"
    override fun getLabel() = "Open Terminal"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.Terminal)
    override fun action(actionContext: ActionContext) { ctx.vm.navigate(Screen.TERMINAL) }
}

// ── Open Git ──────────────────────────────────────────────────────────────────
class GitCommand(ctx: CommandContext) : GlobalCommand(ctx) {
    override val id = "global.git"
    override fun getLabel() = "Open Git"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.AccountTree)
    override fun isSupported() = ctx.vm.currentProject.value != null
    override fun action(actionContext: ActionContext) { ctx.vm.navigate(Screen.GIT) }
}

// ── Open Home ─────────────────────────────────────────────────────────────────
class HomeCommand(ctx: CommandContext) : GlobalCommand(ctx) {
    override val id = "global.home"
    override fun getLabel() = "Go to Home"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.Home)
    override fun action(actionContext: ActionContext) { ctx.vm.navigate(Screen.HOME) }
}

// ── Project Search ────────────────────────────────────────────────────────────
class ProjectSearchCommand(ctx: CommandContext) : GlobalCommand(ctx) {
    override val id = "global.search_code"
    override fun getLabel() = "Search in Project"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.ManageSearch)
    override val defaultKeybinds = KeyCombination(android.view.KeyEvent.KEYCODE_F, ctrl = true, shift = true)
    override fun isSupported() = ctx.vm.currentProject.value != null
    override fun action(actionContext: ActionContext) { ctx.vm.navigate(Screen.PROJECT_SEARCH) }
}

// ── Gradle Tasks ──────────────────────────────────────────────────────────────
class GradleTasksCommand(ctx: CommandContext) : GlobalCommand(ctx) {
    override val id = "global.gradle_tasks"
    override fun getLabel() = "Gradle Tasks"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.Build)
    override fun isSupported() = ctx.vm.currentProject.value != null
    override fun action(actionContext: ActionContext) { ctx.vm.navigate(Screen.GRADLE_TASKS) }
}

// ── Build Project ─────────────────────────────────────────────────────────────
class BuildProjectCommand(ctx: CommandContext) : GlobalCommand(ctx) {
    override val id = "global.build"
    override fun getLabel() = "Build Project"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.Build)
    override val defaultKeybinds = KeyCombination(android.view.KeyEvent.KEYCODE_B, ctrl = true)
    override fun isSupported() = ctx.vm.currentProject.value != null
    override fun action(actionContext: ActionContext) { ctx.vm.buildProject() }
}

// ── Close Tab ─────────────────────────────────────────────────────────────────
class CloseTabCommand(ctx: CommandContext) : GlobalCommand(ctx) {
    override val id = "global.close_tab"
    override fun getLabel() = "Close Current Tab"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.Close)
    override val defaultKeybinds = KeyCombination(android.view.KeyEvent.KEYCODE_W, ctrl = true)
    override fun isSupported() = ctx.vm.activeTab.value != null
    override fun action(actionContext: ActionContext) {
        ctx.vm.closeTab(ctx.vm.activeTabIndex.value)
    }
}

// ── Editor Settings ───────────────────────────────────────────────────────────
class EditorSettingsCommand(ctx: CommandContext) : GlobalCommand(ctx) {
    override val id = "global.editor_settings"
    override fun getLabel() = "Editor Settings"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.Tune)
    override fun action(actionContext: ActionContext) { ctx.vm.navigate(Screen.EDITOR_SETTINGS) }
}

// ── Command Palette (self-referential) ────────────────────────────────────────
class CommandPaletteCommand(ctx: CommandContext) : GlobalCommand(ctx) {
    override val id = "global.command_palette"
    override fun getLabel() = "Command Palette"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.Search)
    override val defaultKeybinds = KeyCombination(android.view.KeyEvent.KEYCODE_P, ctrl = true, shift = true)
    override fun action(actionContext: ActionContext) { ctx.vm.showCommandPalette(null, null) }
}
