package com.example.newandroidapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoOrganizerStateTest {
    private val album = PhotoAlbum(
        id = "album-1",
        name = "旅行",
        photoIds = setOf(1L, 2L, 3L),
        createdAtMillis = 1L,
    )

    @Test
    fun removingPhotosFromAlbum_keepsPhonePhotoMetadataElsewhere() {
        val state = PhotoOrganizerState(
            favoriteIds = setOf(2L),
            albums = listOf(album),
            tagsByPhotoId = mapOf(2L to setOf("海边")),
        )

        val updated = state.withPhotosRemovedFromAlbum("album-1", setOf(2L, 3L))

        assertEquals(setOf(1L), updated.albums.single().photoIds)
        assertTrue(2L in updated.favoriteIds)
        assertTrue(2L in updated.tagsByPhotoId)
    }

    @Test
    fun deletingAlbum_keepsOtherOrganizerData() {
        val other = album.copy(id = "album-2", name = "家人")
        val state = PhotoOrganizerState(
            favoriteIds = setOf(1L),
            albums = listOf(album, other),
            tagsByPhotoId = mapOf(1L to setOf("喜欢")),
        )

        val updated = state.withAlbumDeleted("album-1")

        assertFalse(updated.albums.any { it.id == "album-1" })
        assertEquals(listOf(other), updated.albums)
        assertEquals(state.favoriteIds, updated.favoriteIds)
        assertEquals(state.tagsByPhotoId, updated.tagsByPhotoId)
    }
}
