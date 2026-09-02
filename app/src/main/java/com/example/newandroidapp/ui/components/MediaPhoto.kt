package com.example.newandroidapp.ui.components

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import com.example.newandroidapp.data.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun MediaPhoto(
    photo: PhotoItem,
    modifier: Modifier = Modifier,
    targetSizePx: Int = 512,
    contentScale: ContentScale = ContentScale.Crop,
    highQuality: Boolean = false,
    backgroundColor: Color? = null,
) {
    val context = LocalContext.current
    val cacheKey = "${photo.uri}@${if (highQuality) "full" else "thumb"}-$targetSizePx"
    val bitmap by produceState<Bitmap?>(
        initialValue = PhotoBitmapCache.get(cacheKey)
            ?: if (highQuality) PhotoBitmapCache.get("${photo.uri}@thumb-512") else null,
        key1 = cacheKey,
    ) {
        val cached = PhotoBitmapCache.get(cacheKey)
        if (cached != null) {
            value = cached
        } else {
            val loaded = withContext(Dispatchers.IO) {
                if (highQuality) {
                    loadFullPhoto(
                        context = context.applicationContext,
                        photo = photo,
                        maxDimension = targetSizePx,
                    )
                } else {
                    loadPhotoThumbnail(
                        context = context.applicationContext,
                        photo = photo,
                        targetSizePx = targetSizePx,
                    )
                }
            }
            if (loaded != null) {
                PhotoBitmapCache.put(cacheKey, loaded)
                value = loaded
            }
        }
    }

    Box(
        modifier = modifier
            .background(backgroundColor ?: MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = photo.displayName },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null && highQuality) {
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        }
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = photo.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}

private fun loadFullPhoto(
    context: Context,
    photo: PhotoItem,
    maxDimension: Int,
): Bitmap? = runCatching {
    val resolver = context.contentResolver
    val uri = photo.uri.toUri()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(resolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val largest = maxOf(info.size.width, info.size.height)
            if (largest > maxDimension) {
                val scale = maxDimension.toFloat() / largest.toFloat()
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1),
                )
            }
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(
                it,
                null,
                BitmapFactory.Options().apply { inSampleSize = sampleSize },
            )
        } ?: return@runCatching null
        if (photo.orientationDegrees == 0) {
            decoded
        } else {
            val matrix = Matrix().apply { postRotate(photo.orientationDegrees.toFloat()) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        }
    }
}.getOrNull()

private object PhotoBitmapCache {
    private val cacheSizeKb = (Runtime.getRuntime().maxMemory() / 1_024L / 16L)
        .coerceAtMost(32_768L)
        .toInt()
    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1_024
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        if (cache.get(key) == null) cache.put(key, bitmap)
    }
}

@Suppress("DEPRECATION")
private fun loadPhotoThumbnail(
    context: Context,
    photo: PhotoItem,
    targetSizePx: Int,
): Bitmap? {
    val resolver: ContentResolver = context.contentResolver
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.loadThumbnail(
                photo.uri.toUri(),
                Size(targetSizePx, targetSizePx),
                null,
            )
        } else {
            MediaStore.Images.Thumbnails.getThumbnail(
                resolver,
                photo.id,
                MediaStore.Images.Thumbnails.MINI_KIND,
                null,
            )
        }
    }.getOrNull()
}
