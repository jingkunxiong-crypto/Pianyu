package com.example.newandroidapp.data

import android.app.RecoverableSecurityException
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface PhotoDeleteResult {
    data object Deleted : PhotoDeleteResult
    data class NeedsSystemConfirmation(val request: IntentSenderRequest) : PhotoDeleteResult
    data class Failed(val message: String) : PhotoDeleteResult
}

class PhotoDeleteManager(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun prepare(photo: PhotoItem): PhotoDeleteResult = withContext(Dispatchers.IO) {
        val uri = photo.uri.toUri()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(resolver, listOf(uri))
                PhotoDeleteResult.NeedsSystemConfirmation(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                deleteWithRecoverableAccess(uri)
            } else {
                val deletedRows = resolver.delete(uri, null, null)
                if (deletedRows > 0) {
                    PhotoDeleteResult.Deleted
                } else {
                    PhotoDeleteResult.Failed("系统没有删除这张照片，请确认文件仍然存在。")
                }
            }
        } catch (_: SecurityException) {
            PhotoDeleteResult.Failed("没有删除这张照片的权限，请在系统提示中允许后重试。")
        } catch (error: Exception) {
            PhotoDeleteResult.Failed(error.message ?: "删除失败，请稍后重试。")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteWithRecoverableAccess(uri: Uri): PhotoDeleteResult {
        return try {
            val deletedRows = resolver.delete(uri, null, null)
            if (deletedRows > 0) {
                PhotoDeleteResult.Deleted
            } else {
                PhotoDeleteResult.Failed("系统没有删除这张照片，请确认文件仍然存在。")
            }
        } catch (recoverable: RecoverableSecurityException) {
            PhotoDeleteResult.NeedsSystemConfirmation(
                IntentSenderRequest.Builder(recoverable.userAction.actionIntent.intentSender).build(),
            )
        }
    }
}
