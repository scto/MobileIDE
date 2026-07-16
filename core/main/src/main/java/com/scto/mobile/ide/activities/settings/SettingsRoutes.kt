/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.scto.mobile.ide.activities.settings

sealed class SettingsRoutes(val route: String) {
    data object Settings : SettingsRoutes("settings")

    data object AppSettings : SettingsRoutes("app_settings")

    data object EditorSettings : SettingsRoutes("editor_settings")

    data object Keybindings : SettingsRoutes("keybindings")

    data object TerminalSettings : SettingsRoutes("terminal_settings")

    data object TerminalExtraKeys : SettingsRoutes("terminal_extra_keys")

    data object TerminalCheck : SettingsRoutes("terminal_check")

    data object About : SettingsRoutes("about")

    data object EditorFontScreen : SettingsRoutes("editor_font_screen")

    data object AppFontScreen : SettingsRoutes("app_font_screen")

    data object TerminalFontScreen : SettingsRoutes("terminal_font_screen")

    data object DefaultEncoding : SettingsRoutes("default_encoding")

    data object DefaultLineEnding : SettingsRoutes("default_line_ending")

    data object ToolbarActions : SettingsRoutes("toolbar_actions")

    data object ExtraKeys : SettingsRoutes("extra_keys")

    data object ExcludeFiles : SettingsRoutes("exclude_files")

    data object Extensions : SettingsRoutes("extensions")

    data object ExtensionDetail : SettingsRoutes("extension_detail")

    data object ExtensionSettings : SettingsRoutes("extension_settings")

    data object DeveloperOptions : SettingsRoutes("developer_options")

    data object AppLogs : SettingsRoutes("app_logs")

    data object Support : SettingsRoutes("support")

    data object Formatters : SettingsRoutes("formatters")

    data object LanguageScreen : SettingsRoutes("language")

    data object Runners : SettingsRoutes("runners")

    data object HtmlRunner : SettingsRoutes("html_preview")

    data object Themes : SettingsRoutes("theme")

    data object LspSettings : SettingsRoutes("lsp_settings")

    data object LspServerDetail : SettingsRoutes("lsp_server_detail")

    data object LspServerLogs : SettingsRoutes("lsp_server_logs")

    data object Git : SettingsRoutes("git")
}
