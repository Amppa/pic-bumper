package com.picbumper.ui.settings

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var showTimeStrategyDialog by remember { mutableStateOf(false) }
    var showRenameStrategyDialog by remember { mutableStateOf(false) }

    // SAF Directory picker triggered by clicking the album path item
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val folderName = docId.substringAfterLast(':').substringAfterLast('/').ifEmpty { "PicBumper" }
            viewModel.setAlbumName(folderName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Setting Item 1: Album Path (Material 3 Preference Style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { folderPickerLauncher.launch(null) }
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "相簿路徑",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pictures/${settings.albumName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Setting Item 2: Time Strategy
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showTimeStrategyDialog = true }
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "覆蓋時間設定",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                val activeExtras = mutableListOf<String>()
                if (settings.overrideDateAdded) activeExtras.add("DATE_ADDED")
                if (settings.overrideDateTaken) activeExtras.add("DATE_TAKEN")
                if (settings.overrideExif) activeExtras.add("EXIF")

                val subtitleText = if (activeExtras.isEmpty()) {
                    "基礎：檔案修改時間 (DATE_MODIFIED) ‧ 點擊設定進階選項"
                } else {
                    "基礎：DATE_MODIFIED ‧ 進階：${activeExtras.joinToString(", ")}"
                }

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Setting Item 3: Rename Strategy
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showRenameStrategyDialog = true }
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "重命名策略",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (settings.silentRename) {
                        "無彈窗模式 (複製為自持新檔，極速順暢)"
                    } else {
                        "系統授權彈窗模式 (保持原檔案 URI)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Dialog 1: Option 2 Time Strategy Dialog
            if (showTimeStrategyDialog) {
                AlertDialog(
                    onDismissRequest = { showTimeStrategyDialog = false },
                    title = {
                        Text(
                            text = "覆蓋時間設定",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = true,
                                    onCheckedChange = null,
                                    enabled = false
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "檔案修改時間 (DATE_MODIFIED)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "基礎置頂依據，相容性最高且極速不產生重複檔。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "進階相容選項（若您的相簿或軟體仍無法置頂才需勾選）：",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            DialogCheckboxRow(
                                title = "檔案新增時間 (DATE_ADDED)",
                                subtitle = "部分系統相簿使用；若開啟，舊圖片可能需複製產生新檔。",
                                checked = settings.overrideDateAdded,
                                onCheckedChange = { viewModel.setOverrideDateAdded(it) }
                            )

                            DialogCheckboxRow(
                                title = "相片拍攝時間 (DATE_TAKEN)",
                                subtitle = "部分手機原生相簿（如小米、華為、OPPO 相簿）依拍攝時間排序時使用。",
                                checked = settings.overrideDateTaken,
                                onCheckedChange = { viewModel.setOverrideDateTaken(it) }
                            )

                            DialogCheckboxRow(
                                title = "寫入相片 EXIF 資訊時間",
                                subtitle = "將當前時間直接寫入 JPG 圖檔內部的 EXIF 拍攝資訊標籤。",
                                checked = settings.overrideExif,
                                onCheckedChange = { viewModel.setOverrideExif(it) }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTimeStrategyDialog = false }) {
                            Text("完成", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Dialog 2: Rename Strategy Dialog
            if (showRenameStrategyDialog) {
                AlertDialog(
                    onDismissRequest = { showRenameStrategyDialog = false },
                    title = {
                        Text(
                            text = "重命名策略設定",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            DialogRadioRow(
                                title = "無彈窗模式 (預設推薦)",
                                subtitle = "重命名受限的舊照片時，自動複製為新的自持檔案並清理舊副本，100% 零彈窗打擾。",
                                selected = settings.silentRename,
                                onClick = { viewModel.setSilentRename(true) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            DialogRadioRow(
                                title = "系統授權彈窗模式",
                                subtitle = "保持原檔案 URI，重命名非自建照片時由 Android 系統彈出授權對話框。",
                                selected = !settings.silentRename,
                                onClick = { viewModel.setSilentRename(false) }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showRenameStrategyDialog = false }) {
                            Text("完成", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DialogCheckboxRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun DialogRadioRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}
