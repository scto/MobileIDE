package com.scto.mobile.ide.ui.editor.aicoding

import android.content.Context
import com.scto.mobile.ide.features.exec.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.io.File

object AiderBridgeService {

    private fun getHomeDir(context: Context): File {
        val pkgName = context.packageName
        val homePath = "/data/data/$pkgName/files/home"
        val dir = File(homePath)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSecretFile(context: Context, fileName: String): File {
        return File(getHomeDir(context), fileName)
    }

    fun getApiKey(context: Context, provider: AiderProvider): String {
        val file = getSecretFile(context, provider.secretFileName)
        return if (file.exists()) file.readText().trim() else ""
    }

    fun saveApiKey(context: Context, provider: AiderProvider, key: String) {
        val file = getSecretFile(context, provider.secretFileName)
        file.writeText(key.trim())
    }

    fun getVenvPath(context: Context): File {
        return File(getHomeDir(context), ".venv")
    }

    fun isVenvInstalled(context: Context): Boolean {
        val venvDir = getVenvPath(context)
        val binActivate = File(venvDir, "bin/activate")
        return venvDir.exists() && binActivate.exists()
    }

    fun buildAiderCommand(
        context: Context,
        projectPath: String,
        model: AiderModelSpec,
        chatMode: AiderChatMode,
        isSubtreeOnly: Boolean,
        isBrowser: Boolean,
        message: String,
        selectedContextFiles: List<String>
    ): List<String> {
        val homeDir = getHomeDir(context).absolutePath
        val venvActivate = "$homeDir/.venv/bin/activate"

        val geminiKey = getApiKey(context, AiderProvider.GEMINI_FLASH).ifBlank { getApiKey(context, AiderProvider.GEMINI_PRO) }
        val anthropicKey = getApiKey(context, AiderProvider.CLAUDE)
        val openaiKey = getApiKey(context, AiderProvider.OPENAI)
        val deepseekKey = getApiKey(context, AiderProvider.DEEPSEEK)

        val envPrefix = StringBuilder()
        if (geminiKey.isNotBlank()) envPrefix.append("export GEMINI_API_KEY=\"$geminiKey\" && ")
        if (anthropicKey.isNotBlank()) envPrefix.append("export ANTHROPIC_API_KEY=\"$anthropicKey\" && ")
        if (openaiKey.isNotBlank()) envPrefix.append("export OPENAI_API_KEY=\"$openaiKey\" && ")
        if (deepseekKey.isNotBlank()) envPrefix.append("export DEEPSEEK_API_KEY=\"$deepseekKey\" && ")
        envPrefix.append("export UV_LINK_MODE=copy && ")

        val cmdBuilder = StringBuilder()
        cmdBuilder.append(envPrefix)
        cmdBuilder.append("source $venvActivate && ")
        cmdBuilder.append("aider --model ${model.id}")

        if (chatMode != AiderChatMode.AUTO) {
            cmdBuilder.append(" --${chatMode.flagName}")
        }
        if (isSubtreeOnly) {
            cmdBuilder.append(" --subtree-only")
        }
        if (isBrowser || model.isBrowserSupported) {
            cmdBuilder.append(" --browser")
        }

        // Add config file if exists
        val configFile = File(projectPath, ".aider.conf.yml")
        if (configFile.exists()) {
            cmdBuilder.append(" --config .aider.conf.yml")
        }

        // Add read-only context files
        for (cf in selectedContextFiles) {
            val file = File(projectPath, cf)
            if (file.exists()) {
                cmdBuilder.append(" --read $cf")
            }
        }

        // Add non-interactive message prompt if provided
        if (message.isNotBlank()) {
            val safeMsg = message.replace("\"", "\\\"")
            cmdBuilder.append(" --message \"$safeMsg\" --no-auto-commits")
        }

        return listOf("bash", "-c", cmdBuilder.toString())
    }

    fun executeAiderStream(
        context: Context,
        projectPath: String,
        model: AiderModelSpec,
        chatMode: AiderChatMode,
        isSubtreeOnly: Boolean,
        isBrowser: Boolean,
        message: String,
        selectedContextFiles: List<String>
    ): Flow<String> = flow {
        val command = buildAiderCommand(
            context = context,
            projectPath = projectPath,
            model = model,
            chatMode = chatMode,
            isSubtreeOnly = isSubtreeOnly,
            isBrowser = isBrowser,
            message = message,
            selectedContextFiles = selectedContextFiles
        )

        Timber.tag("AiderBridgeService").i("Executing Aider command in $projectPath: ${command.joinToString(" ")}")
        emit("🚀 Starte Aider (${model.name}, Modus: ${chatMode.displayName})...\n")

        try {
            val process = ProcessBuilder(command)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    emit(line!! + "\n")
                }
            }

            val exitCode = process.waitFor()
            emit("\n✅ Aider beendet mit Exit-Code: $exitCode\n")
        } catch (e: Exception) {
            Timber.tag("AiderBridgeService").e(e, "Error running Aider process")
            emit("\n❌ Fehler beim Ausführen von Aider: ${e.message}\n")
        }
    }.flowOn(Dispatchers.IO)
}
