package com.picbumper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.picbumper.domain.model.BumpSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pic_bumper_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val OVERRIDE_DATE_ADDED = booleanPreferencesKey("override_date_added")
        val OVERRIDE_DATE_MODIFIED = booleanPreferencesKey("override_date_modified")
        val OVERRIDE_DATE_TAKEN = booleanPreferencesKey("override_date_taken")
        val OVERRIDE_EXIF = booleanPreferencesKey("override_exif")
        val ALBUM_NAME = stringPreferencesKey("album_name")
    }

    val settingsFlow: Flow<BumpSettings> = context.dataStore.data.map { preferences ->
        val overrideDateAdded = preferences[PreferencesKeys.OVERRIDE_DATE_ADDED] ?: true
        val overrideDateModified = preferences[PreferencesKeys.OVERRIDE_DATE_MODIFIED] ?: true
        val overrideDateTaken = preferences[PreferencesKeys.OVERRIDE_DATE_TAKEN] ?: true
        val overrideExif = preferences[PreferencesKeys.OVERRIDE_EXIF] ?: true
        val albumName = preferences[PreferencesKeys.ALBUM_NAME] ?: "PicBumper"

        BumpSettings(
            overrideDateAdded = overrideDateAdded,
            overrideDateModified = overrideDateModified,
            overrideDateTaken = overrideDateTaken,
            overrideExif = overrideExif,
            albumName = albumName
        )
    }

    suspend fun updateOverrideDateAdded(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERRIDE_DATE_ADDED] = enabled
        }
    }

    suspend fun updateOverrideDateModified(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERRIDE_DATE_MODIFIED] = enabled
        }
    }

    suspend fun updateOverrideDateTaken(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERRIDE_DATE_TAKEN] = enabled
        }
    }

    suspend fun updateOverrideExif(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERRIDE_EXIF] = enabled
        }
    }

    suspend fun updateAlbumName(name: String) {
        val sanitized = name.trim().ifEmpty { "PicBumper" }
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALBUM_NAME] = sanitized
        }
    }
}
