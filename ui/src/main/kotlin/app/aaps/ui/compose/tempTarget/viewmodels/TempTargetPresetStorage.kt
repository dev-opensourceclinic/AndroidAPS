package app.aaps.ui.compose.tempTarget.viewmodels

import app.aaps.core.data.model.TT
import app.aaps.core.data.model.TTPreset
import org.json.JSONArray
import org.json.JSONObject

/**
 * Extension function to convert a list of TTPreset to JSON string.
 * @return JSON string representation of presets
 */
fun List<TTPreset>.toJson(): String {
    val jsonArray = JSONArray()
    forEach { preset ->
        val obj = JSONObject().apply {
            put("id", preset.id)
            preset.name?.let { put("name", it) }
            preset.nameRes?.let { put("nameRes", it) }
            put("reason", preset.reason.text)
            put("targetValue", preset.targetValue)
            put("duration", preset.duration)
            put("isDeletable", preset.isDeletable)
        }
        jsonArray.put(obj)
    }
    return jsonArray.toString()
}

/**
 * Extension function to parse JSON string into a list of TTPreset.
 * @return List of TTPreset objects, or empty list if parsing fails
 */
fun String.toTTPresets(): List<TTPreset> {
    return try {
        if (isEmpty() || this == "[]") {
            emptyList()
        } else {
            val jsonArray = JSONArray(this)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                TTPreset(
                    id = obj.getString("id"),
                    name = if (obj.has("name") && !obj.isNull("name")) obj.getString("name") else null,
                    nameRes = if (obj.has("nameRes") && !obj.isNull("nameRes")) obj.getInt("nameRes") else null,
                    reason = TT.Reason.fromString(obj.getString("reason")),
                    targetValue = obj.getDouble("targetValue"),
                    duration = obj.getLong("duration"),
                    isDeletable = obj.getBoolean("isDeletable")
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
