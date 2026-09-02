package com.example.newandroidapp.domain

import com.example.newandroidapp.data.PhotoItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class PhotoLibraryFunctionsTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun groupPhotosByDay_sortsNewestDayFirst() {
        val older = photo(id = 1, date = 1_700_000_000_000L)
        val newer = photo(id = 2, date = 1_700_086_400_000L)

        val groups = groupPhotosByDay(listOf(older, newer), utc)

        assertEquals(listOf(2L, 1L), groups.map { it.photos.single().id })
    }

    @Test
    fun searchPhotos_matchesNameBucketAndTags() {
        val beach = photo(id = 1, name = "sunset.jpg", bucket = "Camera")
        val food = photo(id = 2, name = "IMG_2.jpg", bucket = "Downloads")
        val tags = mapOf(2L to setOf("晚餐", "家人"))

        assertEquals(listOf(1L), searchPhotos(listOf(beach, food), tags, "sunset").map { it.id })
        assertEquals(listOf(2L), searchPhotos(listOf(beach, food), tags, "Downloads").map { it.id })
        assertEquals(listOf(2L), searchPhotos(listOf(beach, food), tags, "家人").map { it.id })
    }

    @Test
    fun normalizeTags_trimsDeduplicatesAndSupportsChineseSeparator() {
        assertEquals(
            linkedSetOf("旅行", "家人", "晚霞"),
            normalizeTags(" 旅行, 家人，旅行#晚霞 "),
        )
    }

    @Test
    fun formatDayLabel_usesProvidedNowForYearComparison() {
        val nowIn2023 = 1_700_000_000_000L
        val sameYearDay = 1_690_000_000_000L

        val label = formatDayLabel(sameYearDay, nowIn2023, timeZone = utc)

        assertEquals(false, label.contains("2023年"))
    }

    private fun photo(
        id: Long,
        date: Long = 1_700_000_000_000L,
        name: String = "IMG_$id.jpg",
        bucket: String = "Camera",
    ) = PhotoItem(
        id = id,
        uri = "content://photos/$id",
        displayName = name,
        dateTakenMillis = date,
        dateAddedSeconds = date / 1_000L,
        width = 100,
        height = 100,
        orientationDegrees = 0,
        sizeBytes = 1_024L,
        mimeType = "image/jpeg",
        bucketName = bucket,
        relativePath = null,
    )
}
