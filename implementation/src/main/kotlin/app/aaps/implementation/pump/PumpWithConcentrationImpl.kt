package app.aaps.implementation.pump

import androidx.annotation.VisibleForTesting
import app.aaps.core.data.pump.defs.ManufacturerType
import app.aaps.core.data.pump.defs.PumpDescription
import app.aaps.core.data.pump.defs.PumpType
import app.aaps.core.data.pump.defs.TimeChangeType
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.profile.EffectiveProfile
import app.aaps.core.interfaces.profile.Profile
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.core.interfaces.pump.Pump
import app.aaps.core.interfaces.pump.PumpEnactResult
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.pump.PumpWithConcentration
import app.aaps.core.interfaces.pump.actions.CustomAction
import app.aaps.core.interfaces.pump.actions.CustomActionType
import app.aaps.core.interfaces.queue.CustomCommand
import app.aaps.implementation.plugin.PluginStore
import org.json.JSONObject
import javax.inject.Inject

class PumpWithConcentrationImpl @Inject constructor(
    private val activePlugin: ActivePlugin,
    private val profileFunction: ProfileFunction,
    private val pumpSync: PumpSync,
    private val config: Config
) : PumpWithConcentration {

    @VisibleForTesting val activePumpInternal
        get() = (activePlugin as PluginStore).activePumpInternal

    override fun selectedActivePump(): Pump = activePumpInternal
    private val concentration: Double
        get() = if (config.enableInsulinConcentration()) profileFunction.getProfile()?.insulinConcentration() ?: 1.0 else 1.0

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
            activePumpInternal.setNewBasalProfile((profile as EffectiveProfile).toPump())
        } else activePumpInternal.setNewBasalProfile(profile)

    override fun isThisProfileSet(profile: Profile): Boolean =
        if (config.enableInsulinConcentration()) {
            activePumpInternal.isThisProfileSet((profile as EffectiveProfile).toPump())
        } else activePumpInternal.isThisProfileSet(profile)

    override val baseBasalRate: Double
        get() = activePumpInternal.baseBasalRate * concentration

    override fun deliverTreatment(detailedBolusInfo: DetailedBolusInfo): PumpEnactResult = activePumpInternal.deliverTreatment(detailedBolusInfo.also { it.insulin /= concentration } )

    override fun setTempBasalAbsolute(
        absoluteRate: Double,
        durationInMinutes: Int,
        profile: Profile,
        enforceNew: Boolean,
        tbrType: PumpSync.TemporaryBasalType
    ): PumpEnactResult =
        if (config.enableInsulinConcentration()) {
            activePumpInternal.setTempBasalAbsolute(absoluteRate / concentration, durationInMinutes, (profile as EffectiveProfile).toPump(), enforceNew, tbrType)
        } else activePumpInternal.setTempBasalAbsolute(absoluteRate, durationInMinutes, profile, enforceNew, tbrType)

    override fun setTempBasalPercent(
        percent: Int,
        durationInMinutes: Int,
        profile: Profile,
        enforceNew: Boolean,
        tbrType: PumpSync.TemporaryBasalType
    ): PumpEnactResult =
        if (config.enableInsulinConcentration()) {
            activePumpInternal.setTempBasalPercent(percent, durationInMinutes, (profile as EffectiveProfile).toPump(), enforceNew, tbrType)
        } else activePumpInternal.setTempBasalPercent(percent, durationInMinutes, profile, enforceNew, tbrType)

    override fun setExtendedBolus(insulin: Double, durationInMinutes: Int): PumpEnactResult =
        if (config.enableInsulinConcentration()) {
            activePumpInternal.setExtendedBolus(insulin / concentration, durationInMinutes)
        } else activePumpInternal.setExtendedBolus(insulin, durationInMinutes)

    /** PumpWithConcentration.pumpDescription should be used instead of Pump.pumpDescription outside Pump Driver to have corrected values */
    override val pumpDescription: PumpDescription =
        if (config.enableInsulinConcentration()) {
            activePumpInternal.pumpDescription.also {
                it.bolusStep *= concentration
                it.extendedBolusStep *= concentration
                it.maxTempAbsolute *= concentration
                it.tempAbsoluteStep *= concentration
                it.basalStep *= concentration
                it.basalMinimumRate *= concentration
                it.basalMaximumRate *= concentration
                it.maxResorvoirReading = (it.maxResorvoirReading * concentration).toInt()
            }
        } else activePumpInternal.pumpDescription
}