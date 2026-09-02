package com.example.newandroidapp.editing

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.example.newandroidapp.data.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface PhotoWriteResult {
    data object Done : PhotoWriteResult
    data class NeedsSystemConfirmation(val request: IntentSenderRequest) : PhotoWriteResult
    data class Failed(val message: String) : PhotoWriteResult
}

class PhotoOriginalManager(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val backupDirectory = File(appContext.filesDir, "photo_originals")

    fun hasBackup(photo: PhotoItem): Boolean = backupFile(photo).isFile

    suspend fun overwrite(
        photo: PhotoItem,
        settings: EditSettings,
        authorized: Boolean = false,
    ): PhotoWriteResult = withContext(Dispatchers.IO) {
        if (photo.mimeType.lowercase() !in SupportedOverwriteTypes) {
            return@withContext PhotoWriteResult.Failed(
                "${photo.mimeType.ifBlank { "这种格式" }} 暂不支持安全覆盖，请使用“另存副本”。",
            )
        }
        val backupResult = runCatching { ensureBackup(photo) }
        if (backupResult.isFailure) {
            return@withContext PhotoWriteResult.Failed(
                "无法备份原图：${backupResult.exceptionOrNull()?.message ?: "请检查存储空间"}",
            )
        }
        val bitmap = runCatching { PhotoEditorEngine.renderForExport(appContext, photo, settings) }
            .getOrElse {
                return@withContext PhotoWriteResult.Failed(
                    "无法生成编辑结果：${it.message ?: "请重试"}",
                )
            }
        try {
            writeBitmap(photo.uri.toUri(), bitmap, photo.mimeType)
            touch(photo.uri.toUri())
            PhotoWriteResult.Done
        } catch (security: SecurityException) {
            if (authorized) {
                PhotoWriteResult.Failed("系统没有授予修改这张照片的权限。")
            } else {
                confirmationRequest(photo.uri.toUri(), security)
            }
        } catch (error: Exception) {
            PhotoWriteResult.Failed(error.message ?: "覆盖原图失败，请重试。")
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun restore(
        photo: PhotoItem,
        authorized: Boolean = false,
    ): PhotoWriteResult = withContext(Dispatchers.IO) {
        val backup = backupFile(photo)
        if (!backup.isFile) {
            return@withContext PhotoWriteResult.Failed("没有找到这张照片的原始备份。")
        }
        try {
            resolver.openOutputStream(photo.uri.toUri(), "rwt")?.use { output ->
                backup.inputStream().buffered().use { input -> input.copyTo(output) }
            } ?: error("系统相册没有提供写入通道")
            touch(photo.uri.toUri())
            backup.delete()
            PhotoWriteResult.Done
        } catch (security: SecurityException) {
            if (authorized) {
                PhotoWriteResult.Failed("系统没有授予复原这张照片的权限。")
            } else {
                confirmationRequest(photo.uri.toUri(), security)
            }
        } catch (error: Exception) {
            PhotoWriteResult.Failed(error.message ?: "复原原图失败，请重试。")
        }
    }

    private fun ensureBackup(photo: PhotoItem) {
        val backup = backupFile(photo)
        if (backup.isFile) return
        check(backupDirectory.exists() || backupDirectory.mkdirs()) { "无法创建备份目录" }
        val temporary = File(backupDirectory, "${photo.id}.writing")
        try {
            resolver.openInputStream(photo.uri.toUri())?.use { input ->
                temporary.outputStream().buffered().use { output -> input.copyTo(output) }
            } ?: error("无法读取原始照片")
            check(temporary.length() > 0L) { "原始照片为空" }
            check(temporary.renameTo(backup)) { "无法完成原图备份" }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun writeBitmap(uri: Uri, bitmap: Bitmap, mimeType: String) {
        val format = when (mimeType.lowercase()) {
            "image/png" -> Bitmap.CompressFormat.PNG
            "image/webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            else -> Bitmap.CompressFormat.JPEG
        }
        resolver.openOutputStream(uri, "rwt")?.use { output ->
            check(bitmap.compress(format, 96, output)) { "照片编码失败" }
        } ?: error("系统相册没有提供写入通道")
    }

    private fun touch(uri: Uri) {
        runCatching {
            resolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1_000L)
                },
                null,
                null,
            )
        }
    }

    private fun confirmationRequest(uri: Uri, security: SecurityException): PhotoWriteResult {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> confirmationRequestR(uri)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> confirmationRequestQ(security)
            else -> PhotoWriteResult.Failed("系统没有授予修改这张照片的权限。")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun confirmationRequestR(uri: Uri): PhotoWriteResult {
        val request = MediaStore.createWriteRequest(resolver, listOf(uri))
        return PhotoWriteResult.NeedsSystemConfirmation(
            IntentSenderRequest.Builder(request.intentSender).build(),
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun confirmationRequestQ(security: SecurityException): PhotoWriteResult {
        val recoverable = security as? RecoverableSecurityException
            ?: return PhotoWriteResult.Failed("系统没有提供可授权的照片写入请求。")
        return PhotoWriteResult.NeedsSystemConfirmation(
            IntentSenderRequest.Builder(recoverable.userAction.actionIntent.intentSender).build(),
        )
    }

    private fun backupFile(photo: PhotoItem): File = File(backupDirectory, "${photo.id}.original")

    private companion object {
        val SupportedOverwriteTypes = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
    }
}
