@file:Suppress("DEPRECATION")

package app.aaps.wear.complications

import android.app.PendingIntent
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import app.aaps.core.interfaces.logging.LTag
import dagger.android.AndroidInjection

/**
 * IOB Detailed Complication
 *
 * Shows detailed insulin on board (IOB) information
 * Displays both total IOB and additional detail if space permits
 * Tap action opens bolus wizard
 *
 */
class IobDetailedComplication : ModernBaseComplicationProviderService() {

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
            ComplicationData.TYPE_SHORT_TEXT -> {
                // Pass EventData arrays directly to DisplayFormat
                val status = arrayOf(data.statusData, data.statusData1, data.statusData2)

                val iob = displayFormat.detailedIob(status, 0)
                val builder = ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                    .setShortText(ComplicationText.plainText(iob.first))
                    .setTapAction(complicationPendingIntent)
                if (iob.second.isNotEmpty()) {
                    builder.setShortTitle(ComplicationText.plainText(iob.second))
                }
                builder.build()
            }

            else                             -> {
                aapsLogger.warn(LTag.WEAR, "Unexpected complication type $dataType")
                null
            }
        }
    }

    override fun getProviderCanonicalName(): String = IobDetailedComplication::class.java.canonicalName!!
    override fun getComplicationAction(): ComplicationAction = ComplicationAction.BOLUS
}