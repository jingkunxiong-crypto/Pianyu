package com.example.newandroidapp.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoRepository(context: Context) {
    private val contentResolver = context.applicationContext.contentResolver

    private val collectionUri: Uri
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    suspend fun loadPhotos(): List<PhotoItem> = withContext(Dispatchers.IO) {
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.DATE_TAKEN)
            add(MediaStore.Images.Media.DATE_ADDED)
            add(MediaStore.Images.Media.WIDTH)
            add(MediaStore.Images.Media.HEIGHT)
            add(MediaStore.Images.Media.ORIENTATION)
            add(MediaStore.Images.Media.SIZE)
            add(MediaStore.Images.Media.MIME_TYPE)
            add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        val photos = mutableListOf<PhotoItem>()
        contentResolver.query(
            collectionUri,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val orientationColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateAddedSeconds = cursor.getLong(addedColumn)
                val dateTaken = cursor.getLong(takenColumn).takeIf { it > 0L }
                    ?: (dateAddedSeconds * 1_000L)

                photos += PhotoItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collectionUri, id).toString(),
                    displayName = cursor.getString(nameColumn) ?: "照片_$id",
                    dateTakenMillis = dateTaken,
                    dateAddedSeconds = dateAddedSeconds,
                    width = cursor.getInt(widthColumn),
                    height = cursor.getInt(heightColumn),
                    orientationDegrees = cursor.getInt(orientationColumn),
                    sizeBytes = cursor.getLong(sizeColumn),
                    mimeType = cursor.getString(mimeColumn) ?: "image/*",
                    bucketName = cursor.getString(bucketColumn) ?: "其他",
                    relativePath = if (pathColumn >= 0) cursor.getString(pathColumn) else null,
                )
            }
        }
        photos.sortedByDescending(PhotoItem::dateTakenMillis)
    }

    fun registerObserver(onMediaChanged: () -> Unit): ContentObserver {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onMediaChanged()
            }
        }
        contentResolver.registerContentObserver(collectionUri, true, observer)
        return observer
    }

    fun unregisterObserver(observer: ContentObserver) {
        contentResolver.unregisterContentObserver(observer)
    }
}
