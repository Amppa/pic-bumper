package com.picbumper.ui.home

import androidx.compose.runtime.Immutable
import com.picbumper.domain.model.ImageItem

@Immutable
sealed class GridEntry {
    abstract val id: String

    data class Header(
        val title: String,
        val count: Int,
        override val id: String
    ) : GridEntry()

    data class Photo(
        val item: ImageItem,
        override val id: String = item.uriString
    ) : GridEntry()
}
