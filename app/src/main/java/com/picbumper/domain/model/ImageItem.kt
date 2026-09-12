package com.picbumper.domain.model

import android.net.Uri

data class ImageItem(
    val uri: Uri,
    val displayName: String,
    val size: Long = 0L,
    val isFromDirectoryA: Boolean = false
)
