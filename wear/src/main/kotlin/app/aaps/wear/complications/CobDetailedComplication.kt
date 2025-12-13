@file:Suppress("DEPRECATION")

package app.aaps.wear.complications

import android.app.PendingIntent
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import app.aaps.core.interfaces.logging.LTag
import dagger.android.AndroidInjection

/**
 * COB Detailed Complication
 *
 * Shows detailed carbs on board (COB) information
 * Displays both total COB and additional detail if space permits
 * Tap action opens carb/wizard dialog
 *
 */
class CobDetailedComplication : ModernBaseComplicationProviderService() {

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

                val cob = displayFormat.detailedCob(status, 0)
                val builder = ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                    .setShortText(ComplicationText.plainText(cob.first))
                    .setTapAction(complicationPendingIntent)
                if (cob.second.isNotEmpty()) {
                    builder.setShortTitle(ComplicationText.plainText(cob.second))
                }
                builder.build()
            }

            else                             -> {
                aapsLogger.warn(LTag.WEAR, "Unexpected complication type $dataType")
                null
            }
        }
    }

    override fun getProviderCanonicalName(): String = CobDetailedComplication::class.java.canonicalName!!
    override fun getComplicationAction(): ComplicationAction = ComplicationAction.WIZARD
}