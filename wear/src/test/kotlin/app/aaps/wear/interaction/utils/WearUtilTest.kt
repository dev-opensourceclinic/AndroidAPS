package app.aaps.wear.interaction.utils

import app.aaps.wear.WearTestBase
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Created by dlvoy on 22.11.2019.
 */
class WearUtilTest : WearTestBase() {

    @Test fun timestampAndTimeDiffsTest() {

        // smoke for mocks - since we freeze "now" to get stable tests
        assertThat(wearUtil.timestamp()).isEqualTo(REF_NOW)
        assertThat(wearUtil.msSince(REF_NOW + 3456L)).isEqualTo(-3456L)
        assertThat(wearUtil.msSince(REF_NOW - 6294L)).isEqualTo(6294L)
    }
}
