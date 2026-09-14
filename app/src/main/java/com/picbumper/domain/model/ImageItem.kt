package com.picbumper.domain.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class ImageItem(
    val uri: Uri,
    val displayName: String,
    val size: Long = 0L,
    val isFromDirectoryA: Boolean = false,
    val dateModified: Long = 0L,
    val dateAdded: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val dateTaken: Long = 0L,
    val uriString: String = uri.toString()
)

