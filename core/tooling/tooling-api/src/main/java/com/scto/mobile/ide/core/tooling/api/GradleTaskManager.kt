package com.scto.mobile.ide.core.tooling.api

import android.content.Context
import kotlinx.coroutines.flow.Flow

data class GradleTask(
    val name: String,
    val description: String? = null,
    val group: String? = null
)

interface GradleTaskManager {
    suspend fun getTasks(context: Context, projectPath: String): List<GradleTask>
    fun runTasks(context: Context, projectPath: String, taskNames: List<String>): Flow<String>
}
