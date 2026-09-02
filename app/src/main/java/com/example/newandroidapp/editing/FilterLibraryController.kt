package com.example.newandroidapp.editing

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

@Stable
class FilterLibraryController(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "filter_library",
        Context.MODE_PRIVATE,
    )

    var customPresets: List<FilterPreset> by mutableStateOf(loadCustomPresets())
        private set

    val allPresets: List<FilterPreset>
        get() = BuiltInFilters.presets + customPresets

    fun saveImported(preset: FilterPreset): FilterPreset {
        val safeName = uniquePresetName(
            base = preset.name.ifBlank { "导入滤镜" }.take(40),
            existingNames = customPresets.map(FilterPreset::name),
        )
        val saved = preset.copy(
            id = "saved-${System.currentTimeMillis()}-${preset.settings.toneSignature().hashCode()}",
            name = safeName,
            builtIn = false,
        )
        customPresets = (listOf(saved) + customPresets).take(MaxCustomPresets)
        persist()
        return saved
    }

    fun deleteCustomPreset(presetId: String): Boolean {
        val updated = customPresets.filterNot { it.id == presetId }
        if (updated.size == customPresets.size) return false
        customPresets = updated
        persist()
        return true
    }

    private fun persist() {
        val array = JSONArray().apply {
            customPresets.forEach { preset ->
                put(JSONObject().apply {
                    put("id", preset.id)
                    put("name", preset.name)
                    put("payload", FilterQrCodec.encode(preset))
                })
            }
        }
        preferences.edit { putString(PresetKey, array.toString()) }
    }

    private fun loadCustomPresets(): List<FilterPreset> {
        val raw = preferences.getString(PresetKey, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    val item = array.optJSONObject(index) ?: return@repeat
                    val decoded = FilterQrCodec.classify(item.optString("payload"))
                    if (decoded is ScannedFilterCode.Pianyu) {
                        add(
                            decoded.preset.copy(
                                id = item.optString("id", decoded.preset.id),
                                name = item.optString("name", decoded.preset.name),
                            ),
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PresetKey = "custom_presets_v1"
        const val MaxCustomPresets = 50
    }
}
