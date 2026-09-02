package com.example.newandroidapp.editing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.newandroidapp.data.PhotoItem

private const val SnapseedPackage = "com.niksoftware.snapseed"

object SnapseedIntegration {
    fun isInstalled(context: Context): Boolean {
        return context.packageManager.getLaunchIntentForPackage(SnapseedPackage) != null
    }

    fun openPhoto(context: Context, photo: PhotoItem): Boolean {
        val uri = photo.uri.toUri()
        val editIntent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, photo.mimeType)
            setPackage(SnapseedPackage)
            clipData = ClipData.newUri(context.contentResolver, photo.displayName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (startIfResolvable(context, editIntent)) return true

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = photo.mimeType
            setPackage(SnapseedPackage)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, photo.displayName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return startIfResolvable(context, sendIntent)
    }

    fun openQrLook(context: Context, url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            setPackage(SnapseedPackage)
        }
        return startIfResolvable(context, intent)
    }

    fun openStore(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, "market://details?id=$SnapseedPackage".toUri())
        val web = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$SnapseedPackage".toUri(),
        )
        if (!startIfResolvable(context, market)) context.startActivity(web)
    }

    private fun startIfResolvable(context: Context, intent: Intent): Boolean {
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
