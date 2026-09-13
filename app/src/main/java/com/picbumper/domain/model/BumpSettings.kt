package com.picbumper.domain.model

data class BumpSettings(
    val overrideDateAdded: Boolean = false,
    val overrideDateModified: Boolean = true,
    val overrideDateTaken: Boolean = false,
    val overrideExif: Boolean = false,
    val albumName: String = "PicBumper",
    val silentRename: Boolean = true
)
