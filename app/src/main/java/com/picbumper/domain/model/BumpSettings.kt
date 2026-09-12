package com.picbumper.domain.model

enum class ExternalDeleteMode {
    ASK,
    ALWAYS_KEEP,
    ALWAYS_DELETE
}

data class BumpSettings(
    val overrideDateAdded: Boolean = true,
    val overrideDateModified: Boolean = true,
    val overrideDateTaken: Boolean = true,
    val overrideExif: Boolean = true,
    val externalDeleteMode: ExternalDeleteMode = ExternalDeleteMode.ASK,
    val albumName: String = "PicBumper"
)
