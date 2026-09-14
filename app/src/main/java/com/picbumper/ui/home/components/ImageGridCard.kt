package com.picbumper.ui.home.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.picbumper.domain.model.ImageItem

private val TilePlaceholderBg = Color(0xFF242424)
private val TilePlaceholderIconColor = Color(0xFF383838)
private val UncheckedBadgeBg = Color(0xFF202020)
private val UncheckedBadgeBorder = Color(0xFF666666)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGridCard(
    item: ImageItem,
    isChecked: Boolean,
    isMultiSelectMode: Boolean = false,
    thumbnailSize: Int = 150,
    context: Context,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val imageRequest = remember(item.uriString, thumbnailSize) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .memoryCacheKey("${item.uriString}_$thumbnailSize")
            .diskCacheKey("${item.uriString}_$thumbnailSize")
            .size(thumbnailSize)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .crossfade(false)
            .build()
    }

    val painter = rememberAsyncImagePainter(model = imageRequest)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(TilePlaceholderBg, RectangleShape)
            .then(
                if (isChecked) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RectangleShape)
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Static neutral photo icon shown while image is loading in 5~15ms
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = TilePlaceholderIconColor,
            modifier = Modifier.size(32.dp)
        )

        Image(
            painter = painter,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Selection Checkbox Badge (No full-card alpha dimming layer for 0 overdraw)
        if (isMultiSelectMode || isChecked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isChecked) MaterialTheme.colorScheme.primary else UncheckedBadgeBg)
                    .border(
                        1.5.dp,
                        if (isChecked) MaterialTheme.colorScheme.primary else UncheckedBadgeBorder,
                        CircleShape
                    ),
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
