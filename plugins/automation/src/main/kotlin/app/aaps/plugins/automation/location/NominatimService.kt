package app.aaps.plugins.automation.location

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for geocoding using OpenStreetMap Nominatim API
 * https://nominatim.org/release-docs/develop/api/Search/
 *
 * Usage Policy:
 * - Max 1 request per second
 * - Provide a valid User-Agent
 * - Results must include attribution to OpenStreetMap
 */
@Singleton
class NominatimService @Inject constructor(
    private val aapsLogger: AAPSLogger
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BASE_URL = "https://nominatim.openstreetmap.org"
        private const val USER_AGENT = "AndroidAPS/1.0 (https://androidaps.readthedocs.io)"
    }

    /**
     * Search for places by query string
     * @param query The search query (address, place name, etc.)
     * @param limit Maximum number of results (default 5, max 50)
     * @return Single emitting list of places
     */
    fun search(query: String, limit: Int = 5): Single<List<NominatimPlace>> {
        return Single.fromCallable {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$BASE_URL/search?q=$encodedQuery&format=json&limit=$limit&addressdetails=1"

            aapsLogger.debug(LTag.AUTOMATION, "Nominatim search: $query")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                aapsLogger.error(LTag.AUTOMATION, "Nominatim search failed: ${response.code}")
                return@fromCallable emptyList<NominatimPlace>()
            }

            val body = response.body?.string() ?: return@fromCallable emptyList<NominatimPlace>()
            val jsonArray = JSONArray(body)

            NominatimPlace.fromJsonArray(jsonArray).also {
                aapsLogger.debug(LTag.AUTOMATION, "Nominatim found ${it.size} results")
            }
        }.subscribeOn(Schedulers.io())
    }

    /**
     * Reverse geocode coordinates to address
     * @param lat Latitude
     * @param lon Longitude
     * @return Maybe emitting place if found, empty otherwise
     */
    fun reverseGeocode(lat: Double, lon: Double): Maybe<NominatimPlace> {
        return Maybe.fromCallable<NominatimPlace> {
            val url = "$BASE_URL/reverse?lat=$lat&lon=$lon&format=json"

            aapsLogger.debug(LTag.AUTOMATION, "Nominatim reverse geocode: $lat, $lon")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                aapsLogger.error(LTag.AUTOMATION, "Nominatim reverse geocode failed: ${response.code}")
                return@fromCallable null
            }

            val body = response.body?.string() ?: return@fromCallable null
            val json = JSONObject(body)

            if (json.has("error")) {
                aapsLogger.error(LTag.AUTOMATION, "Nominatim error: ${json.optString("error")}")
                return@fromCallable null
            }

            NominatimPlace.fromJson(json)
        }.subscribeOn(Schedulers.io())
    }
}
