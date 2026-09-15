package com.pictotop.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.pictotop.domain.model.ImageItem
import com.pictotop.util.FormatUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreviewDialog(
    items: List<ImageItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onBump: (ImageItem) -> Unit,
    onRename: (ImageItem) -> Unit,
    onDelete: (ImageItem) -> Unit
) {
    if (items.isEmpty()) return

    val validInitialIndex = initialIndex.coerceIn(0, items.size - 1)
    val pagerState = rememberPagerState(
        initialPage = validInitialIndex,
        pageCount = { items.size }
    )

    LaunchedEffect(initialIndex) {
        val targetIndex = initialIndex.coerceIn(0, items.size - 1)
        if (targetIndex in 0 until items.size && pagerState.currentPage != targetIndex) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    val currentItem = items.getOrNull(pagerState.currentPage) ?: return

    var isZoomed by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        isZoomed = false
    }

    val modifiedString = remember(currentItem.dateModified) { FormatUtils.formatDateTime(currentItem.dateModified, fallback = "未知") }
    val addedString = remember(currentItem.dateAdded) { FormatUtils.formatDateTime(currentItem.dateAdded, fallback = "未設定") }
    val takenString = remember(currentItem.dateTaken) { FormatUtils.formatDateTime(currentItem.dateTaken, fallback = "未設定") }
    val formattedSize = remember(currentItem.size) { FormatUtils.formatFileSize(currentItem.size) }
    val dimensionString = if (currentItem.width > 0 && currentItem.height > 0) {
        "${currentItem.width} × ${currentItem.height} ($formattedSize)"
    } else {
        formattedSize
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    key = { page -> items.getOrNull(page)?.uri ?: page },
                    userScrollEnabled = !isZoomed,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val item = items.getOrNull(page) ?: return@HorizontalPager
                    ZoomablePreviewImage(
                        item = item,
                        onSwipeDownDismiss = onDismiss,
                        onBackgroundClick = onDismiss,
                        onZoomChanged = { zoomed ->
                            if (page == pagerState.currentPage) {
                                isZoomed = zoomed
                            }
                        }
                    )
                }

                PreviewTopBar(
                    displayName = currentItem.displayName,
                    showInfo = showInfo,
                    onDismiss = onDismiss,
                    onRename = { onRename(currentItem) },
                    onDelete = { onDelete(currentItem) },
                    onToggleInfo = { showInfo = !showInfo },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                if (showInfo) {
                    PreviewMetadataCard(
                        modifiedString = modifiedString,
                        addedString = addedString,
                        takenString = takenString,
                        dimensionString = dimensionString,
                        modifier = Modifier
                            .padding(top = 64.dp, start = 12.dp)
                            .align(Alignment.TopStart)
                    )
                }

                PreviewBottomBar(
                    onBump = {
                        onDismiss()
                        onBump(currentItem)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun ZoomablePreviewImage(
    item: ImageItem,
    onSwipeDownDismiss: () -> Unit,
    onBackgroundClick: () -> Unit,
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember(item.uri) { mutableFloatStateOf(1f) }
    var offset by remember(item.uri) { mutableStateOf(Offset.Zero) }
    var swipeDownOffsetY by remember(item.uri) { mutableFloatStateOf(0f) }

    val animatedScale by animateFloatAsState(targetValue = scale, label = "scaleAnimation")
    val animatedOffset by animateOffsetAsState(targetValue = offset, label = "offsetAnimation")
    val animatedSwipeDownY by animateFloatAsState(targetValue = swipeDownOffsetY, label = "swipeDownAnimation")

    val coroutineScope = rememberCoroutineScope()
    var lastTapTime by remember(item.uri) { mutableLongStateOf(0L) }
    var lastTapPosition by remember(item.uri) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(scale) {
        onZoomChanged(scale > 1.05f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.uri) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragTotal = Offset.Zero
                    var isMultiTouch = false
                    var isPullingDown = false
                    var isHorizontalSwipe = false
                    var isPanning = false

                    do {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }

                        if (activePointers.size >= 2) {
                            // 2 or more fingers: Multi-touch Pinch to Zoom and Pan
                            isMultiTouch = true
                            isPullingDown = false
                            swipeDownOffsetY = 0f

                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                            scale = newScale
                            if (scale > 1.05f) {
                                offset += panChange
                            } else {
                                scale = 1f
                                offset = Offset.Zero
                            }
                            event.changes.forEach { it.consume() }
                        } else if (activePointers.size == 1 && !isMultiTouch) {
                            val change = activePointers[0]
                            val panChange = change.position - change.previousPosition

                            if (scale > 1.05f) {
                                // Zoomed in: 1-finger pan across the image
                                isPanning = true
                                offset += panChange
                                change.consume()
                            } else {
                                // Normal 1x view
                                dragTotal += panChange

                                if (!isHorizontalSwipe && !isPullingDown) {
                                    val dist = hypot(dragTotal.x, dragTotal.y)
                                    if (dist > touchSlop) {
                                        if (abs(dragTotal.x) > abs(dragTotal.y)) {
                                            // Primarily horizontal swipe -> leave unconsumed for HorizontalPager!
                                            isHorizontalSwipe = true
                                        } else if (dragTotal.y > 0) {
                                            // Primarily vertical drag downwards -> handle pull-down dismiss!
                                            isPullingDown = true
                                        }
                                    }
                                }

                                if (isPullingDown) {
                                    swipeDownOffsetY = (swipeDownOffsetY + panChange.y).coerceAtLeast(0f)
                                    change.consume()
                                }
                                // If isHorizontalSwipe is true, we do NOT consume, allowing HorizontalPager to page!
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // When all pointers are released
                    if (isPullingDown) {
                        if (swipeDownOffsetY > 160f) {
                            onSwipeDownDismiss()
                        } else {
                            swipeDownOffsetY = 0f
                        }
                    } else if (!isMultiTouch && !isPanning && !isHorizontalSwipe && hypot(dragTotal.x, dragTotal.y) < touchSlop) {
                        // Tap gesture
                        val now = System.currentTimeMillis()
                        val tapDist = hypot(down.position.x - lastTapPosition.x, down.position.y - lastTapPosition.y)
                        if (now - lastTapTime < 300 && tapDist < touchSlop * 2) {
                            // Double tap: toggle zoom
                            lastTapTime = 0L
                            if (scale > 1.1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                                offset = Offset.Zero
                            }
                        } else {
                            // First tap recorded
                            lastTapTime = now
                            lastTapPosition = down.position
                            coroutineScope.launch {
                                delay(320)
                                if (System.currentTimeMillis() - lastTapTime >= 300 && lastTapTime != 0L) {
                                    onBackgroundClick()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = animatedOffset.x,
                    translationY = animatedOffset.y + animatedSwipeDownY
                )
        )
    }
}

@Composable
private fun PreviewTopBar(
    displayName: String,
    showInfo: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onToggleInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
        Text(
            text = displayName,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )
        IconButton(onClick = onRename) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Rename photo",
                tint = Color.White
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete photo",
                tint = Color.White
            )
        }
        IconButton(onClick = onToggleInfo) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Toggle metadata info",
                tint = if (showInfo) MaterialTheme.colorScheme.primary else Color.White
            )
        }
    }
}

@Composable
private fun PreviewMetadataCard(
    modifiedString: String,
    addedString: String,
    takenString: String,
    dimensionString: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Intercept clicks on card body
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = "修改時間：$modifiedString",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.88f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "新增時間：$addedString",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.88f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "拍攝時間：$takenString",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.88f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "大小：$dimensionString",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
private fun PreviewBottomBar(
    onBump: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Button(
            onClick = onBump,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "置頂",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
