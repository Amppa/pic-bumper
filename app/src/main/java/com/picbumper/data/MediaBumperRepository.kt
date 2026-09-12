package com.picbumper.data

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.picbumper.domain.model.BumpSettings
import com.picbumper.domain.model.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaBumperRepository(private val context: Context) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    suspend fun resolveImageItems(uris: List<Uri>, albumName: String): List<ImageItem> =
        withContext(Dispatchers.IO) {
            uris.map { uri ->
                val displayName = queryDisplayName(uri) ?: "image_${System.currentTimeMillis()}.png"
                val size = queryFileSize(uri)
                val isFromA = isUriInDirectoryA(uri, albumName)
                ImageItem(
                    uri = uri,
                    displayName = displayName,
                    size = size,
                    isFromDirectoryA = isFromA
                )
            }
        }

    /**
     * Load all existing images previously bumped into directory A (Pictures/<albumName>).
     * Sorted by DATE_MODIFIED descending so the most recent memes appear first.
     */
    suspend fun loadAlbumImages(albumName: String): List<ImageItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ImageItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED
        )

        val selection: String
        val selectionArgs: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            selectionArgs = arrayOf("${Environment.DIRECTORY_PICTURES}/$albumName/%")
        } else {
            selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            selectionArgs = arrayOf("%/${Environment.DIRECTORY_PICTURES}/$albumName/%")
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC, ${MediaStore.Images.Media._ID} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "image_$id.png"
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val contentUri = android.content.ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    result.add(
                        ImageItem(
                            uri = contentUri,
                            displayName = name,
                            size = size,
                            isFromDirectoryA = true,
                            dateModified = dateModified
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        result
    }

    /**
     * Bump multiple images to the latest timestamp in the dedicated album directory.
     * Each image receives an incremental timestamp offset (+1 second) to guarantee strict order.
     */
    suspend fun bumpImages(
        items: List<ImageItem>,
        settings: BumpSettings
    ): BumpResult = withContext(Dispatchers.IO) {
        val bumpedUris = mutableListOf<Uri>()
        val selfDeletedUris = mutableListOf<Uri>()
        val externalUrisToAsk = mutableListOf<Uri>()
        val baseNowMillis = System.currentTimeMillis()

        items.forEachIndexed { index, item ->
            val timestampSec = (baseNowMillis / 1000) + index
            val timestampMillis = timestampSec * 1000

            // 1. Insert new media entry into dedicated album
            val newUri = insertImageToAlbum(
                sourceUri = item.uri,
                displayName = item.displayName,
                timestampSec = timestampSec,
                timestampMillis = timestampMillis,
                settings = settings
            )

            if (newUri != null) {
                bumpedUris.add(newUri)

                // 2. Handle original image based on its source directory
                if (item.isFromDirectoryA) {
                    // Files originally in directory A are self-owned and can be deleted silently
                    deleteSelfOwnedUri(item.uri)
                    selfDeletedUris.add(item.uri)
                } else {
                    externalUrisToAsk.add(item.uri)
                }
            }
        }

        BumpResult(
            bumpedUris = bumpedUris,
            selfDeletedUris = selfDeletedUris,
            externalUrisToAsk = externalUrisToAsk
        )
    }

    private fun insertImageToAlbum(
        sourceUri: Uri,
        displayName: String,
        timestampSec: Long,
        timestampMillis: Long,
        settings: BumpSettings
    ): Uri? {
        val relativePath = "${Environment.DIRECTORY_PICTURES}/${settings.albumName}/"
        val mimeType = contentResolver.getType(sourceUri) ?: inferMimeType(displayName)

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            if (settings.overrideDateAdded) {
                put(MediaStore.Images.Media.DATE_ADDED, timestampSec)
            }
            if (settings.overrideDateModified) {
                put(MediaStore.Images.Media.DATE_MODIFIED, timestampSec)
            }
            if (settings.overrideDateTaken) {
                put(MediaStore.Images.Media.DATE_TAKEN, timestampMillis)
            }
        }

        val targetUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null

        try {
            // Direct zero-decode binary stream transfer
            contentResolver.openInputStream(sourceUri)?.use { input ->
                contentResolver.openOutputStream(targetUri)?.use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }

            // Update EXIF if requested and applicable (JPG/JPEG)
            if (settings.overrideExif && isExifEligible(mimeType, displayName)) {
                tryUpdateExif(targetUri, timestampMillis)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(targetUri, values, null, null)
            }

            return targetUri
        } catch (e: Exception) {
            // Cleanup incomplete entry on failure
            try {
                contentResolver.delete(targetUri, null, null)
            } catch (_: Exception) {}
            return null
        }
    }

    private fun tryUpdateExif(uri: Uri, timestampMillis: Long) {
        try {
            contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                val dateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                val dateString = dateFormat.format(Date(timestampMillis))
                exif.setAttribute(ExifInterface.TAG_DATETIME, dateString)
                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateString)
                exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, dateString)
                exif.saveAttributes()
            }
        } catch (_: Exception) {
            // Ignore if EXIF write is unsupported on this stream
        }
    }

    private fun deleteSelfOwnedUri(uri: Uri): Boolean {
        return try {
            contentResolver.delete(uri, null, null) > 0
        } catch (_: Exception) {
            false
        }
    }

    fun buildDeleteIntentSender(uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(contentResolver, uris)
        } else {
            null
        }
    }

    fun isUriInDirectoryA(uri: Uri, albumName: String): Boolean {
        val targetPathSegment = "Pictures/$albumName"
        try {
            val projection = arrayOf(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.RELATIVE_PATH
                } else {
                    MediaStore.Images.Media.DATA
                }
            )
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val path = cursor.getString(0)
                    if (path != null && path.contains(targetPathSegment, ignoreCase = true)) {
                        return true
                    }
                }
            }
        } catch (_: Exception) {}
        return false
    }

    private fun queryDisplayName(uri: Uri): String? {
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
        } catch (_: Exception) {}
        return uri.lastPathSegment
    }

    private fun queryFileSize(uri: Uri): Long {
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0)
                }
            }
        } catch (_: Exception) {}
        return 0L
    }

    private fun inferMimeType(name: String): String {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".heic") || lower.endsWith(".heif") -> "image/heic"
            else -> "image/jpeg"
        }
    }

    private fun isExifEligible(mimeType: String, displayName: String): Boolean {
        return mimeType == "image/jpeg" ||
                displayName.endsWith(".jpg", ignoreCase = true) ||
                displayName.endsWith(".jpeg", ignoreCase = true)
    }
}

data class BumpResult(
    val bumpedUris: List<Uri>,
    val selfDeletedUris: List<Uri>,
    val externalUrisToAsk: List<Uri>
)
