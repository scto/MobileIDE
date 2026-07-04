package com.scto.mobile.ide.core.tooling.server

import com.scto.mobile.ide.core.tooling.api.IpcProtocol
import java.io.File
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.Executors

/**
 * Unix Domain Socket IPC server for Gradle build operations.
 *
 * Listens at [IpcProtocol.SOCKET_PATH] and handles two commands:
 *   GET_TASKS|<projectPath>      → TASKS_LIST|task1,task2,...
 *   EXECUTE_TASKS|<path>|t1,t2  → stream of PROGRESS/LOG lines → RESULT|SUCCESS|ERROR
 *
 * Runs on the Linux/JVM side (proot Ubuntu, Java 17+) — NOT on Android.
 * Android clients connect using [android.net.LocalSocket].
 */
class GradleIpcServer {

    @Volatile private var running = false
    private var serverChannel: ServerSocketChannel? = null
    private val executor = Executors.newCachedThreadPool { r ->
        Thread(r, "gradle-ipc-worker").apply { isDaemon = true }
    }

    /** Starts the blocking accept loop. Blocks the calling thread until [stop] is called. */
    fun start() {
        val socketFile = File(IpcProtocol.SOCKET_PATH)
        socketFile.delete()
        socketFile.parentFile?.mkdirs()

        val address = UnixDomainSocketAddress.of(IpcProtocol.SOCKET_PATH)
        val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        channel.bind(address)
        serverChannel = channel
        running = true

        println("[GradleIpcServer] Listening on ${IpcProtocol.SOCKET_PATH}")

        try {
            while (running) {
                val client = channel.accept() ?: break
                executor.submit { handleClient(client) }
            }
        } catch (e: Exception) {
            if (running) System.err.println("[GradleIpcServer] Accept error: ${e.message}")
        } finally {
            socketFile.delete()
            println("[GradleIpcServer] Stopped.")
        }
    }

    fun stop() {
        running = false
        try { serverChannel?.close() } catch (_: Exception) {}
        executor.shutdownNow()
    }

    private fun handleClient(channel: java.nio.channels.SocketChannel) {
        try {
            channel.configureBlocking(true)
            val socket = channel.socket()
            val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
            val writer = socket.getOutputStream().bufferedWriter(Charsets.UTF_8)

            val line   = reader.readLine() ?: return
            val parts  = line.split(IpcProtocol.DELIMITER, limit = 3)

            when (parts.getOrNull(0)) {
                IpcProtocol.CMD_GET_TASKS -> {
                    val path = parts.getOrElse(1) { "" }.ifBlank { return sendError(writer, "Missing project path") }
                    handleGetTasks(path, writer)
                }
                IpcProtocol.CMD_EXECUTE_TASKS -> {
                    val path     = parts.getOrElse(1) { "" }.ifBlank { return sendError(writer, "Missing project path") }
                    val tasksCsv = parts.getOrElse(2) { "" }.ifBlank { return sendError(writer, "Missing task list") }
                    val tasks    = tasksCsv.split(IpcProtocol.LIST_SEPARATOR).filter { it.isNotBlank() }
                    handleExecuteTasks(path, tasks, writer)
                }
                else -> sendError(writer, "Unknown command: ${parts.getOrNull(0)}")
            }
        } catch (e: Exception) {
            System.err.println("[GradleIpcServer] Client error: ${e.message}")
        } finally {
            try { channel.close() } catch (_: Exception) {}
        }
    }

    private fun handleGetTasks(projectPath: String, writer: java.io.BufferedWriter) {
        try {
            val csv = GradleModelReader.fetchTaskPaths(projectPath)
                .joinToString(IpcProtocol.LIST_SEPARATOR)
            writer.write("${IpcProtocol.RESP_TASKS_LIST}${IpcProtocol.DELIMITER}$csv")
            writer.newLine(); writer.flush()
        } catch (e: Exception) {
            sendError(writer, "fetchTasks failed: ${e.message}")
        }
    }

    private fun handleExecuteTasks(
        projectPath: String,
        tasks: List<String>,
        writer: java.io.BufferedWriter,
    ) {
        GradleServerWorker(
            projectPath  = projectPath,
            tasks        = tasks,
            outputStream = object : java.io.OutputStream() {
                override fun write(b: Int) = writer.write(b)
                override fun write(b: ByteArray, off: Int, len: Int) =
                    writer.write(String(b, off, len, Charsets.UTF_8))
                override fun flush() = writer.flush()
            },
        ).execute()
    }

    private fun sendError(writer: java.io.BufferedWriter, message: String) {
        try {
            writer.write("${IpcProtocol.RESP_ERROR}${IpcProtocol.DELIMITER}$message")
            writer.newLine(); writer.flush()
        } catch (_: Exception) {}
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val server = GradleIpcServer()
            Runtime.getRuntime().addShutdownHook(Thread { server.stop() })
            server.start()
        }
    }
}
