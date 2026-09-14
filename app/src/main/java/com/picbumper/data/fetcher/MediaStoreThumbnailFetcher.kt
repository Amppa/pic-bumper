package com.picbumper.data.fetcher

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        return withContext(Dispatchers.IO) {
            val bitmap = loadThumbnail(context.contentResolver, uri, options.size)
            if (bitmap != null) {
                DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            } else {
                null
            }
        }
    }

    private fun loadThumbnail(contentResolver: ContentResolver, uri: Uri, size: coil.size.Size): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pixelSize = if (size.width is coil.size.Dimension.Pixels) {
                    (size.width as coil.size.Dimension.Pixels).px
                } else 150
                contentResolver.loadThumbnail(uri, Size(pixelSize, pixelSize), null)
            } else {
                val id = uri.lastPathSegment?.toLongOrNull() ?: return null
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.getThumbnail(
                    contentResolver,
                    id,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme == ContentResolver.SCHEME_CONTENT) {
                return MediaStoreThumbnailFetcher(context, data, options)
            }
            return null
        }
    }
}
