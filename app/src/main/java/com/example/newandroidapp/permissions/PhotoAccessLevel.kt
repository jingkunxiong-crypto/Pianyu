package com.example.newandroidapp.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class PhotoAccessLevel {
    Full,
    Partial,
    Denied,
}

fun currentPhotoAccessLevel(context: Context): PhotoAccessLevel {
    return resolvePhotoAccessLevel(
        sdkInt = Build.VERSION.SDK_INT,
        hasImagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.hasPermission(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            false
        },
        hasSelectedImagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else {
            false
        },
        hasLegacyStoragePermission = context.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE),
    )
}

fun requiredPhotoPermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )

        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }
}

internal fun resolvePhotoAccessLevel(
    sdkInt: Int,
    hasImagesPermission: Boolean,
    hasSelectedImagesPermission: Boolean,
    hasLegacyStoragePermission: Boolean,
): PhotoAccessLevel {
    return when {
        sdkInt >= Build.VERSION_CODES.TIRAMISU && hasImagesPermission -> PhotoAccessLevel.Full
        sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && hasSelectedImagesPermission -> {
            PhotoAccessLevel.Partial
        }

        sdkInt <= Build.VERSION_CODES.S_V2 && hasLegacyStoragePermission -> PhotoAccessLevel.Full
        else -> PhotoAccessLevel.Denied
    }
}

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
