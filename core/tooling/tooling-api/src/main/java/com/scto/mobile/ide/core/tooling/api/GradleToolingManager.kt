package com.scto.mobile.ide.core.tooling.api

import kotlinx.coroutines.flow.Flow

/**
 * High-level interface for all Gradle tooling operations.
 *
 * Designed for use over an IPC boundary (Unix Domain Socket) between the
 * Android UI process (client) and the Linux Gradle worker process (server).
 */
interface GradleToolingManager {

    /**
     * Fetches all available Gradle task paths for the project at [projectPath].
     * Emits [Resource.Loading] → [Resource.Success] or [Resource.Error].
     */
    fun fetchAvailableTasks(projectPath: String): Flow<Resource<List<String>>>

    /**
     * Executes the given [tasks] in the project at [projectPath].
     * Streams [GradleLogEvent]s until [GradleLogEvent.Result] is emitted.
     */
    fun executeTasks(projectPath: String, tasks: List<String>): Flow<GradleLogEvent>
}
