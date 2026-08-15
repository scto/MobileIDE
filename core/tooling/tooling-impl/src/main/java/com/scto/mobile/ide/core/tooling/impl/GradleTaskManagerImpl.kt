package com.scto.mobile.ide.core.tooling.impl

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object GradleTaskManagerImpl : GradleTaskManager {

    private val tasksCache = ConcurrentHashMap<String, List<GradleTask>>()

    private fun getPrefixDir(context: Context): File = context.filesDir.parentFile!!
    private fun getLocalDir(context: Context): File = File(getPrefixDir(context), "local").apply { mkdirs() }
    private fun getBinDir(context: Context): File = File(getLocalDir(context), "bin").apply { mkdirs() }
    
    private fun buildProotCommand(context: Context, command: Array<String>): List<String> {
        val prefixDir = getPrefixDir(context)
        val distroDir = File(prefixDir, "local/sandbox")
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val libProot = File(nativeLibDir, "libproot.so")
        val prootExec = if (libProot.exists()) libProot.absolutePath else File(getBinDir(context), "proot").absolutePath

        val args = mutableListOf<String>()
        args.add(prootExec)
        args.add("--kill-on-exit")
        args.add("--link2symlink")
        args.add("--sysvipc")
        args.add("-L")
        args.add("-0")

        val mounts = listOf("/proc", "/sys", "/dev", "/data", "/storage", "/system")
        mounts.forEach {
            if (File(it).exists()) {
                args.add("-b")
                args.add(it)
            }
        }

        val tmpDir = File(distroDir, "tmp").apply { mkdirs() }
        args.add("-b")
        args.add("${tmpDir.absolutePath}:/dev/shm")

        val rootHome = File(distroDir, "root")
        if (!rootHome.exists()) {
            rootHome.mkdirs()
        }
        args.add("-b")
        args.add("${rootHome.absolutePath}:/root")
        args.add("-b")
        args.add(context.filesDir.absolutePath)
        args.add("-r")
        args.add(distroDir.absolutePath)
        args.add("-w")
        args.add("/root")

        args.add("/usr/bin/env")
        args.add("-i")
        args.add("HOME=/root")
        args.add("PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
        args.add("LANG=C.UTF-8")
        args.add("TERM=xterm-256color")
        args.add("TMPDIR=/tmp")

        args.addAll(command)
        return args
    }

    private fun getProotEnv(context: Context): Map<String, String> {
        val env = mutableMapOf<String, String>()
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val prootTmpDir = File(context.filesDir, "usr/tmp").apply { mkdirs() }
        prootTmpDir.setReadable(true, false)
        prootTmpDir.setWritable(true, false)
        prootTmpDir.setExecutable(true, false)

        env["PROOT_TMP_DIR"] = prootTmpDir.absolutePath
        env["TMPDIR"] = prootTmpDir.absolutePath
        env["TMP_DIR"] = prootTmpDir.absolutePath
        val libPath = "${context.filesDir.absolutePath}:${context.filesDir.absolutePath}/local/lib:$nativeLibDir"
        env["LD_LIBRARY_PATH"] = libPath

        val loader64 = listOf(File(nativeLibDir, "libproot-loader.so"), File(nativeLibDir, "libloader.so")).firstOrNull { it.exists() }
        val loader32 = listOf(File(nativeLibDir, "libproot-loader32.so"), File(nativeLibDir, "libloader32.so")).firstOrNull { it.exists() }

        if (loader64 != null) {
            loader64.setExecutable(true, false)
            env["PROOT_LOADER"] = loader64.absolutePath
        }
        if (loader32 != null) {
            loader32.setExecutable(true, false)
            env["PROOT_LOADER32"] = loader32.absolutePath
            env["PROOT_LOADER_32"] = loader32.absolutePath
        }
        return env
    }

    override suspend fun getTasks(context: Context, projectPath: String, forceRefresh: Boolean): List<GradleTask> {
        if (!forceRefresh && tasksCache.containsKey(projectPath)) {
            val cached = tasksCache[projectPath]
            if (!cached.isNullOrEmpty()) {
                return cached
            }
        }

        val tasksList = mutableListOf<GradleTask>()
        val prefixDir = context.filesDir.parentFile!!
        val sandboxDir = File(prefixDir, "local/sandbox")
        
        val javaHomeInContainer = when {
            File(sandboxDir, "usr/lib/jvm/java-21-openjdk").exists() -> "/usr/lib/jvm/java-21-openjdk"
            File(sandboxDir, "usr/lib/jvm/java-21-openjdk-amd64").exists() -> "/usr/lib/jvm/java-21-openjdk-amd64"
            File(sandboxDir, "usr/lib/jvm/java-17-openjdk").exists() -> "/usr/lib/jvm/java-17-openjdk"
            File(sandboxDir, "usr/lib/jvm/java-17-openjdk-amd64").exists() -> "/usr/lib/jvm/java-17-openjdk-amd64"
            else -> ""
        }

        val cleanProjectPath = projectPath.trim()
        val javaHomeExport = if (javaHomeInContainer.isNotEmpty()) "export JAVA_HOME=$javaHomeInContainer && " else ""
        val gradlewFile = File(cleanProjectPath, "gradlew")
        val compileCmd = if (gradlewFile.exists()) {
            "${javaHomeExport}cd \"$cleanProjectPath\" && bash ./gradlew tasks --all"
        } else {
            "${javaHomeExport}cd \"$cleanProjectPath\" && gradle tasks --all"
        }

        val cmd = buildProotCommand(context, arrayOf("sh", "-c", compileCmd))
        
        try {
            val processBuilder = ProcessBuilder(cmd)
            processBuilder.directory(File(cleanProjectPath))
            processBuilder.environment().putAll(getProotEnv(context))
            val process = processBuilder.start()
            
            process.inputStream.bufferedReader().useLines { lines ->
                var currentGroup = "Other"
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) return@forEach
                    if (line.startsWith("---")) return@forEach
                    
                    if (trimmed.contains(" - ")) {
                        val parts = trimmed.split(" - ", limit = 2)
                        val taskName = parts[0].trim()
                        val taskDesc = parts[1].trim()
                        tasksList.add(GradleTask(name = taskName, description = taskDesc, group = currentGroup))
                    } else if (!line.startsWith(" ") && line.endsWith("tasks")) {
                        currentGroup = line.trim()
                    } else if (trimmed.matches(Regex("^[a-zA-Z0-9:]+$"))) {
                        tasksList.add(GradleTask(name = trimmed, description = null, group = currentGroup))
                    }
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val result = tasksList.filter { 
            it.name.isNotEmpty() && 
            !it.name.contains(" ") && 
            !it.name.startsWith(":") &&
            it.name != "tasks"
        }.distinctBy { it.name }

        if (result.isNotEmpty()) {
            tasksCache[projectPath] = result
        }

        return result
    }

    override fun runTasks(
        context: Context,
        projectPath: String,
        taskNames: List<String>,
        flags: List<String>
    ): Flow<GradleLogLine> = flow {
        val tasksString = taskNames.joinToString(" ")
        val flagsString = flags.joinToString(" ")
        val fullArgs = listOf(tasksString, flagsString).filter { it.isNotBlank() }.joinToString(" ")

        val prefixDir = context.filesDir.parentFile!!
        val sandboxDir = File(prefixDir, "local/sandbox")
        
        val javaHomeInContainer = when {
            File(sandboxDir, "usr/lib/jvm/java-21-openjdk").exists() -> "/usr/lib/jvm/java-21-openjdk"
            File(sandboxDir, "usr/lib/jvm/java-21-openjdk-amd64").exists() -> "/usr/lib/jvm/java-21-openjdk-amd64"
            File(sandboxDir, "usr/lib/jvm/java-17-openjdk").exists() -> "/usr/lib/jvm/java-17-openjdk"
            File(sandboxDir, "usr/lib/jvm/java-17-openjdk-amd64").exists() -> "/usr/lib/jvm/java-17-openjdk-amd64"
            else -> ""
        }

        val cleanProjectPath = projectPath.trim()
        val javaHomeExport = if (javaHomeInContainer.isNotEmpty()) "export JAVA_HOME=$javaHomeInContainer && " else ""
        val gradlewFile = File(cleanProjectPath, "gradlew")
        val compileCmd = if (gradlewFile.exists()) {
            "${javaHomeExport}cd \"$cleanProjectPath\" && bash ./gradlew $fullArgs"
        } else {
            "${javaHomeExport}cd \"$cleanProjectPath\" && gradle $fullArgs"
        }

        val cmd = buildProotCommand(context, arrayOf("sh", "-c", compileCmd))
        
        var lineNum = 1
        val startLine = "Starting Gradle execution: $fullArgs"
        val startLog = GradleLogLine.parse(lineNum++, startLine)
        val pathForLog = cleanProjectPath.replace(" ", "[SPACE]")
        ToolingLogManagerImpl.log(com.scto.mobile.ide.core.tooling.api.ToolingLogCategory.BUILD, "INFO", "Executing in working directory \"$pathForLog\": $startLine")

        try {
            val processBuilder = ProcessBuilder(cmd)
            processBuilder.directory(File(cleanProjectPath))
            processBuilder.environment().putAll(getProotEnv(context))
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            
            process.inputStream.bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val parsed = GradleLogLine.parse(lineNum++, line)
                    emit(parsed)
                    val levelStr = when (parsed.level) {
                        GradleLogLevel.ERROR -> "ERROR"
                        GradleLogLevel.WARN -> "WARN"
                        else -> "INFO"
                    }
                    ToolingLogManagerImpl.log(
                        com.scto.mobile.ide.core.tooling.api.ToolingLogCategory.BUILD,
                        levelStr,
                        line
                    )
                    line = reader.readLine()
                }
            }
            val exitCode = process.waitFor()
            val endLine = "Execution finished with exit code: $exitCode"
            val endLog = GradleLogLine.parse(lineNum++, endLine)
            emit(endLog)
            ToolingLogManagerImpl.log(com.scto.mobile.ide.core.tooling.api.ToolingLogCategory.BUILD, if (exitCode == 0) "INFO" else "ERROR", endLine)
        } catch (e: Exception) {
            val errLine = "Error executing tasks: ${e.message}"
            val errLog = GradleLogLine.parse(lineNum++, errLine)
            emit(errLog)
            ToolingLogManagerImpl.log(com.scto.mobile.ide.core.tooling.api.ToolingLogCategory.BUILD, "ERROR", errLine)
        }
    }
}
