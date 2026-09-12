package com.picbumper.domain.model

data class BumpSettings(
    val overrideDateAdded: Boolean = true,
    val overrideDateModified: Boolean = true,
    val overrideDateTaken: Boolean = true,
    val overrideExif: Boolean = true,
    val albumName: String = "PicBumper"
)
