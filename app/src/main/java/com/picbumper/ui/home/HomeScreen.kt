package com.picbumper.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.picbumper.domain.model.ImageItem
import com.picbumper.ui.home.components.DeleteConfirmDialog
import com.picbumper.ui.home.components.EmptyAlbumView
import com.picbumper.ui.home.components.ExternalDeleteDialog
import com.picbumper.ui.home.components.FloatingStatusCapsule
import com.picbumper.ui.home.components.HomeTopBar
import com.picbumper.ui.home.components.ImageGridCard
import com.picbumper.ui.home.components.ImagePreviewDialog
import com.picbumper.ui.home.components.RenameDialog
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(checkMediaPermission(context)) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInputName by remember { mutableStateOf("") }
    var renameTargetItem by remember { mutableStateOf<ImageItem?>(null) }
    var previewItem by remember { mutableStateOf<ImageItem?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        hasPermission = granted
        if (granted) {
            viewModel.loadAlbumImages()
        }
    }

    LaunchedEffect(Unit) {
        if (!checkMediaPermission(context)) {
            permissionLauncher.launch(getRequiredPermissions())
        }
    }

    // SAF Document Picker (Launches native Android Files Manager)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.onNewImagesSelected(uris)
    }

    // Auto-dismiss status message banner after 4 seconds
    LaunchedEffect(uiState.statusMessage) {
        if (uiState.statusMessage != null) {
            delay(4000)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                isMultiSelectMode = uiState.isMultiSelectMode,
                checkedCount = uiState.checkedItemUris.size,
                albumName = settings.albumName,
                onClearSelection = { viewModel.clearMultiSelect() },
                onDeleteClick = { showDeleteConfirmDialog = true },
                onBumpClick = { viewModel.bumpCheckedItems() },
                onRefreshClick = {
                    if (!checkMediaPermission(context)) {
                        permissionLauncher.launch(getRequiredPermissions())
                    } else {
                        viewModel.loadAlbumImages(showFeedback = true)
                    }
                },
                onSettingsClick = onNavigateToSettings
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = { documentPickerLauncher.launch(arrayOf("image/*")) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(68.dp)
                    .padding(bottom = 8.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add external images to album",
                    modifier = Modifier.size(34.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.albumItems.isEmpty()) {
                    // Empty state: Pictures/<Album> is currently empty or awaiting permission
                    EmptyAlbumView(
                        albumName = settings.albumName,
                        hasPermission = hasPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(getRequiredPermissions())
                        },
                        onOpenFileManager = {
                            documentPickerLauncher.launch(arrayOf("image/*"))
                        }
                    )
                } else {
                    val recentItems = uiState.recentItems
                    val olderItems = uiState.olderItems

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 105.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Section 1: Recent Items (< 30 min)
                        if (recentItems.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "近 30 分鐘常用 (${recentItems.size} 張)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            items(recentItems, key = { it.uri }) { item ->
                                val isChecked = uiState.checkedItemUris.contains(item.uri)
                                val onClick = remember(item.uri, uiState.isMultiSelectMode) {
                                    {
                                        if (uiState.isMultiSelectMode) {
                                            viewModel.toggleItemCheck(item.uri)
                                        } else {
                                            previewItem = item
                                        }
                                    }
                                }
                                val onLongClick = remember(item.uri) {
                                    { viewModel.toggleItemCheck(item.uri) }
                                }

                                ImageGridCard(
                                    item = item,
                                    isChecked = isChecked,
                                    isMultiSelectMode = uiState.isMultiSelectMode,
                                    thumbnailSize = settings.thumbnailSize,
                                    context = context,
                                    onClick = onClick,
                                    onLongClick = onLongClick
                                )
                            }
                        }

                        // Section 2: Horizontal Divider & Older Items (>= 30 min)
                        if (olderItems.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = if (recentItems.isEmpty()) 8.dp else 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = if (recentItems.isEmpty()) "較舊照片 (${olderItems.size} 張)" else "30 分鐘內照片 (${olderItems.size} 張)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )
                                }
                            }

                            items(olderItems, key = { it.uri }) { item ->
                                val isChecked = uiState.checkedItemUris.contains(item.uri)
                                val onClick = remember(item.uri, uiState.isMultiSelectMode) {
                                    {
                                        if (uiState.isMultiSelectMode) {
                                            viewModel.toggleItemCheck(item.uri)
                                        } else {
                                            previewItem = item
                                        }
                                    }
                                }
                                val onLongClick = remember(item.uri) {
                                    { viewModel.toggleItemCheck(item.uri) }
                                }

                                ImageGridCard(
                                    item = item,
                                    isChecked = isChecked,
                                    isMultiSelectMode = uiState.isMultiSelectMode,
                                    thumbnailSize = settings.thumbnailSize,
                                    context = context,
                                    onClick = onClick,
                                    onLongClick = onLongClick
                                )
                            }
                        }
                    }
                }
            }

            // Floating Capsule Status Pill Overlay (Bottom Center, avoiding FAB)
            FloatingStatusCapsule(
                message = uiState.statusMessage,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Processing overlay
            if (uiState.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // Dialog: Full-screen Image Preview Dialog
            previewItem?.let { item ->
                ImagePreviewDialog(
                    item = item,
                    onDismiss = { previewItem = null },
                    onBump = {
                        viewModel.bumpSingleItem(item.uri)
                    },
                    onRename = {
                        renameTargetItem = item
                        renameInputName = item.displayName
                        showRenameDialog = true
                    }
                )
            }

            // Dialog: Confirm deleting selected photos from album
            if (showDeleteConfirmDialog) {
                DeleteConfirmDialog(
                    checkedCount = uiState.checkedItemUris.size,
                    onDismiss = { showDeleteConfirmDialog = false },
                    onConfirm = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteCheckedItems()
                    }
                )
            }

            // Dialog: Rename photo
            if (showRenameDialog) {
                RenameDialog(
                    inputName = renameInputName,
                    onNameChange = { renameInputName = it },
                    onDismiss = {
                        showRenameDialog = false
                        renameTargetItem = null
                    },
                    onConfirm = {
                        showRenameDialog = false
                        renameTargetItem?.let { target ->
                            viewModel.renameItem(target.uri, renameInputName)
                            previewItem = null
                        }
                        renameTargetItem = null
                    }
                )
            }


            // Dialog: Ask whether to delete external original images
            if (uiState.externalUrisToAskDelete != null) {
                ExternalDeleteDialog(
                    albumName = settings.albumName,
                    onDismiss = { viewModel.onDismissExternalDelete() },
                    onConfirm = { viewModel.onConfirmExternalDelete() }
                )
            }
        }
    }
}

private fun checkMediaPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun getRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
