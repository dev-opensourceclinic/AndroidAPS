package app.aaps.core.objects.extensions

import app.aaps.core.data.model.ICfg
import org.json.JSONObject

/** used to save configuration within InsulinPlugin */
fun ICfg.toJson(): JSONObject = JSONObject()
    .put("insulinLabel", insulinLabel)
    .put("insulinEndTime", insulinEndTime)
    .put("insulinPeakTime", insulinPeakTime)
    .put("concentration", concentration)
    .put("insulinTemplate", insulinTemplate)
    .put("isNew", isNew)

/** used to restore configuration within InsulinPlugin and insulin Editor */
fun ICfg.Companion.fromJson(json: JSONObject): ICfg = ICfg(
    insulinLabel = json.optString("insulinLabel", ""),
    insulinEndTime = json.optLong("insulinEndTime", 0),
    insulinPeakTime = json.optLong("insulinPeakTime", 0),
    concentration = json.optDouble("concentration", 1.0)

) .also {
    it.insulinTemplate = json.optInt("insulinTemplate", 0)
    it.isNew =  json.optBoolean("isNew", false)
}