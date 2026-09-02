package com.example.newandroidapp.editing

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoEditorMathTest {
    @Test
    fun nostalgicReferenceFilter_isAvailableWithExpectedTone() {
        val preset = BuiltInFilters.presets.single { it.id == "old-forest-light" }

        assertEquals("旧日森光", preset.name)
        assertEquals(-19, preset.settings.brightness)
        assertEquals(38, preset.settings.warmth)
        assertEquals(36, preset.settings.vignette)
    }

    @Test
    fun squareCrop_centersLandscapePhoto() {
        assertEquals(
            CropBounds(left = 500, top = 0, width = 3_000, height = 3_000),
            computeCenterCropBounds(4_000, 3_000, CropAspect.Square),
        )
    }

    @Test
    fun portraitCrop_centersPortraitPhoto() {
        assertEquals(
            CropBounds(left = 0, top = 250, width = 2_000, height = 2_500),
            computeCenterCropBounds(2_000, 3_000, CropAspect.Portrait),
        )
    }

    @Test
    fun originalCrop_keepsAllPixels() {
        assertEquals(
            CropBounds(left = 0, top = 0, width = 4_032, height = 3_024),
            computeCenterCropBounds(4_032, 3_024, CropAspect.Original),
        )
    }

    @Test
    fun watermarkBounds_anchorToRequestedBottomPosition() {
        val left = computeWatermarkBounds(4_000, 3_000, 1_299, 519, WatermarkPosition.BottomLeft)
        val center = computeWatermarkBounds(4_000, 3_000, 1_299, 519, WatermarkPosition.BottomCenter)
        val right = computeWatermarkBounds(4_000, 3_000, 1_299, 519, WatermarkPosition.BottomRight)

        assertEquals(105f, left.left, 0.1f)
        assertEquals(2_895f, left.bottom, 0.1f)
        assertEquals(2_000f, (center.left + center.right) / 2f, 0.1f)
        assertEquals(105f, 4_000f - right.right, 0.1f)
        assertEquals(left.right - left.left, center.right - center.left, 0.1f)
    }

    @Test
    fun applyingFilter_keepsWatermarkChoiceButPresetDoesNotStoreIt() {
        val edited = EditSettings(
            watermarkStyle = WatermarkStyle.White,
            watermarkPosition = WatermarkPosition.BottomLeft,
        ).applyPreset(BuiltInFilters.presets.first { it.id == "film" })

        assertEquals(WatermarkStyle.White, edited.watermarkStyle)
        assertEquals(WatermarkPosition.BottomLeft, edited.watermarkPosition)
        assertEquals(WatermarkStyle.None, edited.asPreset("测试").settings.watermarkStyle)
        assertEquals(
            WatermarkPosition.BottomCenter,
            edited.asPreset("测试").settings.watermarkPosition,
        )
    }
}
