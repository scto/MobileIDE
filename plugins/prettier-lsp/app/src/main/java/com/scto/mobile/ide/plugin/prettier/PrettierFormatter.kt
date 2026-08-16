package com.scto.mobile.ide.plugin.prettier

import com.scto.mobile.ide.plugin.prettier.utils.buildArgs
import com.scto.mobile.ide.exec.ubuntuProcess
import com.scto.mobile.ide.features.extensions.ExtensionContext
import com.scto.mobile.ide.core.common.files.FileObject
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.TextRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class PrettierFormatter(
    private val context: ExtensionContext,
    private val settings: PrettierSettings,
    private val binary: File,
    private val targetFile: FileObject,
) : Formatter {

    private var process: Process? = null
    private var receiver: Formatter.FormatResultReceiver? = null

    override fun format(
        text: Content,
        cursorRange: TextRange,
    ) {
        context.scope.launch {
            runPrettier(text.toString(), cursorRange)
        }
    }

    override fun formatRegion(
        text: Content,
        rangeToFormat: TextRange,
        cursorRange: TextRange,
    ) {
        context.scope.launch {
            runPrettier(
                text.toString(),
                cursorRange,
                arrayOf(
                    "--range-start=${rangeToFormat.startIndex}",
                    "--range-end=${rangeToFormat.endIndex}",
                ),
            )
        }
    }

    private suspend fun runPrettier(
        source: String,
        cursorRange: TextRange,
        extraArgs: Array<String> = emptyArray(),
    ) =
        withContext(Dispatchers.IO) {
            if (process != null) {
                receiver?.onFormatFail(IllegalStateException("Prettier is already running"))
                context.logError("Prettier is already running")
                return@withContext
            }

            if (!binary.exists()) {
                receiver?.onFormatFail(IllegalStateException("Prettier binary not found: ${binary.absolutePath}"))
                context.logError("Prettier binary not found: ${binary.absolutePath}")
                return@withContext
            }

            try {
                val currentProcess =
                    ubuntuProcess(
                            command =
                                listOf(
                                    binary.absolutePath,
                                    "--stdin-filepath",
                                    targetFile.getAbsolutePath(),
                                    *extraArgs,
                                    *buildArgs(settings),
                                )
                        )
                        .also {
                            process = it
                        }

                currentProcess.outputStream.bufferedWriter().use { writer ->
                    writer.write(source)
                }

                val timedOut = !currentProcess.waitFor(5, TimeUnit.SECONDS)

                if (timedOut) {
                    currentProcess.destroy()
                    receiver?.onFormatFail(RuntimeException("Prettier timed out"))
                    context.logError("Prettier timed out")
                    return@withContext
                }

                val result = currentProcess.inputStream.bufferedReader().readText()
                val error = currentProcess.errorStream.bufferedReader().readText()

                if (currentProcess.exitValue() != 0) {
                    receiver?.onFormatFail(RuntimeException("Prettier failed: $error"))
                    context.logError("Prettier failed: $error")
                    return@withContext
                }

                receiver?.onFormatSucceed(result, cursorRange)
            } catch (e: Exception) {
                receiver?.onFormatFail(e)
                context.logError("Prettier failed: \n${e.stackTraceToString()}")
            } finally {
                process = null
            }
        }

    override fun setReceiver(receiver: Formatter.FormatResultReceiver?) {
        this.receiver = receiver
    }

    override fun isRunning(): Boolean {
        return process?.isAlive ?: false
    }

    override fun destroy() {
        process?.destroy()
        process = null
        receiver = null
    }
}
