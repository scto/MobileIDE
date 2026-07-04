package com.scto.mobile.ide.core.tooling.server

import com.scto.mobile.ide.core.tooling.api.IpcProtocol
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.GradleProject
import java.io.File

/**
 * Uses Eclipse Buildship's Gradle Tooling API to recursively extract all
 * available task paths (e.g. ':app:assembleDebug') from a given project directory.
 *
 * Runs on the Linux/JVM side (proot Ubuntu) — NOT on Android.
 */
object GradleModelReader {

    /**
     * Returns a flat, sorted list of all task paths for the project at [projectPath].
     * Recursively traverses all subprojects.
     *
     * Example: [":tasks", ":app:assembleDebug", ":core:library:jar"]
     */
    fun fetchTaskPaths(projectPath: String): List<String> {
        val connector = GradleConnector.newConnector()
            .forProjectDirectory(File(projectPath))
        connector.connect().use { connection ->
            val model = connection.getModel(GradleProject::class.java)
            return collectTaskPaths(model)
        }
    }

    private fun collectTaskPaths(project: GradleProject, prefix: String = ""): List<String> {
        val paths = mutableListOf<String>()
        val projectPath = if (prefix.isEmpty()) ":" else prefix

        for (task in project.tasks) {
            val taskPath = if (projectPath == ":") ":${task.name}" else "$projectPath:${task.name}"
            paths.add(taskPath)
        }
        for (child in project.children) {
            val childPath = if (projectPath == ":") ":${child.name}" else "$projectPath:${child.name}"
            paths.addAll(collectTaskPaths(child, childPath))
        }
        return paths.sorted()
    }
}
