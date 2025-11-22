package app.aaps.plugins.automation.location

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a place result from Nominatim geocoding API
 */
data class NominatimPlace(
    val placeId: Long,
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val type: String
) {
    companion object {
        fun fromJson(json: JSONObject): NominatimPlace {
            return NominatimPlace(
                placeId = json.optLong("place_id", 0),
                latitude = json.optString("lat", "0").toDoubleOrNull() ?: 0.0,
                longitude = json.optString("lon", "0").toDoubleOrNull() ?: 0.0,
                displayName = json.optString("display_name", ""),
                type = json.optString("type", "")
            )
        }

        fun fromJsonArray(jsonArray: JSONArray): List<NominatimPlace> {
            val places = mutableListOf<NominatimPlace>()
            for (i in 0 until jsonArray.length()) {
                places.add(fromJson(jsonArray.getJSONObject(i)))
            }
            return places
        }
    }
}
