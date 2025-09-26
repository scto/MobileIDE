package com.mobileide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject

interface TerminalRepository {
    fun execute(command: String, workingDir: String): Flow<String>
}

class TerminalRepositoryImpl @Inject constructor() : TerminalRepository {
    override fun execute(command: String, workingDir: String): Flow<String> = flow {
        if (command.isBlank()) {
            emit("")
            return@flow
        }
        try {
            // Robust command splitting that respects quotes.
            val commandList = command.splitCommand()
            val process = ProcessBuilder(commandList)
                .directory(File(workingDir))
                .redirectErrorStream(true) // Combines stdout and stderr
                .start()

            // Read the output stream line by line.
            InputStreamReader(process.inputStream).forEachLine { line ->
                emit(line)
            }

            val exitCode = process.waitFor()
            emit("Process finished with exit code $exitCode")

        } catch (e: Exception) {
            emit("Error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Splits a command string into a list of arguments, handling quoted parts.
     */
    private fun String.splitCommand(): List<String> {
        val result = mutableListOf<String>()
        val regex = Regex("\"([^\"]*)\"|'([^']*)'|\\S+")
        regex.findAll(this).forEach {
            result.add(it.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: it.value)
        }
        return result
    }
}
