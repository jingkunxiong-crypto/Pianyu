package com.example.newandroidapp.ui

import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatPhotoDateTime(timeMillis: Long): String {
    return DateFormat.getDateTimeInstance(
        DateFormat.LONG,
        DateFormat.SHORT,
        Locale.getDefault(),
    ).format(Date(timeMillis))
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1_024L) return "$bytes B"
    val kilobytes = bytes / 1_024.0
    if (kilobytes < 1_024.0) return String.format(Locale.getDefault(), "%.1f KB", kilobytes)
    val megabytes = kilobytes / 1_024.0
    return String.format(Locale.getDefault(), "%.1f MB", megabytes)
}
