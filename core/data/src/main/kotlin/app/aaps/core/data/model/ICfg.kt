package app.aaps.core.data.model

import app.aaps.core.data.model.ICfg

data class ICfg(
    var insulinLabel: String,
    var insulinEndTime: Long, // DIA before [milliseconds]
    var insulinPeakTime: Long, // [milliseconds]
    var concentration: Double = 1.0 // multiplication factor, 1.0 for U100, 2.0 for U200...
) {

    constructor(insulinLabel: String, peak: Int, dia: Double, concentration: Double = 1.0) : this(insulinLabel = insulinLabel, insulinEndTime = (dia * 3600 * 1000).toLong(), insulinPeakTime = (peak * 60000).toLong(), concentration = 1.0)

    fun getDia(): Double = Math.round(insulinEndTime / 3600.0 / 100.0) / 10.0
    fun getPeak(): Int = (insulinPeakTime / 60000).toInt()

    fun setDia(dia: Double) {
        insulinEndTime = (dia * 3600 * 1000).toLong()
    }

    fun setPeak(peak: Int) {
        this.insulinPeakTime = (peak * 60000).toLong()
    }

    companion object;
}