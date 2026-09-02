package com.example.newandroidapp.permissions

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.newandroidapp.PianyuApp
import com.example.newandroidapp.ui.screens.PhotoPermissionScreen

private const val PermissionPreferences = "photo_permission_preferences"
private const val HasRequestedPhotoAccess = "has_requested_photo_access"

@Composable
fun PianyuRoot() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val preferences = remember(context) {
        context.getSharedPreferences(PermissionPreferences, Context.MODE_PRIVATE)
    }
    var accessLevel by remember { mutableStateOf(currentPhotoAccessLevel(context)) }
    var hasRequestedAccess by remember {
        mutableStateOf(preferences.getBoolean(HasRequestedPhotoAccess, false))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        accessLevel = currentPhotoAccessLevel(context)
    }

    fun requestPhotoAccess() {
        hasRequestedAccess = true
        preferences.edit { putBoolean(HasRequestedPhotoAccess, true) }
        permissionLauncher.launch(requiredPhotoPermissions())
    }

    DisposableEffect(activity, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessLevel = currentPhotoAccessLevel(context)
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
        }
    }

    when (accessLevel) {
        PhotoAccessLevel.Denied -> PhotoPermissionScreen(
            previouslyRequested = hasRequestedAccess,
            onRequestAccess = ::requestPhotoAccess,
            onOpenSettings = { context.openAppSettings() },
        )

        PhotoAccessLevel.Partial,
        PhotoAccessLevel.Full,
        -> PianyuApp(
            photoAccessLevel = accessLevel,
            onManagePhotoAccess = ::requestPhotoAccess,
        )
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? {
    return when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
