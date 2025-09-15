package com.mobileide.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mobileide.settings.presentation.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Definiert den DataStore für die Einstellungen
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

interface SettingsRepository {
    suspend fun appTheme(): Flow<AppTheme?>
    
    suspend fun setTheme(theme: AppTheme)
    
    suspend fun useDynamicColor(): Flow<Boolean?>
    
    suspend fun setUseDynamicColor(useDynamic: Boolean)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val THEME_KEY = stringPreferencesKey("app_theme")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")

    suspend fun appTheme(): Flow<AppTheme?> = context.settingsDataStore.data.map { preferences ->
            AppTheme.valueOf(preferences[THEME_KEY] ?: AppTheme.SYSTEM.name)
        }

    suspend fun setTheme(theme: AppTheme) {
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
    
    suspend fun useDynamicColor():Flow<Boolean> = context.settingsDataStore.data.map { preferenc ->
            preferences[DYNAMIC_COLOR_KEY] ?: true // Standardmäßig aktiviert
    }

    suspend fun setUseDynamicColor(useDynamic: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[DYNAMIC_COLOR_KEY] = useDynamic
        }
    }
}