package com.mobileide.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Definiert den DataStore auf Top-Level-Ebene für den Zugriff.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "editor_session")

@Singleton
class SessionRepository @Inject constructor(@ApplicationContext private val context: Context) {

    // Schlüssel für die Speicherung der Daten in DataStore.
    private val OPEN_FILES_KEY = stringSetPreferencesKey("open_files_paths")
    private val ACTIVE_FILE_INDEX_KEY = intPreferencesKey("active_file_index")

    /**
     * Speichert die aktuelle Sitzung: die Liste der offenen Dateipfade und den aktiven Index.
     */
    suspend fun saveSession(paths: List<String>, activeIndex: Int) {
        context.dataStore.edit { preferences ->
            preferences[OPEN_FILES_KEY] = paths.toSet()
            preferences[ACTIVE_FILE_INDEX_KEY] = activeIndex
        }
    }

    /**
     * Stellt einen Flow bereit, der die Liste der zuletzt geöffneten Dateipfade emittiert.
     * Der EditorViewModel kann diesen Flow beobachten.
     */
    val openFilesPaths: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[OPEN_FILES_KEY]?.toList() ?: emptyList()
    }
    
    /**
     * Stellt einen Flow bereit, der den Index der zuletzt aktiven Datei emittiert.
     */
    val activeFileIndex: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_FILE_INDEX_KEY] ?: -1
    }
}
