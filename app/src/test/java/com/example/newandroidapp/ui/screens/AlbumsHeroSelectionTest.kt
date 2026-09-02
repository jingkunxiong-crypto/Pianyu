package com.example.newandroidapp.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumsHeroSelectionTest {
    @Test
    fun noFavoritesUsesThreeMostRecentPhotos() {
        val selected = selectHeroPhotoIds(
            orderedPhotoIds = listOf(9L, 8L, 7L, 6L),
            favoriteIds = emptySet(),
            seed = 42,
        )

        assertEquals(listOf(9L, 8L, 7L), selected)
    }

    @Test
    fun threeOrMoreFavoritesSelectsOnlyFavoritesAndStaysStableForSessionSeed() {
        val favorites = setOf(2L, 4L, 6L, 8L, 10L)
        val first = selectHeroPhotoIds((1L..10L).toList(), favorites, seed = 8472)
        val second = selectHeroPhotoIds((1L..10L).toList(), favorites, seed = 8472)

        assertEquals(3, first.size)
        assertTrue(first.all(favorites::contains))
        assertEquals(first, second)
    }

    @Test
    fun fewerThanThreeFavoritesFillsRemainingSlotsWithRecentPhotos() {
        val selected = selectHeroPhotoIds(
            orderedPhotoIds = listOf(9L, 8L, 7L, 6L),
            favoriteIds = setOf(7L),
            seed = 12,
        )

        assertEquals(listOf(7L, 9L, 8L), selected)
    }
}
