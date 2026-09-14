package com.picbumper.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.picbumper.data.SettingsRepository
import com.picbumper.domain.model.BumpSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<BumpSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BumpSettings()
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
        viewModelScope.launch { repository.updateOverrideDateAdded(enabled) }
    }

    fun setOverrideDateModified(enabled: Boolean) {
        val s = settings.value
        if (!enabled && !canDisable(s.overrideDateAdded, s.overrideDateTaken, s.overrideExif)) {
            return // Guard: At least one must stay enabled
        }
        viewModelScope.launch { repository.updateOverrideDateModified(enabled) }
    }

    fun setOverrideDateTaken(enabled: Boolean) {
        val s = settings.value
        if (!enabled && !canDisable(s.overrideDateAdded, s.overrideDateModified, s.overrideExif)) {
            return // Guard: At least one must stay enabled
        }
        viewModelScope.launch { repository.updateOverrideDateTaken(enabled) }
    }

    fun setOverrideExif(enabled: Boolean) {
        val s = settings.value
        if (!enabled && !canDisable(s.overrideDateAdded, s.overrideDateModified, s.overrideDateTaken)) {
            return // Guard: At least one must stay enabled
        }
        viewModelScope.launch { repository.updateOverrideExif(enabled) }
    }

    fun setAlbumName(name: String) {
        viewModelScope.launch { repository.updateAlbumName(name) }
    }

    fun setSilentRename(enabled: Boolean) {
        viewModelScope.launch { repository.updateSilentRename(enabled) }
    }
}

