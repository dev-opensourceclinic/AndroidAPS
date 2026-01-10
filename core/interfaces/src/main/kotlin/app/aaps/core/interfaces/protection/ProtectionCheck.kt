package app.aaps.core.interfaces.protection

import androidx.annotation.UiThread
import androidx.fragment.app.FragmentActivity

/**
 * Typealias for backward compatibility.
 * ProtectionType is now defined in core/keys module.
 */
typealias ProtectionType = app.aaps.core.keys.ProtectionType

interface ProtectionCheck {
    enum class Protection {
        PREFERENCES,
        APPLICATION,
        BOLUS
    }

    fun isLocked(protection: Protection): Boolean
    fun resetAuthorization()

    @UiThread
    fun queryProtection(activity: FragmentActivity, protection: Protection, ok: Runnable?, cancel: Runnable? = null, fail: Runnable? = null)
}