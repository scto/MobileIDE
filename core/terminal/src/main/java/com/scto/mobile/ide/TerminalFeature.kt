package com.scto.mobile.ide

import android.app.Application
import android.content.Intent
import com.scto.mobile.ide.activities.main.MainActivity
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.activities.terminal.Terminal
import com.scto.mobile.ide.commands.CommandProvider
import com.scto.mobile.ide.commands.ToolbarConfiguration
import com.scto.mobile.ide.commands.global.TerminalCommand
import com.scto.mobile.ide.drawer.AddProjectOption
import com.scto.mobile.ide.drawer.AddProjectRegistry
import com.scto.mobile.ide.exec.pendingCommand
import com.scto.mobile.ide.exec.ubuntuProcess
import com.scto.mobile.ide.feature.Feature
import com.scto.mobile.ide.feature.FeatureRegistry
import com.scto.mobile.ide.feature.FeatureToggle
import com.scto.mobile.ide.feature.SettingsCategory
import com.scto.mobile.ide.feature.SettingsRegistry
import com.scto.mobile.ide.feature.SettingsRoute
import com.scto.mobile.ide.file.FileObject
import com.scto.mobile.ide.file.FileWrapper
import com.scto.mobile.ide.file.sandboxHomeDir
import com.scto.mobile.ide.filetree.FileAction
import com.scto.mobile.ide.filetree.FileActionContext
import com.scto.mobile.ide.filetree.FileActionProvider
import com.scto.mobile.ide.filetree.FileActionType
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.lsp.LspRegistry
import com.scto.mobile.ide.lsp.servers.Bash
import com.scto.mobile.ide.lsp.servers.CSS
import com.scto.mobile.ide.lsp.servers.ESLint
import com.scto.mobile.ide.lsp.servers.Emmet
import com.scto.mobile.ide.lsp.servers.HTML
import com.scto.mobile.ide.lsp.servers.Markdown
import com.scto.mobile.ide.lsp.servers.TypeScript
import com.scto.mobile.ide.lsp.servers.XML
import com.scto.mobile.ide.resources.drawables
import com.scto.mobile.ide.resources.getString
import com.scto.mobile.ide.resources.strings
import com.scto.mobile.ide.runner.RunnerManager
import com.scto.mobile.ide.runner.runners.UniversalRunner
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.settings.editor.TerminalFontScreen
import com.scto.mobile.ide.settings.terminal.SettingsTerminalScreen
import com.scto.mobile.ide.settings.terminal.TerminalCheckScreen
import com.scto.mobile.ide.settings.terminal.TerminalExtraKeys
import com.scto.mobile.ide.utils.dialogRes
import com.scto.mobile.ide.utils.toast

class TerminalFeature : Feature {
    override val toggle =
        FeatureToggle(
            nameRes = strings.terminal_feature,
            key = "feature_terminal",
            default = true,
            iconRes = drawables.terminal,
        )

    override fun init(application: Application) {

        // Register the file action
        FileActionProvider.registerAction(TerminalAction)

        // Register settings categories
        SettingsRegistry.registerCategory(
            SettingsCategory(
                labelRes = strings.terminal,
                descriptionRes = strings.terminal_desc,
                iconRes = drawables.terminal,
                route = SettingsRoutes.TerminalSettings.route,
            )
        )

        if (FeatureRegistry.isEnabled("feature_terminal")) {
            AddProjectRegistry.options.add(
                AddProjectOption(
                    icon = Icon.ResourceIcon(drawables.terminal),
                    titleRes = strings.terminal_home,
                    descriptionRes = strings.terminal_home_desc,
                    onClick = { onDismiss ->
                        if (!Settings.has_shown_terminal_dir_warning) {
                            dialogRes(
                                title = strings.attention.getString(),
                                msg = strings.warning_private_dir.getString(),
                                onOk = {
                                    Settings.has_shown_terminal_dir_warning = true
                                    MainActivity.instance
                                        ?.drawerViewModel
                                        ?.addFileTreeTab(FileWrapper(sandboxHomeDir()), true)
                                },
                            )
                        } else {
                            MainActivity.instance?.drawerViewModel?.addFileTreeTab(FileWrapper(sandboxHomeDir()), true)
                        }
                        onDismiss()
                    },
                )
            )
        }

        // Register settings routes
        SettingsRegistry.registerRoute(
            SettingsRoute(SettingsRoutes.TerminalSettings.route) { SettingsTerminalScreen() }
        )
        SettingsRegistry.registerRoute(SettingsRoute(SettingsRoutes.TerminalExtraKeys.route) { TerminalExtraKeys() })
        SettingsRegistry.registerRoute(SettingsRoute(SettingsRoutes.TerminalCheck.route) { TerminalCheckScreen() })
        SettingsRegistry.registerRoute(SettingsRoute(SettingsRoutes.TerminalFontScreen.route) { TerminalFontScreen() })
        // Register UniversalRunner dynamically
        RunnerManager.registerRunner(UniversalRunner)

        // Register TerminalLauncher handler
        TerminalLauncher.handler = { activity, sandbox, exe, args, id, terminatePreviousSession, workingDir, env ->
            pendingCommand =
                com.scto.mobile.ide.exec.TerminalCommand(
                    sandbox = sandbox,
                    exe = exe,
                    args = args,
                    id = id,
                    terminatePreviousSession = terminatePreviousSession,
                    workingDir = workingDir,
                    env = env,
                )
            try {
                val intent = Intent(activity, Terminal::class.java)
                activity.startActivity(intent)
            } catch (e: Exception) {
                toast("Terminal feature is not available in this build")
            }
        }

        // Register SandboxedProcessRegistry provider
        SandboxedProcessRegistry.provider = { command, workingDir, excludeMounts ->
            ubuntuProcess(excludeMounts, workingDir = workingDir, command = command)
        }

        // Register global command
        val command = TerminalCommand()
        CommandProvider.registerCommand(command)

        // assuming there's atleast one item already there
        ToolbarConfiguration.addGlobalToolbarCommand(command, index = 1)

        // Register built-in LSP servers
        LspRegistry.registerServer(Bash)
        LspRegistry.registerServer(CSS)
        LspRegistry.registerServer(ESLint)
        LspRegistry.registerServer(Emmet)
        LspRegistry.registerServer(HTML)
        LspRegistry.registerServer(Markdown)
        LspRegistry.registerServer(TypeScript)
        LspRegistry.registerServer(XML)
    }
}

object TerminalAction : FileAction() {
    override val icon = Icon.ResourceIcon(drawables.terminal)
    override val title = strings.open_in_terminal.getString()

    override fun action(context: FileActionContext) {
        val file = context.file
        val ctx = context.context

        val intent = Intent(ctx, Terminal::class.java)
        intent.putExtra("cwd", file.getAbsolutePath())
        ctx.startActivity(intent)
    }

    override fun isSupported(file: FileObject): Boolean {
        return file is FileWrapper && FeatureRegistry.isEnabled("feature_terminal")
    }

    override val type = FileActionType(file = false, folder = true, rootFolder = true)
}
