package com.picbumper.ui.home.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.picbumper.domain.model.ImageItem
import com.picbumper.ui.theme.DarkSurfaceVariant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGridCard(
    item: ImageItem,
    isChecked: Boolean,
    isMultiSelectMode: Boolean = false,
    context: Context,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Coil Downsampling with remember cache
    val imageRequest = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(200)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(
                width = if (isChecked) 2.5.dp else 0.5.dp,
                color = if (isChecked) MaterialTheme.colorScheme.primary else Color.DarkGray,
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dim overlay when checked
        if (isChecked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }

        // Selection Checkbox (Only shown in multi-select mode or when checked)
        if (isMultiSelectMode || isChecked) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isChecked) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f))
                    .border(1.5.dp, if (isChecked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f), CircleShape)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
