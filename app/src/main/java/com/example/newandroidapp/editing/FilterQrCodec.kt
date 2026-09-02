package com.example.newandroidapp.editing

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed interface ScannedFilterCode {
    data class Pianyu(val preset: FilterPreset) : ScannedFilterCode
    data class Snapseed(val url: String) : ScannedFilterCode
    data class Unsupported(val reason: String) : ScannedFilterCode
}

object FilterQrCodec {
    private const val Scheme = "pianyu"
    private const val Host = "filter"
    private const val Path = "/v1"

    fun encode(preset: FilterPreset): String {
        val settings = preset.settings.normalized()
        val values = linkedMapOf(
            "name" to preset.name.take(40),
            "e" to settings.exposure.toString(),
            "b" to settings.brightness.toString(),
            "c" to settings.contrast.toString(),
            "h" to settings.highlights.toString(),
            "sh" to settings.shadows.toString(),
            "s" to settings.saturation.toString(),
            "w" to settings.warmth.toString(),
            "t" to settings.tint.toString(),
            "f" to settings.fade.toString(),
            "cl" to settings.sharpness.toString(),
            "g" to settings.grain.toString(),
            "v" to settings.vignette.toString(),
        )
        val query = values.entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, StandardCharsets.UTF_8.name())}"
        }
        return "$Scheme://$Host$Path?$query"
    }

    fun classify(rawValue: String): ScannedFilterCode {
        val raw = rawValue.trim()
        if (raw.isEmpty()) return ScannedFilterCode.Unsupported("二维码中没有可读取的内容")
        val uri = runCatching { URI(raw) }.getOrNull()
            ?: return ScannedFilterCode.Unsupported("二维码内容不是有效链接")

        if (uri.scheme.equals(Scheme, ignoreCase = true) &&
            uri.host.equals(Host, ignoreCase = true) &&
            uri.path == Path
        ) {
            return decodePianyu(uri)
        }

        val host = uri.host.orEmpty().lowercase()
        if ((uri.scheme.equals("https", ignoreCase = true) ||
                uri.scheme.equals("http", ignoreCase = true)) &&
            (host == "snapseed.com" || host.endsWith(".snapseed.com")) &&
            uri.path.orEmpty().startsWith("/qr")
        ) {
            return ScannedFilterCode.Snapseed(raw)
        }

        return ScannedFilterCode.Unsupported("这不是片屿或 Snapseed 滤镜码")
    }

    private fun decodePianyu(uri: URI): ScannedFilterCode {
        val values = uri.rawQuery.orEmpty()
            .split('&')
            .mapNotNull { pair ->
                val separator = pair.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                pair.substring(0, separator) to URLDecoder.decode(
                    pair.substring(separator + 1),
                    StandardCharsets.UTF_8.name(),
                )
            }
            .toMap()
        val name = values["name"].orEmpty().trim().take(40)
        if (name.isEmpty()) return ScannedFilterCode.Unsupported("片屿滤镜码缺少名称")

        fun number(key: String, min: Int, max: Int): Int? {
            val value = values[key]?.toIntOrNull() ?: return null
            return value.takeIf { it in min..max }
        }

        val settings = EditSettings(
            exposure = optionalNumber(values, "e", -100, 100),
            brightness = number("b", -100, 100)
                ?: return ScannedFilterCode.Unsupported("亮度参数无效"),
            contrast = number("c", -100, 100)
                ?: return ScannedFilterCode.Unsupported("对比度参数无效"),
            highlights = optionalNumber(values, "h", -100, 100),
            shadows = optionalNumber(values, "sh", -100, 100),
            saturation = number("s", -100, 100)
                ?: return ScannedFilterCode.Unsupported("饱和度参数无效"),
            warmth = number("w", -100, 100)
                ?: return ScannedFilterCode.Unsupported("色温参数无效"),
            tint = optionalNumber(values, "t", -100, 100),
            fade = number("f", 0, 100)
                ?: return ScannedFilterCode.Unsupported("褪色参数无效"),
            sharpness = optionalNumber(values, "cl", 0, 100),
            grain = optionalNumber(values, "g", 0, 100),
            vignette = number("v", 0, 100)
                ?: return ScannedFilterCode.Unsupported("暗角参数无效"),
            filterId = "imported",
        )
        return ScannedFilterCode.Pianyu(
            FilterPreset(
                id = "imported-${settings.toneSignature().hashCode().toUInt().toString(16)}",
                name = name,
                settings = settings,
                builtIn = false,
            ),
        )
    }

    private fun optionalNumber(
        values: Map<String, String>,
        key: String,
        min: Int,
        max: Int,
    ): Int = values[key]?.toIntOrNull()?.takeIf { it in min..max } ?: 0
}
