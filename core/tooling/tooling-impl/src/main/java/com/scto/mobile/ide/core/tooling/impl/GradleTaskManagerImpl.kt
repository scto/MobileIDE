package com.scto.mobile.ide.core.tooling.impl

import android.content.Context
import com.scto.mobile.ide.core.tooling.api.GradleTask
import com.scto.mobile.ide.core.tooling.api.GradleTaskManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

object GradleTaskManagerImpl : GradleTaskManager {

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
        env["PROOT_TMP_DIR"] = context.cacheDir.absolutePath
        env["TMPDIR"] = context.cacheDir.absolutePath
        val libPath = "${context.filesDir.absolutePath}:${context.filesDir.absolutePath}/local/lib:$nativeLibDir"
        env["LD_LIBRARY_PATH"] = libPath
        if (File(nativeLibDir, "libproot-loader.so").exists()) {
            env["PROOT_LOADER"] = "$nativeLibDir/libproot-loader.so"
        }
        if (File(nativeLibDir, "libproot-loader32.so").exists()) {
            env["PROOT_LOADER32"] = "$nativeLibDir/libproot-loader32.so"
        }
        return env
    }

    override suspend fun getTasks(context: Context, projectPath: String): List<GradleTask> {
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

        val javaHomeExport = if (javaHomeInContainer.isNotEmpty()) "export JAVA_HOME=$javaHomeInContainer && " else ""
        val gradlewFile = File(projectPath, "gradlew")
        val compileCmd = if (gradlewFile.exists()) {
            "${javaHomeExport}cd $projectPath && ./gradlew tasks --all"
        } else {
            "${javaHomeExport}cd $projectPath && gradle tasks --all"
        }

        val cmd = buildProotCommand(context, arrayOf("sh", "-c", compileCmd))
        
        try {
            val processBuilder = ProcessBuilder(cmd)
            processBuilder.directory(File(projectPath))
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
        
        return tasksList.filter { 
            it.name.isNotEmpty() && 
            !it.name.contains(" ") && 
            !it.name.startsWith(":") &&
            it.name != "tasks"
        }.distinctBy { it.name }
    }

    override fun runTasks(context: Context, projectPath: String, taskNames: List<String>): Flow<String> = flow {
        val tasksString = taskNames.joinToString(" ")
        val prefixDir = context.filesDir.parentFile!!
        val sandboxDir = File(prefixDir, "local/sandbox")
        
        val javaHomeInContainer = when {
            File(sandboxDir, "usr/lib/jvm/java-21-openjdk").exists() -> "/usr/lib/jvm/java-21-openjdk"
            File(sandboxDir, "usr/lib/jvm/java-21-openjdk-amd64").exists() -> "/usr/lib/jvm/java-21-openjdk-amd64"
            File(sandboxDir, "usr/lib/jvm/java-17-openjdk").exists() -> "/usr/lib/jvm/java-17-openjdk"
            File(sandboxDir, "usr/lib/jvm/java-17-openjdk-amd64").exists() -> "/usr/lib/jvm/java-17-openjdk-amd64"
            else -> ""
        }

        val javaHomeExport = if (javaHomeInContainer.isNotEmpty()) "export JAVA_HOME=$javaHomeInContainer && " else ""
        val gradlewFile = File(projectPath, "gradlew")
        val compileCmd = if (gradlewFile.exists()) {
            "${javaHomeExport}cd $projectPath && ./gradlew $tasksString"
        } else {
            "${javaHomeExport}cd $projectPath && gradle $tasksString"
        }

        val cmd = buildProotCommand(context, arrayOf("sh", "-c", compileCmd))
        
        emit("Starting Gradle execution: $tasksString\n")
        
        try {
            val processBuilder = ProcessBuilder(cmd)
            processBuilder.directory(File(projectPath))
            processBuilder.environment().putAll(getProotEnv(context))
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            
            process.inputStream.bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    emit(line + "\n")
                    ToolingLogManagerImpl.log(
                        com.scto.mobile.ide.core.tooling.api.ToolingLogCategory.BUILD,
                        "INFO",
                        line
                    )
                    line = reader.readLine()
                }
            }
            val exitCode = process.waitFor()
            emit("Execution finished with exit code: $exitCode\n")
        } catch (e: Exception) {
            emit("Error executing tasks: ${e.message}\n")
        }
    }
}
