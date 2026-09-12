package com.picbumper.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.picbumper.data.SettingsRepository
import com.picbumper.domain.model.BumpSettings
import com.picbumper.domain.model.ExternalDeleteMode
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

    fun setOverrideDateAdded(enabled: Boolean) {
        viewModelScope.launch { repository.updateOverrideDateAdded(enabled) }
    }

    fun setOverrideDateModified(enabled: Boolean) {
        viewModelScope.launch { repository.updateOverrideDateModified(enabled) }
    }

    fun setOverrideDateTaken(enabled: Boolean) {
        viewModelScope.launch { repository.updateOverrideDateTaken(enabled) }
    }

    fun setOverrideExif(enabled: Boolean) {
        viewModelScope.launch { repository.updateOverrideExif(enabled) }
    }

    fun setExternalDeleteMode(mode: ExternalDeleteMode) {
        viewModelScope.launch { repository.updateExternalDeleteMode(mode) }
    }

    fun setAlbumName(name: String) {
        viewModelScope.launch { repository.updateAlbumName(name) }
    }
}
