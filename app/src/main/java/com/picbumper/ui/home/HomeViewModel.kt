package com.picbumper.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.picbumper.data.BumpResult
import com.picbumper.data.MediaBumperRepository
import com.picbumper.data.SettingsRepository
import com.picbumper.domain.model.BumpSettings
import com.picbumper.domain.model.ExternalDeleteMode
import com.picbumper.domain.model.ImageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val selectedItems: List<ImageItem> = emptyList(),
    val checkedItemUris: Set<Uri> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val externalUrisToAskDelete: List<Uri>? = null,
    val systemDeletePendingUris: List<Uri>? = null
)

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

    fun onImagesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = null) }
            val currentSettings = settings.value
            val resolvedItems = bumperRepository.resolveImageItems(uris, currentSettings.albumName)
            _uiState.update {
                it.copy(
                    selectedItems = resolvedItems,
                    checkedItemUris = emptySet(),
                    isMultiSelectMode = false,
                    isProcessing = false,
                    statusMessage = "已載入 ${resolvedItems.size} 張圖片，可單擊推進或長按複選"
                )
            }
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
        bumpItems(listOf(item))
    }

    fun bumpCheckedItems() {
        val checkedUris = _uiState.value.checkedItemUris
        val itemsToBump = _uiState.value.selectedItems.filter { checkedUris.contains(it.uri) }
        if (itemsToBump.isNotEmpty()) {
            bumpItems(itemsToBump)
        }
    }

    fun bumpAllItems() {
        val items = _uiState.value.selectedItems
        if (items.isNotEmpty()) {
            bumpItems(items)
        }
    }

    private fun bumpItems(items: List<ImageItem>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = null) }
            val currentSettings = settings.first()
            val result = bumperRepository.bumpImages(items, currentSettings)

            // Evaluate external image deletion policy
            when (currentSettings.externalDeleteMode) {
                ExternalDeleteMode.ASK -> {
                    if (result.externalUrisToAsk.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                isMultiSelectMode = false,
                                checkedItemUris = emptySet(),
                                statusMessage = "已推進 ${result.bumpedUris.size} 張圖片至相簿第一位",
                                externalUrisToAskDelete = result.externalUrisToAsk
                            )
                        }
                    } else {
                        finishBumpCycle(result.bumpedUris.size)
                    }
                }
                ExternalDeleteMode.ALWAYS_DELETE -> {
                    if (result.externalUrisToAsk.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                isMultiSelectMode = false,
                                checkedItemUris = emptySet(),
                                statusMessage = "已推進 ${result.bumpedUris.size} 張圖片至相簿第一位",
                                systemDeletePendingUris = result.externalUrisToAsk
                            )
                        }
                    } else {
                        finishBumpCycle(result.bumpedUris.size)
                    }
                }
                ExternalDeleteMode.ALWAYS_KEEP -> {
                    finishBumpCycle(result.bumpedUris.size)
                }
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
    }

    fun removeCheckedItemsFromList() {
        val checkedUris = _uiState.value.checkedItemUris
        _uiState.update { current ->
            val remaining = current.selectedItems.filterNot { checkedUris.contains(it.uri) }
            current.copy(
                selectedItems = remaining,
                checkedItemUris = emptySet(),
                isMultiSelectMode = false,
                statusMessage = "已移除選取項目"
            )
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
