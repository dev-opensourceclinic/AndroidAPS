package app.aaps.implementation.profile

import androidx.fragment.app.FragmentActivity
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.profile.ProfileSource
import app.aaps.core.interfaces.profile.ProfileSourceWithConcentration
import app.aaps.core.interfaces.profile.ProfileStore
import app.aaps.core.interfaces.profile.PureProfile
import app.aaps.implementation.plugin.PluginStore
import javax.inject.Inject

class ProfileSourceWithConcentrationImpl @Inject constructor(
    private val activePlugin: ActivePlugin,
    private val config: Config
): ProfileSourceWithConcentration {

    private val activeProfileSourceInternal get() = (activePlugin as PluginStore).activeProfileSourceInternal

    override val profile: ProfileStore? =
        if (config.enableInsulinConcentration()) {
            TODO("Not yet implemented")
        } else activeProfileSourceInternal.profile
    
    override val profileName: String? =
        if (config.enableInsulinConcentration()) {
            TODO("Not yet implemented")
        } else activeProfileSourceInternal.profileName

    override fun addProfile(p: ProfileSource.SingleProfile) {
        if (config.enableInsulinConcentration()) {
            TODO("Not yet implemented")
        } else activeProfileSourceInternal.addProfile(p)
    }

    override fun copyFrom(pureProfile: PureProfile, newName: String): ProfileSource.SingleProfile =
        activeProfileSourceInternal.copyFrom(pureProfile, newName)

    override var currentProfileIndex: Int
        get() = activeProfileSourceInternal.currentProfileIndex
        set(value) { activeProfileSourceInternal.currentProfileIndex = value }

    override fun currentProfile(): ProfileSource.SingleProfile? =
        if (config.enableInsulinConcentration()) {
            TODO("Not yet implemented")
        } else activeProfileSourceInternal.currentProfile()
    
    override fun storeSettings(activity: FragmentActivity?, timestamp: Long) {
        activeProfileSourceInternal.storeSettings(activity, timestamp)
    }

    override fun loadFromStore(store: ProfileStore) {
        activeProfileSourceInternal.loadFromStore(store)
    }
}