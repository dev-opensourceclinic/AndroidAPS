package app.aaps.core.nssdk.localmodel.treatment

data class NSICfg(
    val insulinLabel: String,
    val insulinEndTime: Long,
    val insulinPeakTime: Long,
    val concentration: Double
)