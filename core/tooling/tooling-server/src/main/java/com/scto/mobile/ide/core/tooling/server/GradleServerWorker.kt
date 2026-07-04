package com.scto.mobile.ide.core.tooling.server

import com.scto.mobile.ide.core.tooling.api.IpcProtocol
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProgressListener
import java.io.File
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * Executes Gradle tasks via the Tooling API, capturing stdout/stderr via
 * piped streams and formatting them as IPC protocol lines.
 *
 * Output written to [outputStream] per line:
 *   PROGRESS|<description>
 *   LOG|INFO|<message>
 *   LOG|ERROR|<message>
 *   RESULT|SUCCESS
 *   RESULT|ERROR
 *
 * Runs on the Linux/JVM side (proot Ubuntu) — NOT on Android.
 */
class GradleServerWorker(
    private val projectPath: String,
    private val tasks: List<String>,
    private val outputStream: OutputStream,
) {
    private val writer = outputStream.bufferedWriter(Charsets.UTF_8)

    fun execute() {
        val connector = GradleConnector.newConnector()
            .forProjectDirectory(File(projectPath))
        try {
            connector.connect().use { connection ->
                val (stdoutPipe, stdoutReader) = createLogPipe(IpcProtocol.LEVEL_INFO)
                val (stderrPipe, stderrReader) = createLogPipe(IpcProtocol.LEVEL_ERROR)

                val stdoutThread = Thread(stdoutReader, "gradle-stdout-reader").apply { isDaemon = true; start() }
                val stderrThread = Thread(stderrReader, "gradle-stderr-reader").apply { isDaemon = true; start() }

                val build = connection.newBuild()
                    .forTasks(*tasks.toTypedArray())
                    .setStandardOutput(stdoutPipe)
                    .setStandardError(stderrPipe)
                    .addProgressListener(ProgressListener { event ->
                        writeLine("${IpcProtocol.RESP_PROGRESS}${IpcProtocol.DELIMITER}${event.description}")
                    })

                try {
                    build.run()
                    stdoutThread.join(2_000)
                    stderrThread.join(2_000)
                    writeLine("${IpcProtocol.RESP_RESULT}${IpcProtocol.DELIMITER}${IpcProtocol.RESULT_SUCCESS}")
                } catch (e: Exception) {
                    stdoutThread.join(2_000)
                    stderrThread.join(2_000)
                    writeLine("${IpcProtocol.RESP_LOG}${IpcProtocol.DELIMITER}${IpcProtocol.LEVEL_ERROR}${IpcProtocol.DELIMITER}Build failed: ${e.message}")
                    writeLine("${IpcProtocol.RESP_RESULT}${IpcProtocol.DELIMITER}${IpcProtocol.RESULT_ERROR}")
                }
            }
        } catch (e: Exception) {
            writeLine("${IpcProtocol.RESP_LOG}${IpcProtocol.DELIMITER}${IpcProtocol.LEVEL_ERROR}${IpcProtocol.DELIMITER}Gradle connection failed: ${e.message}")
            writeLine("${IpcProtocol.RESP_RESULT}${IpcProtocol.DELIMITER}${IpcProtocol.RESULT_ERROR}")
        }
    }

    private fun writeLine(line: String) {
        try { writer.write(line); writer.newLine(); writer.flush() } catch (_: Exception) {}
    }

    private fun createLogPipe(logLevel: String): Pair<PipedOutputStream, Runnable> {
        val pipeIn  = PipedInputStream(65_536)
        val pipeOut = PipedOutputStream(pipeIn)
        val reader  = Runnable {
            try {
                pipeIn.bufferedReader(Charsets.UTF_8).forEachLine { line ->
                    if (line.isNotBlank()) {
                        val level = when {
                            line.startsWith("w:") || line.startsWith("WARN ")  -> IpcProtocol.LEVEL_WARN
                            line.startsWith("e:") || line.startsWith("ERROR ") -> IpcProtocol.LEVEL_ERROR
                            else -> logLevel
                        }
                        writeLine("${IpcProtocol.RESP_LOG}${IpcProtocol.DELIMITER}$level${IpcProtocol.DELIMITER}$line")
                    }
                }
            } catch (_: Exception) {}
        }
        return pipeOut to reader
    }
}
