@file:Suppress("DEPRECATION")

package app.aaps.wear.complications

import android.app.PendingIntent
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import app.aaps.core.interfaces.logging.LTag
import dagger.android.AndroidInjection

/**
 * Long Status Complication
 *
 * Shows comprehensive glucose and status information in long text format
 * Title: Glucose value, arrow, delta, and time
 * Text: COB, IOB, and basal rate
 *
 */
class LongStatusComplication : ModernBaseComplicationProviderService() {

    // Not derived from DaggerService, do injection here
    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun buildComplicationData(
        dataType: Int,
        data: app.aaps.wear.data.ComplicationData,
        complicationPendingIntent: PendingIntent
    ): ComplicationData? {
        return when (dataType) {
            ComplicationData.TYPE_LONG_TEXT -> {
                // Pass EventData arrays directly to DisplayFormat
                val singleBg = arrayOf(data.bgData, data.bgData1, data.bgData2)
                val status = arrayOf(data.statusData, data.statusData1, data.statusData2)

                val glucoseLine = displayFormat.longGlucoseLine(singleBg, 0)
                val detailsLine = displayFormat.longDetailsLine(status, 0)

                ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                    .setLongTitle(ComplicationText.plainText(glucoseLine))
                    .setLongText(ComplicationText.plainText(detailsLine))
                    .setTapAction(complicationPendingIntent)
                    .build()
            }

            else                            -> {
                aapsLogger.warn(LTag.WEAR, "Unexpected complication type $dataType")
                null
            }
        }
    }

    override fun getProviderCanonicalName(): String = LongStatusComplication::class.java.canonicalName!!
}