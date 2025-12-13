package app.aaps.wear.interaction.utils

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.rx.weardata.CwfData
import app.aaps.core.interfaces.rx.weardata.CwfMetadataKey
import app.aaps.core.interfaces.rx.weardata.CwfResDataMap
import app.aaps.core.interfaces.rx.weardata.EventData
import app.aaps.core.interfaces.sharedPreferences.SP
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.shared.impl.weardata.ResFileMap
import app.aaps.wear.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by dlvoy on 2019-11-12
 * Refactored by MilosKozak 25/04/2022
 */
@Singleton
open class Persistence @Inject constructor(
    private val aapsLogger: AAPSLogger,
    private val dateUtil: DateUtil,
    private val sp: SP
) {

    companion object {

        const val KEY_COMPLICATIONS = "complications"
        const val KEY_LAST_SHOWN_SINCE_VALUE = "lastSince"
        const val KEY_STALE_REPORTED = "staleReported"

        const val CUSTOM_WATCHFACE = "custom_watchface"
        const val CUSTOM_DEFAULT_WATCHFACE = "custom_default_watchface"
        const val CUSTOM_DEFAULT_WATCHFACE_FULL = "custom_default_watchface_full"
    }

    fun getString(key: String, defaultValue: String): String {
        return sp.getString(key, defaultValue)
    }

    fun putString(key: String, value: String) {
        sp.putString(key, value)
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sp.getBoolean(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        sp.putBoolean(key, value)
    }

    fun getSetOf(key: String): Set<String> {
        return explodeSet(getString(key, ""), "|")
    }

    fun addToSet(key: String, value: String) {
        val set = explodeSet(getString(key, ""), "|")
        set.add(value)
        putString(key, joinSet(set, "|"))
    }

    fun removeFromSet(key: String, value: String) {
        val set = explodeSet(getString(key, ""), "|")
        set.remove(value)
        putString(key, joinSet(set, "|"))
    }

    fun readCustomWatchface(isDefault: Boolean = false): EventData.ActionSetCustomWatchface? {
        try {
            var s = sp.getStringOrNull(if (isDefault) CUSTOM_DEFAULT_WATCHFACE else CUSTOM_WATCHFACE, null)
            if (s != null) {
                return EventData.deserialize(s) as EventData.ActionSetCustomWatchface
            } else {
                s = sp.getStringOrNull(CUSTOM_DEFAULT_WATCHFACE, null)
                if (s != null) {
                    return EventData.deserialize(s) as EventData.ActionSetCustomWatchface
                }
            }
        } catch (exception: Exception) {
            aapsLogger.error(LTag.WEAR, exception.toString())
        }
        return null
    }

    private fun CwfData.simplify(): CwfData? = resData[ResFileMap.CUSTOM_WATCHFACE.fileName]?.let {
        val simplifiedData: CwfResDataMap = mutableMapOf()
        simplifiedData[ResFileMap.CUSTOM_WATCHFACE.fileName] = it
        CwfData(json, metadata, simplifiedData)
    }

    fun readSimplifiedCwf(isDefault: Boolean = false): EventData.ActionSetCustomWatchface? {
        try {
            val defaultKey = if (sp.getBoolean(R.string.key_include_external, false)) CUSTOM_DEFAULT_WATCHFACE_FULL else CUSTOM_DEFAULT_WATCHFACE
            var s = sp.getStringOrNull(if (isDefault) defaultKey else CUSTOM_WATCHFACE, null)
            if (s != null) {
                return (EventData.deserialize(s) as EventData.ActionSetCustomWatchface).let {
                    EventData.ActionSetCustomWatchface(it.customWatchfaceData.simplify() ?: it.customWatchfaceData)
                }

            } else {
                s = sp.getStringOrNull(defaultKey, null)
                if (s != null) {
                    return EventData.deserialize(s) as EventData.ActionSetCustomWatchface
                }
            }
        } catch (exception: Exception) {
            aapsLogger.error(LTag.WEAR, exception.toString())
        }
        return null
    }

    fun store(customWatchface: EventData.ActionSetCustomWatchface, customWatchfaceFull: EventData.ActionSetCustomWatchface? = null, isDefault: Boolean = false) {
        putString(if (isDefault) CUSTOM_DEFAULT_WATCHFACE else CUSTOM_WATCHFACE, customWatchface.serialize())
        customWatchfaceFull?.let { putString(CUSTOM_DEFAULT_WATCHFACE_FULL, it.serialize()) }
        aapsLogger.debug(LTag.WEAR, "Stored Custom Watchface ${customWatchface.customWatchfaceData} ${isDefault}: $customWatchface")
    }

    fun store(customWatchface: EventData.ActionUpdateCustomWatchface) {
        readCustomWatchface()?.let { savedCwData ->
            if (customWatchface.customWatchfaceData.metadata[CwfMetadataKey.CWF_NAME] == savedCwData.customWatchfaceData.metadata[CwfMetadataKey.CWF_NAME] &&
                customWatchface.customWatchfaceData.metadata[CwfMetadataKey.CWF_AUTHOR_VERSION] == savedCwData.customWatchfaceData.metadata[CwfMetadataKey.CWF_AUTHOR_VERSION]
            ) {
                // if same name and author version, then re-sync metadata to watch to update filename and authorization
                val newCwfData = CwfData(savedCwData.customWatchfaceData.json, customWatchface.customWatchfaceData.metadata, savedCwData.customWatchfaceData.resData)
                EventData.ActionSetCustomWatchface(newCwfData).also {
                    putString(CUSTOM_WATCHFACE, it.serialize())
                    aapsLogger.debug(LTag.WEAR, "Update Custom Watchface ${it.customWatchfaceData} : $customWatchface")
                }
            }
        }
    }

    fun setDefaultWatchface() {
        readCustomWatchface(true)?.let { store(it) }
        aapsLogger.debug(LTag.WEAR, "Custom Watchface reset to default")
    }

    fun joinSet(set: Set<String>, separator: String?): String {
        val sb = StringBuilder()
        var i = 0
        for (item in set) {
            val itemToAdd = item.trim { it <= ' ' }
            if (itemToAdd.isNotEmpty()) {
                if (i > 0) sb.append(separator)
                i++
                sb.append(itemToAdd)
            }
        }
        return sb.toString()
    }

    fun explodeSet(joined: String, separator: String): MutableSet<String> {
        // special RegEx literal \\Q starts sequence we escape, \\E ends is
        // we use it to escape separator for use in RegEx
        val items = joined.split(Regex("\\Q$separator\\E")).toTypedArray()
        val set: MutableSet<String> = HashSet()
        for (item in items) {
            val itemToAdd = item.trim { it <= ' ' }
            if (itemToAdd.isNotEmpty()) {
                set.add(itemToAdd)
            }
        }
        return set
    }

    fun turnOff() {
        aapsLogger.debug(LTag.WEAR, "TURNING OFF all active complications")
        putString(KEY_COMPLICATIONS, "")
    }
}
