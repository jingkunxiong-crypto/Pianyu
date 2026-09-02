package com.example.newandroidapp.editing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterQrCodecTest {
    @Test
    fun pianyuFilter_roundTripsAllToneValues() {
        val preset = FilterPreset(
            id = "test",
            name = "海风 · A",
            builtIn = false,
            settings = EditSettings(
                exposure = 18,
                brightness = 12,
                contrast = -8,
                highlights = -37,
                shadows = 26,
                saturation = 33,
                warmth = -21,
                tint = 14,
                fade = 9,
                sharpness = 31,
                grain = 22,
                vignette = 17,
            ),
        )

        val decoded = FilterQrCodec.classify(FilterQrCodec.encode(preset))

        assertTrue(decoded is ScannedFilterCode.Pianyu)
        decoded as ScannedFilterCode.Pianyu
        assertEquals(preset.name, decoded.preset.name)
        assertEquals(preset.settings.toneSignature(), decoded.preset.settings.toneSignature())
    }

    @Test
    fun snapseedQrLink_isRecognizedWithoutTryingToDecodeItsPrivatePayload() {
        val result = FilterQrCodec.classify("https://snapseed.com/qr/looks/example")

        assertEquals(
            ScannedFilterCode.Snapseed("https://snapseed.com/qr/looks/example"),
            result,
        )
    }

    @Test
    fun unrelatedQr_isRejected() {
        val result = FilterQrCodec.classify("https://example.com/filter")

        assertTrue(result is ScannedFilterCode.Unsupported)
    }

    @Test
    fun settingsNormalization_clampsAndNormalizesRotation() {
        val normalized = EditSettings(
            exposure = -160,
            brightness = 150,
            grain = 130,
            vignette = -8,
            rotationDegrees = 450,
        ).normalized()

        assertEquals(-100, normalized.exposure)
        assertEquals(100, normalized.brightness)
        assertEquals(100, normalized.grain)
        assertEquals(0, normalized.vignette)
        assertEquals(90, normalized.rotationDegrees)
    }
}
