package com.mobileide.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import javax.inject.Inject

data class TermuxPackage(val name: String, val version: String)

interface TermuxRepository {
    suspend fun listInstalledPackages(): Result<List<TermuxPackage>>
    suspend fun installPackage(packageName: String): Result<Unit>
    suspend fun uninstallPackage(packageName: String): Result<Unit>
}

class TermuxRepositoryImpl @Inject constructor(
    private val terminalRepository: TerminalRepository
) : TermuxRepository {

    // IMPORTANT: This working directory assumes Termux is installed in its default location.
    private val termuxBinPath = "/data/data/com.termux/files/usr/bin"

    override suspend fun listInstalledPackages(): Result<List<TermuxPackage>> {
        return try {
            val output = terminalRepository.execute("pkg list-installed", termuxBinPath).toList()
            val packages = parsePackages(output)
            Result.success(packages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun installPackage(packageName: String): Result<Unit> {
        return runCommand("pkg install -y $packageName")
    }

    override suspend fun uninstallPackage(packageName: String): Result<Unit> {
        return runCommand("pkg uninstall -y $packageName")
    }
    
    private suspend fun runCommand(command: String): Result<Unit> {
         return try {
            // We execute the command but ignore the output for a simple Result<Unit>
            terminalRepository.execute(command, termuxBinPath).first()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parses the raw output of `pkg list-installed` into a list of TermuxPackage objects.
     * Example line: "zlib/stable,now 1.2.11 aarch64 [installed]"
     */
    private fun parsePackages(lines: List<String>): List<TermuxPackage> {
        val packageRegex = Regex("""^(\S+)/.*?,now\s(.*?)\s.*\[installed]""")
        return lines.mapNotNull { line ->
            packageRegex.find(line)?.let {
                TermuxPackage(name = it.groupValues[1], version = it.groupValues[2])
            }
        }
    }
}
