package com.picbumper.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.picbumper.data.BumpResult
import com.picbumper.data.MediaBumperRepository
import com.picbumper.data.SettingsRepository
import com.picbumper.domain.model.BumpSettings
import com.picbumper.domain.model.ImageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val RECENT_THRESHOLD_SECONDS = 30 * 60L // 30 minutes

data class HomeUiState(
    val albumItems: List<ImageItem> = emptyList(),
    val checkedItemUris: Set<Uri> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val externalUrisToAskDelete: List<Uri>? = null,
    val systemDeletePendingUris: List<Uri>? = null,
    val lastRefreshedAt: Long = System.currentTimeMillis()
) {
    val recentItems: List<ImageItem>
        get() {
            val threshold = (lastRefreshedAt / 1000) - RECENT_THRESHOLD_SECONDS
            return albumItems.filter { it.dateModified >= threshold }
        }

    val olderItems: List<ImageItem>
        get() {
            val threshold = (lastRefreshedAt / 1000) - RECENT_THRESHOLD_SECONDS
            return albumItems.filter { it.dateModified < threshold }
        }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val bumperRepository = MediaBumperRepository(application)
    private val settingsRepository = SettingsRepository(application)

    val settings: StateFlow<BumpSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BumpSettings()
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { currentSettings ->
                loadAlbumImages(currentSettings.albumName)
            }
        }
    }

    fun loadAlbumImages(albumName: String = settings.value.albumName, showFeedback: Boolean = false) {
        viewModelScope.launch {
            val items = bumperRepository.loadAlbumImages(albumName)
            _uiState.update {
                it.copy(
                    albumItems = items,
                    checkedItemUris = emptySet(),
                    isMultiSelectMode = false,
                    lastRefreshedAt = System.currentTimeMillis(),
                    statusMessage = if (showFeedback) "已重新整理相簿時序" else it.statusMessage
                )
            }
        }
    }

    /**
     * Triggered when the user taps the floating [+] button and selects external images.
     * Bumps them directly into the dedicated album directory, then refreshes the gallery.
     */
    fun onNewImagesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = null) }
            val currentSettings = settings.value
            val resolvedItems = bumperRepository.resolveImageItems(uris, currentSettings.albumName)
            bumpItemsInternal(resolvedItems)
        }
    }

    fun toggleItemCheck(uri: Uri) {
        _uiState.update { current ->
            val newChecked = current.checkedItemUris.toMutableSet()
            if (newChecked.contains(uri)) {
                newChecked.remove(uri)
            } else {
                newChecked.add(uri)
            }
            current.copy(
                checkedItemUris = newChecked,
                isMultiSelectMode = newChecked.isNotEmpty()
            )
        }
    }

    fun startMultiSelect(initialUri: Uri) {
        _uiState.update {
            it.copy(
                isMultiSelectMode = true,
                checkedItemUris = setOf(initialUri)
            )
        }
    }

    fun clearMultiSelect() {
        _uiState.update {
            it.copy(
                isMultiSelectMode = false,
                checkedItemUris = emptySet()
            )
        }
    }

    fun bumpSingleItem(item: ImageItem) {
        bumpItemsInternal(listOf(item))
    }

    fun bumpCheckedItems() {
        val checkedUris = _uiState.value.checkedItemUris
        val itemsToBump = _uiState.value.albumItems.filter { checkedUris.contains(it.uri) }
        if (itemsToBump.isNotEmpty()) {
            bumpItemsInternal(itemsToBump)
        }
    }

    private fun bumpItemsInternal(items: List<ImageItem>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = null) }
            val currentSettings = settings.first()
            val result = bumperRepository.bumpImages(items, currentSettings)

            // Refresh album view immediately so the newly bumped image is at the top
            loadAlbumImages(currentSettings.albumName)

            // Always ask for confirmation when external images are bumped
            if (result.externalUrisToAsk.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        isMultiSelectMode = false,
                        checkedItemUris = emptySet(),
                        statusMessage = "已將 ${result.bumpedUris.size} 張圖片推至最前",
                        externalUrisToAskDelete = result.externalUrisToAsk
                    )
                }
            } else {
                finishBumpCycle(result.bumpedUris.size)
            }
        }
    }

    fun onConfirmExternalDelete() {
        val uris = _uiState.value.externalUrisToAskDelete
        _uiState.update { it.copy(externalUrisToAskDelete = null) }
        if (!uris.isNullOrEmpty()) {
            _uiState.update { it.copy(systemDeletePendingUris = uris) }
        }
    }

    fun onDismissExternalDelete() {
        _uiState.update { it.copy(externalUrisToAskDelete = null) }
    }

    fun onSystemDeleteFinished() {
        _uiState.update { it.copy(systemDeletePendingUris = null) }
        loadAlbumImages()
    }

    /**
     * Delete checked items from album. Self-owned items in Directory A are deleted silently.
     */
    fun deleteCheckedItems() {
        val checkedUris = _uiState.value.checkedItemUris
        if (checkedUris.isEmpty()) return

        viewModelScope.launch {
            val contentResolver = getApplication<Application>().contentResolver
            checkedUris.forEach { uri ->
                try {
                    contentResolver.delete(uri, null, null)
                } catch (_: Exception) {}
            }
            _uiState.update {
                it.copy(
                    checkedItemUris = emptySet(),
                    isMultiSelectMode = false,
                    statusMessage = "已刪除選取的圖片"
                )
            }
            loadAlbumImages()
        }
    }

    private fun finishBumpCycle(bumpedCount: Int) {
        _uiState.update {
            it.copy(
                isProcessing = false,
                isMultiSelectMode = false,
                checkedItemUris = emptySet(),
                statusMessage = "⚡ 已成功將 $bumpedCount 張圖片時序推至現在（最新）！"
            )
        }
    }
}
