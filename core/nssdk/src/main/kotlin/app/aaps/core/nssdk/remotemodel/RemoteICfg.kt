package app.aaps.core.nssdk.remotemodel

import com.google.gson.annotations.SerializedName

data class RemoteICfg(
    @SerializedName("insulinLabel") val insulinLabel: String,
    @SerializedName("insulinLabel") val insulinEndTime: Long,
    @SerializedName("insulinLabel") val insulinPeakTime: Long,
    @SerializedName("insulinLabel") val concentration: Double
)