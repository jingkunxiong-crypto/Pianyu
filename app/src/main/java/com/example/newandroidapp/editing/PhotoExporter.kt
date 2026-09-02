package com.example.newandroidapp.editing

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.example.newandroidapp.data.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportedPhoto(
    val id: Long,
    val uri: String,
)

class PhotoExporter(private val context: Context) {
    suspend fun export(
        photo: PhotoItem,
        settings: EditSettings,
    ): ExportedPhoto = withContext(Dispatchers.IO) {
        val bitmap = PhotoEditorEngine.renderForExport(context, photo, settings)
        try {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val title = "Pianyu_$stamp"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                exportScoped(bitmap, photo, "$title.jpg")
            } else {
                @Suppress("DEPRECATION")
                val saved = MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    bitmap,
                    title,
                    "由片屿导出的照片副本",
                ) ?: error("系统相册拒绝了写入请求")
                val uri = saved.toUri()
                ExportedPhoto(ContentUris.parseId(uri), saved)
            }
        } finally {
            bitmap.recycle()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportScoped(
        bitmap: Bitmap,
        source: PhotoItem,
        displayName: String,
    ): ExportedPhoto {
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/片屿")
            put(MediaStore.Images.Media.DATE_TAKEN, source.dateTakenMillis)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val outputUri: Uri = resolver.insert(collection, values)
            ?: error("无法在系统相册中创建照片")
        try {
            resolver.openOutputStream(outputUri, "w")?.use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    "照片编码失败"
                }
            } ?: error("无法打开系统相册输出流")
            resolver.update(
                outputUri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null,
            )
            return ExportedPhoto(ContentUris.parseId(outputUri), outputUri.toString())
        } catch (error: Throwable) {
            resolver.delete(outputUri, null, null)
            throw error
        }
    }
}
