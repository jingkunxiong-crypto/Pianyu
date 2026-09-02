package com.example.newandroidapp.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoAccessLevelTest {
    @Test
    fun android14_fullPermission_isFull() {
        assertAccess(
            sdkInt = 34,
            hasImagesPermission = true,
            hasSelectedImagesPermission = true,
            expected = PhotoAccessLevel.Full,
        )
    }

    @Test
    fun android14_selectedPermissionOnly_isPartial() {
        assertAccess(
            sdkInt = 34,
            hasSelectedImagesPermission = true,
            expected = PhotoAccessLevel.Partial,
        )
    }

    @Test
    fun android14_noPermission_isDenied() {
        assertAccess(sdkInt = 34, expected = PhotoAccessLevel.Denied)
    }

    @Test
    fun android13_imagesPermission_isFull() {
        assertAccess(
            sdkInt = 33,
            hasImagesPermission = true,
            expected = PhotoAccessLevel.Full,
        )
    }

    @Test
    fun android13_selectedPermissionDoesNotApply_isDenied() {
        assertAccess(
            sdkInt = 33,
            hasSelectedImagesPermission = true,
            expected = PhotoAccessLevel.Denied,
        )
    }

    @Test
    fun android12_legacyPermission_isFull() {
        assertAccess(
            sdkInt = 32,
            hasLegacyStoragePermission = true,
            expected = PhotoAccessLevel.Full,
        )
    }

    @Test
    fun android12_noPermission_isDenied() {
        assertAccess(sdkInt = 32, expected = PhotoAccessLevel.Denied)
    }

    private fun assertAccess(
        sdkInt: Int,
        hasImagesPermission: Boolean = false,
        hasSelectedImagesPermission: Boolean = false,
        hasLegacyStoragePermission: Boolean = false,
        expected: PhotoAccessLevel,
    ) {
        assertEquals(
            expected,
            resolvePhotoAccessLevel(
                sdkInt = sdkInt,
                hasImagesPermission = hasImagesPermission,
                hasSelectedImagesPermission = hasSelectedImagesPermission,
                hasLegacyStoragePermission = hasLegacyStoragePermission,
            ),
        )
    }
}
