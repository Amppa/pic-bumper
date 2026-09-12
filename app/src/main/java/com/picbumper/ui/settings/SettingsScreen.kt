package com.picbumper.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.picbumper.domain.model.ExternalDeleteMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var albumNameInput by remember(settings.albumName) { mutableStateOf(settings.albumName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定 (Settings)") },
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Group 1: Timestamp Overrides
            Text(
                text = "時間戳覆蓋項目 (Timestamp Overrides)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingSwitchItem(
                        title = "更新加入時間 (DATE_ADDED)",
                        description = "讓依照加入時間排序的選圖器辨識為最新",
                        checked = settings.overrideDateAdded,
                        onCheckedChange = { viewModel.setOverrideDateAdded(it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitchItem(
                        title = "更新修改時間 (DATE_MODIFIED)",
                        description = "讓依照檔案修改時間排序的選圖器辨識為最新",
                        checked = settings.overrideDateModified,
                        onCheckedChange = { viewModel.setOverrideDateModified(it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitchItem(
                        title = "更新拍攝時間 (DATE_TAKEN)",
                        description = "相簿檢視的核心時序欄位，更新至當前毫秒",
                        checked = settings.overrideDateTaken,
                        onCheckedChange = { viewModel.setOverrideDateTaken(it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitchItem(
                        title = "同步更新 EXIF 中繼資料",
                        description = "針對 JPG/JPEG 圖片寫入最新拍攝與修改時間",
                        checked = settings.overrideExif,
                        onCheckedChange = { viewModel.setOverrideExif(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group 2: External Images Delete Policy
            Text(
                text = "外部圖片刪除策略 (External Image Policy)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingRadioItem(
                        title = "每次詢問 (Ask every time)",
                        description = "推頂後彈窗詢問是否刪除外部原圖",
                        selected = settings.externalDeleteMode == ExternalDeleteMode.ASK,
                        onClick = { viewModel.setExternalDeleteMode(ExternalDeleteMode.ASK) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingRadioItem(
                        title = "一律保留 (Always keep)",
                        description = "僅複製推進時序，絕不刪除原圖也不詢問",
                        selected = settings.externalDeleteMode == ExternalDeleteMode.ALWAYS_KEEP,
                        onClick = { viewModel.setExternalDeleteMode(ExternalDeleteMode.ALWAYS_KEEP) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingRadioItem(
                        title = "一律嘗試刪除 (Always delete)",
                        description = "推進後自動呼叫系統刪除確認，省去額外對話框",
                        selected = settings.externalDeleteMode == ExternalDeleteMode.ALWAYS_DELETE,
                        onClick = { viewModel.setExternalDeleteMode(ExternalDeleteMode.ALWAYS_DELETE) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group 3: Storage Album
            Text(
                text = "專屬相簿名稱 (Dedicated Album)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "推進後的圖片將儲存在 Pictures/<相簿名稱> 目錄下。若再次選取該目錄中的圖，會自動靜默刪除舊版。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = albumNameInput,
                        onValueChange = {
                            albumNameInput = it
                            viewModel.setAlbumName(it)
                        },
                        label = { Text("相簿名稱 (預設: PicBumper)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingRadioItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
