package com.example.newandroidapp.data

data class PhotoItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val dateTakenMillis: Long,
    val dateAddedSeconds: Long,
    val width: Int,
    val height: Int,
    val orientationDegrees: Int,
    val sizeBytes: Long,
    val mimeType: String,
    val bucketName: String,
    val relativePath: String?,
) {
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) {
            width.toFloat() / height.toFloat()
        } else {
            1f
        }
}
