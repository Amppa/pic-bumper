package com.pictotop.domain.model

data class BumpSettings(
    val overrideDateAdded: Boolean = false,
    val overrideDateModified: Boolean = true,
    val overrideDateTaken: Boolean = false,
    val overrideExif: Boolean = false,
    val albumName: String = "PicToTop",
    val silentRename: Boolean = true,
    val thumbnailSize: Int = 360
)

