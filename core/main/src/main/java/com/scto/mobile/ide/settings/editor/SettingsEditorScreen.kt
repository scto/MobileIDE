package com.scto.mobile.ide.settings.editor





import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.scto.mobile.ide.activities.main.MainActivity
import com.scto.mobile.ide.activities.main.ui.fileTreeViewModel
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.activities.settings.settingsNavController
import com.scto.mobile.ide.components.EditorSettingsItem
import com.scto.mobile.ide.components.NextScreenCard
import com.scto.mobile.ide.components.PreferenceList
import com.scto.mobile.ide.components.PreferenceSingleInput
import com.scto.mobile.ide.components.SettingsItem
import com.scto.mobile.ide.components.SmoothValueSlider
import com.scto.mobile.ide.components.SteppedValueSlider
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceGroup
import com.scto.mobile.ide.components.compose.preferences.base.PreferenceLayout
import com.scto.mobile.ide.editor.KeywordManager
import com.scto.mobile.ide.filetree.SortMode
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.tabs.editor.EditorTab
import kotlinx.coroutines.launch











@Composable
fun SettingsEditorScreen(navController: NavController) {
    PreferenceLayout(label = stringResource(id = com.scto.mobile.ide.core.main.R.string.editor), backArrowVisible = true) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.language_server)) {
            NextScreenCard(
                navController = navController,
                label = stringResource(com.scto.mobile.ide.core.main.R.string.manage_language_servers),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.manage_language_servers_desc),
                route = SettingsRoutes.LspSettings,
            )

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.insert_final_newline),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.insert_final_newline_desc),
                default = Settings.insert_final_newline,
                sideEffect = { Settings.insert_final_newline = it },
            )

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.trim_trailing_whitespace),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.trim_trailing_whitespace_desc),
                default = Settings.trim_trailing_whitespace,
                sideEffect = { Settings.trim_trailing_whitespace = it },
            )
        }

        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.formatting)) {
            NextScreenCard(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.manage_formatters),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.manage_formatters_desc),
                onClick = { navController.navigate(SettingsRoutes.Formatters.route) },
            )

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.format_on_save),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.format_on_save_desc),
                default = Settings.format_on_save,
                sideEffect = { Settings.format_on_save = it },
            )
        }

        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.intelligent_features)) {
            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.auto_close_tags),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.auto_close_tags_desc),
                default = Settings.auto_close_tags,
                sideEffect = {
                    Settings.auto_close_tags = it
                    refreshEditors()
                },
            )

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.bullet_continuation),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.bullet_continuation_desc),
                default = Settings.bullet_continuation,
                sideEffect = {
                    Settings.bullet_continuation = it
                    refreshEditors()
                },
            )

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.show_color_previews),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.show_color_previews_desc),
                default = Settings.show_color_previews,
                sideEffect = {
                    Settings.show_color_previews = it
                    refreshEditors()
                },
            )
        }

        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.content)) {
            val wordWrap = remember { mutableStateOf(Settings.word_wrap) }
            val wordWrapTxt = remember { mutableStateOf(Settings.word_wrap_text || Settings.word_wrap) }

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.word_wrap),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.word_wrap_desc),
                state = wordWrap,
                sideEffect = {
                    wordWrap.value = it
                    if (it) {
                        wordWrapTxt.value = true
                    }
                    Settings.word_wrap = it
                },
            )

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.txt_word_wrap),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.txt_word_wrap_desc),
                isEnabled = !wordWrap.value,
                state = wordWrapTxt,
                sideEffect = {
                    wordWrapTxt.value = it
                    Settings.word_wrap_text = it
                },
            )

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.read_mode),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.read_mode_desc),
                default = Settings.read_only_default,
                sideEffect = { Settings.read_only_default = it },
            )
        }

        PreferenceGroup(heading = stringResource(id = com.scto.mobile.ide.core.main.R.string.editor)) {
            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.disable_virtual_kbd),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.disable_virtual_kbd_desc),
                default = Settings.hide_soft_keyboard_if_hardware,
                sideEffect = { Settings.hide_soft_keyboard_if_hardware = it },
            )

            PreferenceSingleInput(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.line_spacing),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.line_spacing_desc),
                value = Settings.line_spacing.toString(),
                validate = {
                    if (it.toFloatOrNull() == null) {
                        context.getString(com.scto.mobile.ide.core.main.R.string.value_invalid)
                    } else if (it.toFloat() < 0.6f) {
                        context.getString(com.scto.mobile.ide.core.main.R.string.value_small)
                    } else {
                        null
                    }
                },
                onConfirm = {
                    Settings.line_spacing = it.toFloat()
                    scope.launch { refreshEditorSettings() }
                },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.cursor_anim),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.cursor_anim_desc),
                default = Settings.cursor_animation,
                sideEffect = { Settings.cursor_animation = it },
            )

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.show_minimap),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.show_minimap_desc),
                default = Settings.show_minimap,
                sideEffect = { Settings.show_minimap = it },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.show_line_number),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.show_line_number),
                default = Settings.show_line_numbers,
                sideEffect = { Settings.show_line_numbers = it },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.pin_line_number),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.pin_line_number),
                default = Settings.pin_line_number,
                sideEffect = { Settings.pin_line_number = it },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.render_whitespace),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.render_whitespace_desc),
                default = Settings.render_whitespace,
                sideEffect = { Settings.render_whitespace = it },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.show_suggestions),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.show_suggestions),
                default = Settings.show_suggestions,
                sideEffect = { Settings.show_suggestions = it },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.enable_sticky_scroll),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.enable_sticky_scroll_desc),
                default = Settings.sticky_scroll,
                sideEffect = { Settings.sticky_scroll = it },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.enable_quick_deletion),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.enable_quick_deletion_desc),
                default = Settings.quick_deletion,
                sideEffect = { Settings.quick_deletion = it },
            )

            NextScreenCard(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.manage_editor_font),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.manage_editor_font),
                route = SettingsRoutes.EditorFontScreen,
            )

            SmoothValueSlider(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.text_size),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.text_size_desc),
                default = Settings.editor_text_size,
                min = 6,
                max = 50,
            ) {
                Settings.editor_text_size = it
                scope.launch { refreshEditorSettings() }
            }

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.auto_closing_bracket),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.auto_closing_bracket_desc),
                default = Settings.auto_closing_bracket,
                sideEffect = { Settings.auto_closing_bracket = it },
            )

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.complete_on_enter),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.complete_on_enter_desc),
                default = Settings.complete_on_enter,
                sideEffect = { Settings.complete_on_enter = it },
            )

            SettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.text_mate_suggestion),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.text_mate_suggestion_desc),
                default = Settings.textmate_suggestions,
                sideEffect = { newValue ->
                    Settings.textmate_suggestions = newValue

                    scope.launch {
                        XedHost?.apply {
                            viewModel.tabs.filterIsInstance<EditorTab>().forEach { tab ->
                                val scope = tab.editorState.textmateScope ?: return@forEach
                                val textMateLanguage = tab.editorState.editor.get()?.getTextMateLanguage()

                                if (newValue) {
                                    val keywords = KeywordManager.getKeywords(scope)
                                    keywords?.let { textMateLanguage?.setCompleterKeywords(it.toTypedArray()) }
                                } else {
                                    textMateLanguage?.setCompleterKeywords(null)
                                }
                            }
                        }
                    }
                },
            )

            SteppedValueSlider(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.tab_size),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.tab_size_desc),
                default = Settings.tab_size,
                min = 1,
                max = 16,
            ) {
                Settings.tab_size = it

                XedHost?.apply {
                    viewModel.tabs.filterIsInstance<EditorTab>().forEach { tab ->
                        val textMateLanguage = tab.editorState.editor.get()?.getTextMateLanguage()
                        textMateLanguage?.tabSize = it
                    }
                }

                scope.launch { refreshEditorSettings() }
            }

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.use_tabs),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.use_tabs_desc),
                default = Settings.actual_tabs,
                sideEffect = {
                    Settings.actual_tabs = it

                    XedHost?.apply {
                        viewModel.tabs.filterIsInstance<EditorTab>().forEach { tab ->
                            val textMateLanguage = tab.editorState.editor.get()?.getTextMateLanguage()
                            textMateLanguage?.useTab(it)
                        }
                    }

                    scope.launch { refreshEditorSettings() }
                },
            )
        }

        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.actions)) {
            NextScreenCard(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.toolbar_actions),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.toolbar_actions_desc),
                route = SettingsRoutes.ToolbarActions,
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.extra_keys),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.extra_keys_desc),
                default = Settings.show_extra_keys,
                sideEffect = { Settings.show_extra_keys = it },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.extra_key_bg),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.extra_key_bg_desc),
                isEnabled = Settings.show_extra_keys,
                default = Settings.extra_keys_bg,
                sideEffect = { Settings.extra_keys_bg = it },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.split_extra_keys),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.split_extra_keys_desc),
                isEnabled = Settings.show_extra_keys,
                default = Settings.split_extra_keys,
                sideEffect = { Settings.split_extra_keys = it },
            )

            NextScreenCard(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.change_extra_keys),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.change_extra_keys_desc),
                route = SettingsRoutes.ExtraKeys,
                isEnabled = Settings.show_extra_keys,
            )
        }

        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.drawer)) {
            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.keep_drawer_locked),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.drawer_lock_desc),
                default = Settings.keep_drawer_locked,
                sideEffect = { Settings.keep_drawer_locked = it },
            )

            PreferenceList(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.sort_mode),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.sort_mode_desc),
                items = SortMode.entries.map { it to stringResource(it.stringRes) },
                selectedItem = SortMode.entries[Settings.sort_mode],
                onItemSelected = { sortMode ->
                    Settings.sort_mode = sortMode.ordinal
                    fileTreeViewModel.get()?.apply {
                        this.sortMode = sortMode
                        viewModelScope.launch { refreshEverything() }
                    }
                },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.show_hidden_files_drawer),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.show_hidden_files_drawer_desc),
                default = Settings.show_hidden_files_drawer,
                sideEffect = { Settings.show_hidden_files_drawer = it },
            )

            NextScreenCard(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.exclude_files_drawer),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.exclude_files_drawer_desc),
                onClick = { settingsNavController.get()!!.navigate("${SettingsRoutes.ExcludeFiles.route}/true") },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.compact_folders_drawer),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.compact_folders_drawer_desc),
                default = Settings.compact_folders_drawer,
                sideEffect = { Settings.compact_folders_drawer = it },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.show_hidden_files_search),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.show_hidden_files_search_desc),
                default = Settings.show_hidden_files_search,
                sideEffect = { Settings.show_hidden_files_search = it },
            )

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.always_index_projects),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.always_index_projects_desc),
                default = Settings.always_index_projects,
                sideEffect = { Settings.always_index_projects = it },
            )

            NextScreenCard(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.exclude_files_search),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.exclude_files_search_desc),
                onClick = { settingsNavController.get()!!.navigate("${SettingsRoutes.ExcludeFiles.route}/false") },
            )

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.auto_open_new_files),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.auto_open_new_files_desc),
                default = Settings.auto_open_new_files,
                sideEffect = { Settings.auto_open_new_files = it },
            )
        }

        PreferenceGroup(heading = stringResource(com.scto.mobile.ide.core.main.R.string.other)) {
            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.detect_bin_files),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.detect_bin_files_desc),
                default = Settings.detect_bin_files,
                sideEffect = { Settings.detect_bin_files = it },
            )

            EditorSettingsItem(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.oom_prediction),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.oom_prediction_desc),
                default = Settings.oom_prediction,
                sideEffect = { Settings.oom_prediction = it },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.restore_sessions),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.restore_sessions_desc),
                default = Settings.restore_sessions,
                sideEffect = { Settings.restore_sessions = it },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.smooth_tabs),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.smooth_tab_desc),
                default = Settings.smooth_tabs,
                sideEffect = { Settings.smooth_tabs = it },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.show_tab_icons),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.show_tab_icons_desc),
                default = Settings.show_tab_icons,
                sideEffect = { Settings.show_tab_icons = it },
            )

            NextScreenCard(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.default_encoding),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.default_encoding_desc),
                route = SettingsRoutes.DefaultEncoding,
            )

            NextScreenCard(
                label = stringResource(com.scto.mobile.ide.core.main.R.string.line_ending),
                description = stringResource(com.scto.mobile.ide.core.main.R.string.line_ending_desc),
                route = SettingsRoutes.DefaultLineEnding,
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.auto_save),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.auto_save_desc),
                default = Settings.auto_save,
                sideEffect = { Settings.auto_save = it },
            )

            PreferenceSingleInput(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.auto_save_delay),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.auto_save_delay_desc),
                value = Settings.auto_save_delay.toString(),
                validate = {
                    if (it.toIntOrNull() == null) {
                        context.getString(com.scto.mobile.ide.core.main.R.string.value_invalid)
                    } else if (it.toInt() > 4000) {
                        context.getString(com.scto.mobile.ide.core.main.R.string.value_large)
                    } else if (it.toInt() < 5) {
                        context.getString(com.scto.mobile.ide.core.main.R.string.value_small)
                    } else {
                        null
                    }
                },
                onConfirm = {
                    Settings.auto_save_delay = it.toLong()
                    scope.launch { refreshEditorSettings() }
                },
            )

            EditorSettingsItem(
                label = stringResource(id = com.scto.mobile.ide.core.main.R.string.enable_editorconfig),
                description = stringResource(id = com.scto.mobile.ide.core.main.R.string.enable_editorconfig_desc),
                default = Settings.enable_editorconfig,
                sideEffect = {
                    Settings.enable_editorconfig = it
                    scope.launch { refreshEditorSettings() }
                },
            )
        }
    }
}

fun refreshEditors() {
    XedHost?.apply {
        viewModel.tabs.forEach {
            if (it is EditorTab) {
                it.refreshKey++
            }
        }
    }
}

suspend fun refreshEditorSettings() {
    XedHost?.apply {
        viewModel.tabs.forEach {
            if (it is EditorTab) {
                it.reapplyEditorSettings()
            }
        }
    }
}
