package com.scto.mobile.ide.core.tooling.impl.client

import android.net.LocalSocket
import android.net.LocalSocketAddress
import com.scto.mobile.ide.core.tooling.api.GradleLogEvent
import com.scto.mobile.ide.core.tooling.api.GradleToolingManager
import com.scto.mobile.ide.core.tooling.api.IpcProtocol
import com.scto.mobile.ide.core.tooling.api.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Android-side Gradle tooling client that communicates with [GradleIpcServer]
 * via a Unix Domain Socket at [IpcProtocol.SOCKET_PATH].
 *
 * Features:
 *  - Fully coroutine-based, non-blocking read loop on [Dispatchers.IO]
 *  - Backpressure / chunked buffering: log lines are batched in 50 ms windows
 *    or in chunks of 30 before being emitted, preventing UI thread starvation
 *    during heavy compiler output bursts.
 *  - Parses the raw IPC protocol into typed [GradleLogEvent] objects.
 */
class SocketToolingClient : GradleToolingManager {

    // ── fetchAvailableTasks ───────────────────────────────────────────────────

    override fun fetchAvailableTasks(projectPath: String): Flow<Resource<List<String>>> = flow {
        emit(Resource.Loading)
        try {
            val response = withContext(Dispatchers.IO) {
                openSocket().use { socket ->
                    val writer = socket.outputStream.bufferedWriter(Charsets.UTF_8)
                    val reader = socket.inputStream.bufferedReader(Charsets.UTF_8)

                    // Send request
                    writer.write("${IpcProtocol.CMD_GET_TASKS}${IpcProtocol.DELIMITER}$projectPath")
                    writer.newLine()
                    writer.flush()

                    // Read single-line response
                    reader.readLine() ?: error("No response from server")
                }
            }

            val parts = response.split(IpcProtocol.DELIMITER, limit = 2)
            when (parts.getOrNull(0)) {
                IpcProtocol.RESP_TASKS_LIST -> {
                    val tasks = parts.getOrNull(1)
                        ?.split(IpcProtocol.LIST_SEPARATOR)
                        ?.filter { it.isNotBlank() }
                        ?: emptyList()
                    emit(Resource.Success(tasks))
                }
                IpcProtocol.RESP_ERROR -> {
                    emit(Resource.Error(parts.getOrNull(1) ?: "Unknown server error"))
                }
                else -> {
                    emit(Resource.Error("Unexpected response: $response"))
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error("Connection failed: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)

    // ── executeTasks ──────────────────────────────────────────────────────────

    override fun executeTasks(projectPath: String, tasks: List<String>): Flow<GradleLogEvent> = flow {
        val tasksCsv = tasks.joinToString(IpcProtocol.LIST_SEPARATOR)

        try {
            openSocket().use { socket ->
                val writer = socket.outputStream.bufferedWriter(Charsets.UTF_8)
                val reader = socket.inputStream.bufferedReader(Charsets.UTF_8)

                // Send execute command
                writer.write(
                    "${IpcProtocol.CMD_EXECUTE_TASKS}${IpcProtocol.DELIMITER}" +
                    "$projectPath${IpcProtocol.DELIMITER}$tasksCsv"
                )
                writer.newLine()
                writer.flush()

                // Non-blocking coroutine read loop with backpressure buffering
                val buffer = mutableListOf<GradleLogEvent>()
                var lastFlush = System.currentTimeMillis()
                val CHUNK_SIZE = 30
                val WINDOW_MS = 50L

                while (coroutineContext.isActive) {
                    val line = withContext(Dispatchers.IO) { reader.readLine() }
                        ?: break  // Connection closed

                    val event = parseLine(line) ?: continue

                    // Result is terminal — flush buffer and emit final result
                    if (event is GradleLogEvent.Result) {
                        buffer.forEach { emit(it) }
                        buffer.clear()
                        emit(event)
                        break
                    }

                    buffer.add(event)

                    val now = System.currentTimeMillis()
                    val windowElapsed = now - lastFlush >= WINDOW_MS
                    val chunkFull = buffer.size >= CHUNK_SIZE

                    if (chunkFull || windowElapsed) {
                        buffer.forEach { emit(it) }
                        buffer.clear()
                        lastFlush = now
                        // Yield briefly to allow UI recomposition during heavy output
                        delay(1)
                    }
                }

                // Flush any remaining buffered events
                buffer.forEach { emit(it) }
                buffer.clear()
            }
        } catch (e: Exception) {
            emit(
                GradleLogEvent.Output(
                    logLevel = IpcProtocol.LEVEL_ERROR,
                    text = "Connection error: ${e.message}",
                )
            )
            emit(GradleLogEvent.Result(isSuccess = false, message = e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun openSocket(): LocalSocket {
        val socket = LocalSocket(LocalSocket.SOCKET_STREAM)
        socket.connect(
            LocalSocketAddress(
                IpcProtocol.SOCKET_PATH,
                LocalSocketAddress.Namespace.FILESYSTEM,
            )
        )
        socket.soTimeout = 0  // No timeout — builds can take arbitrarily long
        return socket
    }

    /**
     * Parses a raw IPC line into a typed [GradleLogEvent].
     * Returns null for lines that cannot be parsed (e.g. empty lines).
     */
    private fun parseLine(line: String): GradleLogEvent? {
        if (line.isBlank()) return null
        val parts = line.split(IpcProtocol.DELIMITER, limit = 3)
        val prefix = parts.getOrNull(0) ?: return null

        return when (prefix) {
            IpcProtocol.RESP_PROGRESS -> {
                GradleLogEvent.Progress(
                    message = parts.getOrNull(1) ?: line,
                )
            }
            IpcProtocol.RESP_LOG -> {
                GradleLogEvent.Output(
                    logLevel = parts.getOrNull(1) ?: IpcProtocol.LEVEL_INFO,
                    text = parts.getOrNull(2) ?: "",
                )
            }
            IpcProtocol.RESP_RESULT -> {
                val isSuccess = parts.getOrNull(1) == IpcProtocol.RESULT_SUCCESS
                GradleLogEvent.Result(
                    isSuccess = isSuccess,
                    message = if (isSuccess) "Build successful" else "Build failed",
                )
            }
            IpcProtocol.RESP_ERROR -> {
                GradleLogEvent.Output(
                    logLevel = IpcProtocol.LEVEL_ERROR,
                    text = parts.getOrNull(1) ?: "Server error",
                )
            }
            else -> {
                // Unknown prefix — treat as plain INFO output
                GradleLogEvent.Output(
                    logLevel = IpcProtocol.LEVEL_INFO,
                    text = line,
                )
            }
        }
    }
}
