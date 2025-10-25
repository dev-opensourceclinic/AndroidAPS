package app.aaps.core.interfaces.pump

import app.aaps.core.interfaces.profile.EffectiveProfile
import app.aaps.core.interfaces.profile.Profile

/**
 * This class represents insulin amount delivered by pump.
 *
 * Example: when using U20 insulin and user request 0.6U insulin,
 * pump should deliver 0.6 * (100 / 20) = 3.0U.
 * In this case pump driver must use Insulin(3.0) which will be translated
 * by PumpSync back to 0.6U to store in database
 */
class PumpProfile(private val effectiveProfile: EffectiveProfile) {

    /**
     * Convert efectiveProfile managed within AAPS to concentratedProfile managed within Pump Drivers
     * @return concentratedProfile recalculated (basal rates, IC and ISF) to manage insulin concentration put into pump
     */
    fun concentratedProfile(): Profile = effectiveProfile.toPump()

}