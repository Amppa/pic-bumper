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
    val systemWritePendingUris: List<Uri>? = null,
    val pendingRenameAction: Pair<Uri, String>? = null,
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

    fun clearMultiSelect() {
        _uiState.update {
            it.copy(
                isMultiSelectMode = false,
                checkedItemUris = emptySet()
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
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

            // Handle fallback delete failures for Directory A items via system delete request
            val internalPendingUris = result.internalFailedDeleteUris.mapNotNull { bumperRepository.toMediaStoreUri(it) }

            // Ask for confirmation only when external images are bumped
            if (result.externalUrisToAsk.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        isMultiSelectMode = false,
                        checkedItemUris = emptySet(),
                        statusMessage = "已將 ${result.bumpedUris.size} 張照片置頂",
                        externalUrisToAskDelete = result.externalUrisToAsk,
                        systemDeletePendingUris = internalPendingUris.ifEmpty { null }
                    )
                }
            } else {
                if (internalPendingUris.isNotEmpty()) {
                    _uiState.update {
                        it.copy(systemDeletePendingUris = internalPendingUris)
                    }
                }
                finishBumpCycle(result.bumpedUris.size)
            }
        }
    }

    fun onConfirmExternalDelete() {
        val uris = _uiState.value.externalUrisToAskDelete ?: return
        _uiState.update { it.copy(externalUrisToAskDelete = null) }

        viewModelScope.launch {
            val remainingUris = mutableListOf<Uri>()

            for (uri in uris) {
                val deleted = bumperRepository.deleteExternalOriginal(uri)
                if (!deleted) {
                    remainingUris.add(uri)
                }
            }

            if (remainingUris.isEmpty()) {
                _uiState.update { it.copy(statusMessage = "原圖已成功刪除") }
            } else {
                val mediaStoreUris = remainingUris.mapNotNull { bumperRepository.toMediaStoreUri(it) }
                if (mediaStoreUris.isNotEmpty()) {
                    _uiState.update { it.copy(systemDeletePendingUris = mediaStoreUris) }
                } else {
                    _uiState.update { it.copy(statusMessage = "已置頂，但部分原圖受系統保護無法刪除") }
                }
            }
        }
    }

    fun onDismissExternalDelete() {
        _uiState.update { it.copy(externalUrisToAskDelete = null) }
    }

    fun onSystemDeleteFinished(success: Boolean = true) {
        _uiState.update {
            it.copy(
                systemDeletePendingUris = null,
                statusMessage = if (success) "原圖已成功刪除" else "已置頂，但部分原圖受系統保護無法刪除"
            )
        }
        loadAlbumImages()
    }

    /**
     * Delete checked items from album. Self-owned items in Directory A are deleted silently.
     * Items from previous installations or external sources will trigger system delete confirmation.
     */
    fun deleteCheckedItems() {
        val checkedUris = _uiState.value.checkedItemUris
        if (checkedUris.isEmpty()) return

        viewModelScope.launch {
            val contentResolver = getApplication<Application>().contentResolver
            val failedUris = mutableListOf<Uri>()
            checkedUris.forEach { uri ->
                try {
                    val rows = contentResolver.delete(uri, null, null)
                    if (rows <= 0) {
                        failedUris.add(uri)
                    }
                } catch (_: Exception) {
                    failedUris.add(uri)
                }
            }

            if (failedUris.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        checkedItemUris = emptySet(),
                        isMultiSelectMode = false,
                        systemDeletePendingUris = failedUris
                    )
                }
            } else {
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
    }

    fun renameSelectedItem(inputName: String) {
        val checkedUris = _uiState.value.checkedItemUris
        if (checkedUris.size != 1) return
        val targetUri = checkedUris.first()
        val targetItem = _uiState.value.albumItems.find { it.uri == targetUri } ?: return

        val trimmedName = inputName.trim()
        if (trimmedName.isBlank()) return

        // Preserve file extension if user did not include it
        val oldExt = targetItem.displayName.substringAfterLast('.', "")
        val finalName = if (oldExt.isNotEmpty() && !trimmedName.endsWith(".$oldExt", ignoreCase = true)) {
            "$trimmedName.$oldExt"
        } else {
            trimmedName
        }

        viewModelScope.launch {
            val success = bumperRepository.renameImage(targetUri, finalName)
            if (success) {
                _uiState.update {
                    it.copy(
                        checkedItemUris = emptySet(),
                        isMultiSelectMode = false,
                        statusMessage = "已將檔案重命名為 $finalName"
                    )
                }
                loadAlbumImages()
            } else {
                val mediaStoreUri = bumperRepository.toMediaStoreUri(targetUri) ?: targetUri
                _uiState.update {
                    it.copy(
                        systemWritePendingUris = listOf(mediaStoreUri),
                        pendingRenameAction = Pair(targetUri, finalName)
                    )
                }
            }
        }
    }

    fun onSystemWriteFinished(success: Boolean) {
        val pendingAction = _uiState.value.pendingRenameAction
        _uiState.update {
            it.copy(
                systemWritePendingUris = null,
                pendingRenameAction = null
            )
        }

        if (success && pendingAction != null) {
            viewModelScope.launch {
                val (targetUri, finalName) = pendingAction
                val renamed = bumperRepository.renameImage(targetUri, finalName)
                if (renamed) {
                    _uiState.update {
                        it.copy(
                            checkedItemUris = emptySet(),
                            isMultiSelectMode = false,
                            statusMessage = "已將檔案重命名為 $finalName"
                        )
                    }
                    loadAlbumImages()
                } else {
                    _uiState.update {
                        it.copy(statusMessage = "重命名失敗，請確認檔案存取權限")
                    }
                }
            }
        } else if (!success) {
            _uiState.update {
                it.copy(statusMessage = "重命名已取消或失敗")
            }
        }
    }

    private fun finishBumpCycle(bumpedCount: Int) {
        _uiState.update {
            it.copy(
                isProcessing = false,
                isMultiSelectMode = false,
                checkedItemUris = emptySet(),
                statusMessage = "已成功置頂 $bumpedCount 張照片"
            )
        }
    }
}
