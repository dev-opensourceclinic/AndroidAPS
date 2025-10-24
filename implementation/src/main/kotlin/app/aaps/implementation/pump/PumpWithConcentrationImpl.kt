package app.aaps.implementation.pump

import androidx.annotation.VisibleForTesting
import app.aaps.core.data.pump.defs.ManufacturerType
import app.aaps.core.data.pump.defs.PumpDescription
import app.aaps.core.data.pump.defs.PumpType
import app.aaps.core.data.pump.defs.TimeChangeType
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.profile.Profile
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.core.interfaces.pump.Pump
import app.aaps.core.interfaces.pump.PumpEnactResult
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.pump.PumpWithConcentration
import app.aaps.core.interfaces.pump.VirtualPump
import app.aaps.core.interfaces.pump.actions.CustomAction
import app.aaps.core.interfaces.pump.actions.CustomActionType
import app.aaps.core.interfaces.queue.CustomCommand
import app.aaps.implementation.plugin.PluginStore
import org.json.JSONObject
import javax.inject.Inject

class PumpWithConcentrationImpl @Inject constructor(
    private val activePlugin: ActivePlugin,
    private val config: Config
) : PumpWithConcentration {

    @VisibleForTesting val activePumpInternal
        get() = (activePlugin as PluginStore).activePumpInternal

    override fun selectedActivePump(): Pump = activePumpInternal

    override fun isInitialized(): Boolean = activePumpInternal.isInitialized()
    override fun isSuspended(): Boolean = activePumpInternal.isSuspended()
    override fun isBusy(): Boolean = activePumpInternal.isBusy()
    override fun isConnected(): Boolean = activePumpInternal.isConnected()
    override fun isConnecting(): Boolean = activePumpInternal.isConnecting()
    override fun isHandshakeInProgress(): Boolean = activePumpInternal.isHandshakeInProgress()
    override fun waitForDisconnectionInSeconds(): Int = activePumpInternal.waitForDisconnectionInSeconds()
    override fun getPumpStatus(reason: String) = activePumpInternal.getPumpStatus(reason)
    override fun lastDataTime(): Long = activePumpInternal.lastDataTime()
    override val reservoirLevel: Double get() = activePumpInternal.reservoirLevel
    override val batteryLevel: Int get() = activePumpInternal.batteryLevel
    override fun cancelTempBasal(enforceNew: Boolean): PumpEnactResult = activePumpInternal.cancelTempBasal(enforceNew)
    override fun cancelExtendedBolus(): PumpEnactResult = activePumpInternal.cancelExtendedBolus()
    override fun getJSONStatus(profile: Profile, profileName: String, version: String): JSONObject = activePumpInternal.getJSONStatus(profile, profileName, version)
    override fun manufacturer(): ManufacturerType = activePumpInternal.manufacturer()
    override fun model(): PumpType = activePumpInternal.model()
    override fun serialNumber(): String = activePumpInternal.serialNumber()
    override fun shortStatus(veryShort: Boolean): String = activePumpInternal.shortStatus(veryShort)
    override val isFakingTempsByExtendedBoluses: Boolean get() = activePumpInternal.isFakingTempsByExtendedBoluses
    override fun loadTDDs(): PumpEnactResult = activePumpInternal.loadTDDs()
    override fun canHandleDST(): Boolean = activePumpInternal.canHandleDST()
    override fun getCustomActions(): List<CustomAction>? = activePumpInternal.getCustomActions()
    override fun executeCustomCommand(customCommand: CustomCommand): PumpEnactResult? = activePumpInternal.executeCustomCommand(customCommand)
    override fun timezoneOrDSTChanged(timeChangeType: TimeChangeType) = activePumpInternal.timezoneOrDSTChanged(timeChangeType)
    override fun isUnreachableAlertTimeoutExceeded(unreachableTimeoutMilliseconds: Long): Boolean = activePumpInternal.isUnreachableAlertTimeoutExceeded(unreachableTimeoutMilliseconds)
    override fun setNeutralTempAtFullHour(): Boolean = activePumpInternal.setNeutralTempAtFullHour()
    override fun isBatteryChangeLoggingEnabled(): Boolean = activePumpInternal.isBatteryChangeLoggingEnabled()
    override fun isUseRileyLinkBatteryLevel(): Boolean = activePumpInternal.isUseRileyLinkBatteryLevel()
    override fun finishHandshaking() { activePumpInternal.finishHandshaking() }
    override fun connect(reason: String) { activePumpInternal.connect(reason) }
    override fun disconnect(reason: String) { activePumpInternal.disconnect(reason) }
    override fun stopConnecting() { activePumpInternal.stopConnecting() }
    override fun stopBolusDelivering() { activePumpInternal.stopBolusDelivering() }
    override fun executeCustomAction(customActionType: CustomActionType) { activePumpInternal.executeCustomAction(customActionType) }

    override fun setNewBasalProfile(profile: Profile): PumpEnactResult =
        if (config.enableInsulinConcentration()) {
            TODO("Not yet implemented")
        } else activePumpInternal.setNewBasalProfile(profile)

    override fun isThisProfileSet(profile: Profile): Boolean =
        if (config.enableInsulinConcentration()) {
            TODO("Not yet implemented")
        } else activePumpInternal.isThisProfileSet(profile)

    override val baseBasalRate: Double
        get() =
            if (config.enableInsulinConcentration()) {
                TODO("Not yet implemented")
            } else activePumpInternal.baseBasalRate

    override fun deliverTreatment(detailedBolusInfo: DetailedBolusInfo): PumpEnactResult =
        if (config.enableInsulinConcentration()) {
            TODO("Not yet implemented")
        } else activePumpInternal.deliverTreatment(detailedBolusInfo)

    override fun setTempBasalAbsolute(
        absoluteRate: Double,
        durationInMinutes: Int,
        profile: Profile,
        enforceNew: Boolean,
        tbrType: PumpSync.TemporaryBasalType
    ): PumpEnactResult =
        if (config.enableInsulinConcentration()) {
            TODO("Not yet implemented")
        } else activePumpInternal.setTempBasalAbsolute(absoluteRate, durationInMinutes, profile, enforceNew, tbrType)

    override fun setTempBasalPercent(
        percent: Int,
        durationInMinutes: Int,
        profile: Profile,
        enforceNew: Boolean,
        tbrType: PumpSync.TemporaryBasalType
    ): PumpEnactResult =
        if (config.enableInsulinConcentration()) {
            TODO("Not yet implemented")
        } else activePumpInternal.setTempBasalPercent(percent, durationInMinutes, profile, enforceNew, tbrType)

    override fun setExtendedBolus(insulin: Double, durationInMinutes: Int): PumpEnactResult =
        if (config.enableInsulinConcentration()) {
            TODO("Not yet implemented")
        } else activePumpInternal.setExtendedBolus(insulin, durationInMinutes)

    override val pumpDescription: PumpDescription =
        if (config.enableInsulinConcentration()) {
            TODO("Not yet implemented")
        } else activePumpInternal.pumpDescription
}