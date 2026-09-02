package com.example.newandroidapp.domain

import com.example.newandroidapp.data.PhotoItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class PhotoDayGroup(
    val dayStartMillis: Long,
    val photos: List<PhotoItem>,
)

fun groupPhotosByDay(
    photos: List<PhotoItem>,
    timeZone: TimeZone = TimeZone.getDefault(),
): List<PhotoDayGroup> {
    return photos
        .groupBy { startOfDay(it.dateTakenMillis, timeZone) }
        .toSortedMap(reverseOrder())
        .map { (day, items) -> PhotoDayGroup(day, items) }
}

fun formatDayLabel(
    dayStartMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault(),
): String {
    val today = startOfDay(nowMillis, timeZone)
    val yesterday = Calendar.getInstance(timeZone).run {
        timeInMillis = today
        add(Calendar.DAY_OF_YEAR, -1)
        timeInMillis
    }
    return when (dayStartMillis) {
        today -> "今天"
        yesterday -> "昨天"
        else -> {
            val isThisYear = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
                .get(Calendar.YEAR) ==
                Calendar.getInstance(timeZone).apply { timeInMillis = dayStartMillis }.get(Calendar.YEAR)
            SimpleDateFormat(if (isThisYear) "M月d日 EEE" else "yyyy年M月d日", locale).apply {
                this.timeZone = timeZone
            }.format(Date(dayStartMillis))
        }
    }
}

fun searchPhotos(
    photos: List<PhotoItem>,
    tagsByPhotoId: Map<Long, Set<String>>,
    query: String,
): List<PhotoItem> {
    val term = query.trim().lowercase(Locale.ROOT)
    if (term.isEmpty()) return emptyList()
    return photos.filter { photo ->
        photo.displayName.lowercase(Locale.ROOT).contains(term) ||
            photo.bucketName.lowercase(Locale.ROOT).contains(term) ||
            photo.mimeType.lowercase(Locale.ROOT).contains(term) ||
            tagsByPhotoId[photo.id].orEmpty().any { it.lowercase(Locale.ROOT).contains(term) }
    }
}

fun normalizeTags(raw: String): Set<String> {
    return raw
        .split(',', '，', '#', '\n')
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map { it.take(24) }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .take(12)
        .toSet()
}

private fun startOfDay(timeMillis: Long, timeZone: TimeZone): Long {
    return Calendar.getInstance(timeZone).apply {
        this.timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
