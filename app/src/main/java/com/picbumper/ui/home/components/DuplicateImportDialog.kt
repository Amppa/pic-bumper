package com.picbumper.ui.home.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.picbumper.domain.model.ImageItem
import com.picbumper.ui.theme.DarkSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DuplicateImportDialog(
    existingItem: ImageItem,
    newUri: Uri,
    newDisplayName: String,
    newSize: Long,
    newDateModified: Long,
    onKeepBoth: () -> Unit,
    onReplace: () -> Unit,
    onSkip: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()) }
    val existingDateStr = if (existingItem.dateModified > 0) dateFormat.format(Date(existingItem.dateModified * 1000L)) else "未知"
    val newDateStr = if (newDateModified > 0) dateFormat.format(Date(newDateModified * 1000L)) else "未知"

    AlertDialog(
        onDismissRequest = onSkip,
        title = {
            Text(
                text = "發現同名照片",
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
                Text(
                    text = "相簿內已存在同名照片 「$newDisplayName」，請選擇處理方式：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Comparison Cards (Side-by-side)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 1: Existing File in Album
                    ComparisonCard(
                        modifier = Modifier.weight(1f),
                        badgeText = "相簿既有照片",
                        badgeColor = MaterialTheme.colorScheme.primary,
                        imageModel = existingItem.uri,
                        displayName = existingItem.displayName,
                        size = existingItem.size,
                        dateStr = existingDateStr
                    )

                    // Card 2: New Incoming File
                    ComparisonCard(
                        modifier = Modifier.weight(1f),
                        badgeText = "欲匯入新照片",
                        badgeColor = MaterialTheme.colorScheme.tertiary,
                        imageModel = newUri,
                        displayName = newDisplayName,
                        size = newSize,
                        dateStr = newDateStr
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onKeepBoth,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("兩者皆保留", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onSkip) {
                    Text("跳過匯入")
                }
                OutlinedButton(
                    onClick = onReplace,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("覆蓋既有檔")
                }
            }
        }
    )
}

@Composable
private fun ComparisonCard(
    modifier: Modifier = Modifier,
    badgeText: String,
    badgeColor: Color,
    imageModel: Any,
    displayName: String,
    size: Long,
    dateStr: String
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Thumbnail
            AsyncImage(
                model = imageModel,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "容量: ${formatFileSize(size)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = dateStr,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.2f MB", mb)
    } else if (kb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f KB", kb)
    } else {
        "$bytes B"
    }
}
