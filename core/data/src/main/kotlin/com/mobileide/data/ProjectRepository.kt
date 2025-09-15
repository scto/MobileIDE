package com.mobileide.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.projectDataStore: DataStore<Preferences> by preferencesDataStore(name = "project_session")

interface ProjectRepository {
    fun getCurrentProject(): Flow<String?>
    suspend fun setCurrentProject(path: String)
    fun getRecentProjects(): Flow<List<String>>
    suspend fun clearCurrentProject()
}

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ProjectRepository {

    private val CURRENT_PROJECT_KEY = stringPreferencesKey("current_project_path")
    private val RECENT_PROJECTS_KEY = stringSetPreferencesKey("recent_projects_paths")

    override fun getCurrentProject(): Flow<String?> = context.projectDataStore.data
        .map { preferences ->
            preferences[CURRENT_PROJECT_KEY]
        }

    override suspend fun setCurrentProject(path: String) {
        context.projectDataStore.edit { preferences ->
            preferences[CURRENT_PROJECT_KEY] = path
            val recent = preferences[RECENT_PROJECTS_KEY] ?: emptySet()
            preferences[RECENT_PROJECTS_KEY] = (recent + path).take(10).toSet() // speichert die letzten 10
        }
    }

    override fun getRecentProjects(): Flow<List<String>> = context.projectDataStore.data
        .map { preferences ->
            (preferences[RECENT_PROJECTS_KEY] ?: emptySet()).toList()
        }
        
    override suspend fun clearCurrentProject() {
        context.projectDataStore.edit { preferences ->
            preferences.remove(CURRENT_PROJECT_KEY)
        }
    }
}
