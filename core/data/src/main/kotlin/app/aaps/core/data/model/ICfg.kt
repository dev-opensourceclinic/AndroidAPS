package app.aaps.core.data.model

import app.aaps.core.data.insulin.InsulinType
import app.aaps.core.data.iob.Iob
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Insulin configuration holds info about insulin
 */
data class ICfg(
    /**
     * Insulin name
     *
     * @Philoul do we need this?
     */
    var insulinLabel: String,
    /**
     * Aka DIA before in milliseconds
     */
    var insulinEndTime: Long,
    /**
     * Peak time from start in milliseconds
     */
    var insulinPeakTime: Long,
    /**
     * Insulin configuration is based on this template
     *
     * @Philoul is this necessary? It should be described enough by peak and dia
     * Here Template was used to make peak values "Read Only", if based on RapidActing, UltraRapidActing or Lyumjev templates,
     * or Read/Write if iCfg has been created from FreePeak template. (insulin managed within a unique InsulinPlugin the same way profilePlugin Manage profiles)
     * We can probably remove insulinTemplate and have peak value as "Read/Write" within InsulinPlugin whatever the template used to initialise insulin...
     */
    var insulinTemplate: InsulinType = InsulinType.UNKNOWN,
    /**
     * Insulin concentration (5.0 for U20, 0.5 for U200 insulin)
     * within my previous PR, concentration value was in the other direction... 2.0 for U200 or 0.2 for U20
     */
    var concentration: Double = 1.0
) {

    constructor(insulinLabel: String, peak: Int, dia: Double, insulinTemplate: InsulinType, concentration: Double)
        : this(insulinLabel = insulinLabel, insulinEndTime = (dia * 3600 * 1000).toLong(), insulinPeakTime = (peak * 60000).toLong(), insulinTemplate = insulinTemplate, concentration = concentration)
    /*
    this is for discussion. Purpose? => This function was linked to "InsulinPlugin" management.
    Because ICfg are recorded within EPS, PS from DB, list of available insulins recorded within the unique "InsulinPlugin" can miss the one embeded insulin from DB,
    so this function compare ICfg values (Peak, DIA, concentration) and update if necessary InsulinName (if available within InsulinPlugin with another name)
    or update the list of insulin within InsulinPlugin (if EPS.iCfg not found within InsulinPlugin list)
        fun isEqual(iCfg: ICfg?): Boolean {
            iCfg?.let { iCfg ->
                if (insulinEndTime != iCfg.insulinEndTime)
                    return false
                if (insulinPeakTime != iCfg.insulinPeakTime)
                    return false
                return true
            }
            return false
        }
    */
    /**
     * DIA (insulinEndTime) in hours rounded to 1 decimal place
     */
    fun getDia(): Double = (insulinEndTime / 3600.0 / 100.0).roundToInt() / 10.0

    /**
     * Peak time in minutes
     */
    fun getPeak(): Int = (insulinPeakTime / 60000).toInt()

    /**
     * Set insulinEndTime aka DIA
     * @param hours duration in hours
     */
    fun setDia(hours: Double) {
        insulinEndTime = (hours * 3600 * 1000).toLong()
    }

    /**
     * Set insulinPeakTime aka peak
     * @param minutes peak tme in minutes
     */
    fun setPeak(minutes: Int) {
        insulinPeakTime = (minutes * 60000).toLong()
    }

    fun deepClone(): ICfg = ICfg(insulinLabel, insulinEndTime, insulinPeakTime, insulinTemplate, concentration)

    fun iobCalcForTreatment(bolus: BS, time: Long): Iob {
        assert(insulinEndTime != 0L)
        assert(insulinPeakTime != 0L)
        val result = Iob()
        if (bolus.amount != 0.0) {
            val bolusTime = bolus.timestamp
            val t = (time - bolusTime) / 1000.0 / 60.0
            val td = getDia() * 60 //getDIA() always >= MIN_DIA
            val tp = getPeak().toDouble()
            // force the IOB to 0 if over DIA hours have passed
            if (t < td) {
                val tau = tp * (1 - tp / td) / (1 - 2 * tp / td)
                val a = 2 * tau / td
                val s = 1 / (1 - a + (1 + a) * exp(-td / tau))
                result.activityContrib = bolus.amount * (s / tau.pow(2.0)) * t * (1 - t / td) * exp(-t / tau)
                result.iobContrib = bolus.amount * (1 - s * (1 - a) * ((t.pow(2.0) / (tau * td * (1 - a)) - t / tau - 1) * exp(-t / tau) + 1))
            }
        }
        return result
    }

    companion object {

        /** Remove this before finishing code. Added to make the code compiled */
        val FAKE = ICfg(insulinLabel = "Fake", insulinEndTime = 9 * 3600 * 1000, insulinPeakTime = 60 * 60 * 1000)
    }
}