package com.mobileide.gradle

import com.mobileide.terminal.data.TerminalRepository

import kotlinx.coroutines.flow.toList
import javax.inject.Inject

data class GradleTask(val name: String, val description: String)

interface GradleRepository {
    suspend fun listTasks(projectPath: String): Result<List<GradleTask>>
    suspend fun executeTask(projectPath: String, taskName: String): Result<List<String>>
}

class GradleRepositoryImpl @Inject constructor(
    private val terminalRepository: TerminalRepository
) : GradleRepository {
    
    override suspend fun listTasks(projectPath: String): Result<List<GradleTask>> {
        return try {
            // Führe `./gradlew tasks` aus und sammle die Ausgabe
            val outputLines = terminalRepository.execute("./gradlew tasks", projectPath).toList()
            
            // Simples Parsen der Ausgabe
            val tasks = mutableListOf<GradleTask>()
            var currentGroup = ""
            
            outputLines.forEach { line ->
                if (line.endsWith(" tasks")) {
                    currentGroup = line.replace(" tasks", "").trim()
                } else if (line.matches(Regex("^\\w.* - .*$"))) {
                    val parts = line.split(" - ", limit = 2)
                    tasks.add(GradleTask(name = parts[0], description = parts[1]))
                }
            }
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun executeTask(projectPath: String, taskName: String): Result<List<String>> {
        return try {
            val outputLines = terminalRepository.execute("./gradlew $taskName", projectPath).toList()
            Result.success(outputLines)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}