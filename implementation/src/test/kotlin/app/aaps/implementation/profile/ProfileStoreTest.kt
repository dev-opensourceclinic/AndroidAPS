package app.aaps.implementation.profile

import app.aaps.core.interfaces.profile.ProfileStore
import app.aaps.core.interfaces.profile.PureProfile
import app.aaps.shared.tests.TestBaseWithProfile
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

internal class ProfileStoreTest : TestBaseWithProfile() {

    @Test
    fun getStartDateTest() {
        assertThat(getValidProfileStore().getStartDate()).isEqualTo(0)
    }

    @Test
    fun getDefaultProfileTest() {
        assertIs<PureProfile>(getValidProfileStore().getDefaultProfile())
    }

    @Test
    fun getDefaultProfileJsonTest() {
        assertThat(getValidProfileStore().getDefaultProfileJson()?.has("dia")).isTrue()
        assertThat(getInvalidProfileStore2().getDefaultProfileJson()).isNull()
    }

    @Test
    fun getDefaultProfileNameTest() {
        assertThat(getValidProfileStore().getDefaultProfileName()).isEqualTo(TESTPROFILENAME)
    }

    @Test
    fun getProfileListTest() {
        assertThat(getValidProfileStore().getProfileList()).hasSize(1)
    }

    @Test
    fun getSpecificProfileTest() {
        assertIs<PureProfile>(getValidProfileStore().getSpecificProfile(TESTPROFILENAME))
    }

    @Test
    fun allProfilesValidTest() {
        assertThat(getValidProfileStore().allProfilesValid).isTrue()
        assertThat(getInvalidProfileStore1().allProfilesValid).isFalse()
        assertThat(getInvalidProfileStore2().allProfilesValid).isFalse()
    }

    val invalidProfileJSON = "{\"dia\":\"1\",\"carbratio\":[{\"time\":\"00:00\",\"value\":\"0\"}],\"carbs_hr\":\"20\",\"delay\":\"20\",\"sens\":[{\"time\":\"00:00\",\"value\":\"3\"}," +
    "{\"time\":\"2:00\",\"value\":\"3.4\"}],\"timezone\":\"UTC\",\"basal\":[{\"time\":\"00:00\",\"value\":\"1\"}],\"target_low\":[{\"time\":\"00:00\",\"value\":\"4.5\"}]," +
    "\"target_high\":[{\"time\":\"00:00\",\"value\":\"7\"}],\"startDate\":\"1970-01-01T00:00:00.000Z\",\"units\":\"mmol\"}"

    fun getInvalidProfileStore1(): ProfileStore {
        val json = JSONObject()
        val store = JSONObject()
        store.put(TESTPROFILENAME, JSONObject(invalidProfileJSON))
        json.put("defaultProfile", TESTPROFILENAME)
        json.put("store", store)
        return ProfileStoreObject(aapsLogger, activePlugin, config, rh, rxBus, hardLimits, dateUtil).with(json)
    }

    fun getInvalidProfileStore2(): ProfileStore {
        val json = JSONObject()
        val store = JSONObject()
        store.put(TESTPROFILENAME, JSONObject(validProfileJSON))
        store.put("invalid", JSONObject(invalidProfileJSON))
        json.put("defaultProfile", TESTPROFILENAME + "invalid")
        json.put("store", store)
        return ProfileStoreObject(aapsLogger, activePlugin, config, rh, rxBus, hardLimits, dateUtil).with(json)
    }
}
