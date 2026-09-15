package com.pictotop.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pictotop.data.SettingsRepository
import com.pictotop.domain.model.AlbumSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = SettingsRepository(application)

    val settings: StateFlow<AlbumSettings> = mediaRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumSettings()
    )

    /**
     * Helper to verify if at least one checkbox remains checked.
     */
    private fun canDisable(vararg otherFlags: Boolean): Boolean {
        return otherFlags.any { it }
    }

    fun setOverrideDateAdded(enabled: Boolean) {
        val s = settings.value
        if (!enabled && !canDisable(s.overrideDateModified, s.overrideDateTaken, s.overrideExif)) {
            return // Guard: At least one must stay enabled
        }
        viewModelScope.launch { mediaRepository.updateOverrideDateAdded(enabled) }
    }

    fun setOverrideDateModified(enabled: Boolean) {
        val s = settings.value
        if (!enabled && !canDisable(s.overrideDateAdded, s.overrideDateTaken, s.overrideExif)) {
            return // Guard: At least one must stay enabled
        }
        viewModelScope.launch { mediaRepository.updateOverrideDateModified(enabled) }
    }

    fun setOverrideDateTaken(enabled: Boolean) {
        val s = settings.value
        if (!enabled && !canDisable(s.overrideDateAdded, s.overrideDateModified, s.overrideExif)) {
            return // Guard: At least one must stay enabled
        }
        viewModelScope.launch { mediaRepository.updateOverrideDateTaken(enabled) }
    }

    fun setOverrideExif(enabled: Boolean) {
        val s = settings.value
        if (!enabled && !canDisable(s.overrideDateAdded, s.overrideDateModified, s.overrideDateTaken)) {
            return // Guard: At least one must stay enabled
        }
        viewModelScope.launch { mediaRepository.updateOverrideExif(enabled) }
    }

    fun setAlbumName(name: String) {
        viewModelScope.launch { mediaRepository.updateAlbumName(name) }
    }

    fun setSilentRename(enabled: Boolean) {
        viewModelScope.launch { mediaRepository.updateSilentRename(enabled) }
    }
}

