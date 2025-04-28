package app.aaps.core.data.model

import java.util.TimeZone

data class RM(
    override var id: Long = 0,
    override var version: Int = 0,
    override var dateCreated: Long = -1,
    override var isValid: Boolean = true,
    override var referenceId: Long? = null,
    override var ids: IDs = IDs(),
    var timestamp: Long,
    var utcOffset: Long = TimeZone.getDefault().getOffset(timestamp).toLong(),
    /** Current running mode. */
    var mode: Mode,
    /** Duration in milliseconds */
    var duration: Long // Planned duration
) : HasIDs {

    fun contentEqualsTo(other: RM): Boolean =
        timestamp == other.timestamp &&
            utcOffset == other.utcOffset &&
            mode == other.mode &&
            duration == other.duration &&
            isValid == other.isValid

    fun onlyNsIdAdded(previous: RM): Boolean =
        previous.id != id &&
            contentEqualsTo(previous) &&
            previous.ids.nightscoutId == null &&
            ids.nightscoutId != null

    enum class Mode {
        DISABLED_LOOP,
        OPEN_LOOP,
        CLOSED_LOOP,
        CLOSED_LOOP_LGS,
        // Temporary only
        SUPER_BOLUS,
        DISCONNECTED_PUMP,
        SUSPENDED_BY_PUMP,
        SUSPENDED_BY_USER
        ;

        fun isClosedLoopOrLgs() = this == CLOSED_LOOP || this == CLOSED_LOOP_LGS
        fun isLoopRunning() = this == OPEN_LOOP || this == CLOSED_LOOP || this == CLOSED_LOOP_LGS
        fun isSuspended() = this == DISCONNECTED_PUMP || this == SUSPENDED_BY_PUMP || this == SUSPENDED_BY_USER || this == SUPER_BOLUS

        companion object {

            fun fromString(reason: String?) = Mode.entries.firstOrNull { it.name == reason } ?: DEFAULT_MODE
        }
    }

    companion object {
        val DEFAULT_MODE = Mode.DISABLED_LOOP
    }
}