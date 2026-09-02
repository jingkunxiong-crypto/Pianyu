package com.example.newandroidapp.editing

import java.util.Locale

enum class CropAspect(
    val label: String,
    val ratio: Float?,
) {
    Original("原始", null),
    Square("正方形", 1f),
    Portrait("竖版 4:5", 4f / 5f),
    Landscape("横版 4:3", 4f / 3f),
}

enum class WatermarkStyle(val label: String) {
    None("不添加"),
    White("白色"),
    Black("黑色"),
}

enum class WatermarkPosition(val label: String) {
    BottomLeft("左下"),
    BottomCenter("底部居中"),
    BottomRight("右下"),
}

data class EditSettings(
    val exposure: Int = 0,
    val brightness: Int = 0,
    val contrast: Int = 0,
    val highlights: Int = 0,
    val shadows: Int = 0,
    val saturation: Int = 0,
    val warmth: Int = 0,
    val tint: Int = 0,
    val fade: Int = 0,
    val sharpness: Int = 0,
    val grain: Int = 0,
    val vignette: Int = 0,
    val rotationDegrees: Int = 0,
    val cropAspect: CropAspect = CropAspect.Original,
    val watermarkStyle: WatermarkStyle = WatermarkStyle.None,
    val watermarkPosition: WatermarkPosition = WatermarkPosition.BottomCenter,
    val filterId: String = BuiltInFilters.OriginalId,
) {
    fun normalized(): EditSettings = copy(
        exposure = exposure.coerceIn(-100, 100),
        brightness = brightness.coerceIn(-100, 100),
        contrast = contrast.coerceIn(-100, 100),
        highlights = highlights.coerceIn(-100, 100),
        shadows = shadows.coerceIn(-100, 100),
        saturation = saturation.coerceIn(-100, 100),
        warmth = warmth.coerceIn(-100, 100),
        tint = tint.coerceIn(-100, 100),
        fade = fade.coerceIn(0, 100),
        sharpness = sharpness.coerceIn(0, 100),
        grain = grain.coerceIn(0, 100),
        vignette = vignette.coerceIn(0, 100),
        rotationDegrees = ((rotationDegrees % 360) + 360) % 360 / 90 * 90,
    )

    fun applyPreset(preset: FilterPreset): EditSettings {
        val tone = preset.settings.normalized()
        return copy(
            exposure = tone.exposure,
            brightness = tone.brightness,
            contrast = tone.contrast,
            highlights = tone.highlights,
            shadows = tone.shadows,
            saturation = tone.saturation,
            warmth = tone.warmth,
            tint = tone.tint,
            fade = tone.fade,
            sharpness = tone.sharpness,
            grain = tone.grain,
            vignette = tone.vignette,
            filterId = preset.id,
        )
    }

    fun asPreset(name: String): FilterPreset {
        val safeName = name.trim().ifEmpty { "我的滤镜" }.take(40)
        val toneOnly = normalized().copy(
            rotationDegrees = 0,
            cropAspect = CropAspect.Original,
            watermarkStyle = WatermarkStyle.None,
            watermarkPosition = WatermarkPosition.BottomCenter,
        )
        return FilterPreset(
            id = "custom-${toneOnly.toneSignature().hashCode().toUInt().toString(16)}",
            name = safeName,
            settings = toneOnly,
            builtIn = false,
        )
    }

    fun toneSignature(): String = listOf(
        exposure,
        brightness,
        contrast,
        highlights,
        shadows,
        saturation,
        warmth,
        tint,
        fade,
        sharpness,
        grain,
        vignette,
    ).joinToString(":")
}

data class FilterPreset(
    val id: String,
    val name: String,
    val settings: EditSettings,
    val builtIn: Boolean,
)

object BuiltInFilters {
    const val OriginalId = "original"

    val presets: List<FilterPreset> = listOf(
        preset(OriginalId, "原始"),
        preset("clear", "清透", exposure = 5, brightness = 4, contrast = 9, highlights = -12, shadows = 8, saturation = 7, warmth = -3, sharpness = 12),
        preset("warm-island", "暖屿", exposure = 3, brightness = 3, contrast = 5, highlights = -8, shadows = 5, saturation = 9, warmth = 22, vignette = 8),
        preset("cedar", "冷杉", brightness = -2, contrast = 12, highlights = -10, saturation = -5, warmth = -24, tint = 5, fade = 6, sharpness = 8),
        preset("film", "旧影", brightness = 3, contrast = -4, highlights = -20, shadows = 10, saturation = -12, warmth = 12, fade = 18, grain = 18, vignette = 16),
        preset(
            "old-forest-light",
            "旧日森光",
            brightness = -19,
            contrast = -20,
            highlights = -34,
            shadows = -12,
            saturation = -20,
            warmth = 38,
            tint = -9,
            fade = 4,
            grain = 12,
            vignette = 36,
        ),
        preset("mono", "银盐", brightness = 2, contrast = 18, highlights = -16, shadows = 10, saturation = -100, fade = 5, sharpness = 18, grain = 22, vignette = 12),
        preset("sunset", "落日", exposure = 3, brightness = 2, contrast = 10, highlights = -18, saturation = 17, warmth = 28, tint = 6, vignette = 10),
    )

    private fun preset(
        id: String,
        name: String,
        exposure: Int = 0,
        brightness: Int = 0,
        contrast: Int = 0,
        highlights: Int = 0,
        shadows: Int = 0,
        saturation: Int = 0,
        warmth: Int = 0,
        tint: Int = 0,
        fade: Int = 0,
        sharpness: Int = 0,
        grain: Int = 0,
        vignette: Int = 0,
    ) = FilterPreset(
        id = id,
        name = name,
        builtIn = true,
        settings = EditSettings(
            exposure = exposure,
            brightness = brightness,
            contrast = contrast,
            highlights = highlights,
            shadows = shadows,
            saturation = saturation,
            warmth = warmth,
            tint = tint,
            fade = fade,
            sharpness = sharpness,
            grain = grain,
            vignette = vignette,
            filterId = id,
        ),
    )
}

enum class Adjustment(
    val label: String,
    val minimum: Int,
    val maximum: Int,
    val group: AdjustmentGroup,
) {
    Exposure("曝光", -100, 100, AdjustmentGroup.Light),
    Brightness("亮度", -100, 100, AdjustmentGroup.Light),
    Contrast("对比", -100, 100, AdjustmentGroup.Light),
    Highlights("高光", -100, 100, AdjustmentGroup.Light),
    Shadows("阴影", -100, 100, AdjustmentGroup.Light),
    Saturation("饱和", -100, 100, AdjustmentGroup.Color),
    Warmth("色温", -100, 100, AdjustmentGroup.Color),
    Tint("色调", -100, 100, AdjustmentGroup.Color),
    Fade("褪色", 0, 100, AdjustmentGroup.Color),
    Sharpness("清晰度", 0, 100, AdjustmentGroup.Detail),
    Grain("颗粒", 0, 100, AdjustmentGroup.Detail),
    Vignette("暗角", 0, 100, AdjustmentGroup.Detail),
}

enum class AdjustmentGroup(val label: String) {
    Light("光线"),
    Color("色彩"),
    Detail("质感"),
}

fun EditSettings.valueFor(adjustment: Adjustment): Int = when (adjustment) {
    Adjustment.Exposure -> exposure
    Adjustment.Brightness -> brightness
    Adjustment.Contrast -> contrast
    Adjustment.Highlights -> highlights
    Adjustment.Shadows -> shadows
    Adjustment.Saturation -> saturation
    Adjustment.Warmth -> warmth
    Adjustment.Tint -> tint
    Adjustment.Fade -> fade
    Adjustment.Sharpness -> sharpness
    Adjustment.Grain -> grain
    Adjustment.Vignette -> vignette
}

fun EditSettings.withValue(adjustment: Adjustment, value: Int): EditSettings {
    val safeValue = value.coerceIn(adjustment.minimum, adjustment.maximum)
    return when (adjustment) {
        Adjustment.Exposure -> copy(exposure = safeValue, filterId = "custom")
        Adjustment.Brightness -> copy(brightness = safeValue, filterId = "custom")
        Adjustment.Contrast -> copy(contrast = safeValue, filterId = "custom")
        Adjustment.Highlights -> copy(highlights = safeValue, filterId = "custom")
        Adjustment.Shadows -> copy(shadows = safeValue, filterId = "custom")
        Adjustment.Saturation -> copy(saturation = safeValue, filterId = "custom")
        Adjustment.Warmth -> copy(warmth = safeValue, filterId = "custom")
        Adjustment.Tint -> copy(tint = safeValue, filterId = "custom")
        Adjustment.Fade -> copy(fade = safeValue, filterId = "custom")
        Adjustment.Sharpness -> copy(sharpness = safeValue, filterId = "custom")
        Adjustment.Grain -> copy(grain = safeValue, filterId = "custom")
        Adjustment.Vignette -> copy(vignette = safeValue, filterId = "custom")
    }
}

fun uniquePresetName(base: String, existingNames: Collection<String>): String {
    if (existingNames.none { it.equals(base, ignoreCase = true) }) return base
    var suffix = 2
    while (existingNames.any { it.equals("$base $suffix", ignoreCase = true) }) suffix++
    return String.format(Locale.ROOT, "%s %d", base, suffix)
}
