package app.aaps.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.rx.weardata.EventData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for complication data using DataStore with Protocol Buffers
 *
 * Best practices implemented:
 * - Single source of truth for complication data
 * - Asynchronous reads/writes (non-blocking)
 * - Type-safe data access
 * - Corruption handling with recovery
 * - Reactive updates via Flow
 *
 */
@Singleton
class ComplicationDataRepository @Inject constructor(
    private val context: Context,
    private val aapsLogger: AAPSLogger
) {

    private val dataStore: DataStore<ComplicationData> = DataStoreFactory.create(
        serializer = ComplicationDataSerializer(aapsLogger),
        produceFile = { context.dataStoreFile("complication_data.pb") }
    )

    /**
     * Reactive Flow of complication data
     * Complications should collect this Flow to get automatic updates
     */
    val complicationData: Flow<ComplicationData> = dataStore.data
        .catch { exception ->
            aapsLogger.error(LTag.WEAR, "Error reading complication data", exception)
            emit(ComplicationData()) // Emit default on error
        }

    /**
     * Update BG data from phone
     * Supports multiple datasets for AAPSClient mode (0=primary, 1=client1, 2=client2)
     */
    suspend fun updateBgData(singleBg: EventData.SingleBg) {
        try {
            dataStore.updateData { current ->
                when (singleBg.dataset) {
                    0    -> current.copy(bgData = singleBg, lastUpdateTimestamp = System.currentTimeMillis())
                    1    -> current.copy(bgData1 = singleBg, lastUpdateTimestamp = System.currentTimeMillis())
                    2    -> current.copy(bgData2 = singleBg, lastUpdateTimestamp = System.currentTimeMillis())
                    else -> {
                        aapsLogger.warn(LTag.WEAR, "Unknown BG dataset ${singleBg.dataset}, ignoring")
                        current
                    }
                }
            }
        } catch (e: Exception) {
            aapsLogger.error(LTag.WEAR, "Failed to update BG data", e)
        }
    }

    /**
     * Update Status data from phone
     * Supports multiple datasets for AAPSClient mode (0=primary, 1=client1, 2=client2)
     */
    suspend fun updateStatusData(status: EventData.Status) {
        try {
            dataStore.updateData { current ->
                when (status.dataset) {
                    0    -> current.copy(statusData = status, lastUpdateTimestamp = System.currentTimeMillis())
                    1    -> current.copy(statusData1 = status, lastUpdateTimestamp = System.currentTimeMillis())
                    2    -> current.copy(statusData2 = status, lastUpdateTimestamp = System.currentTimeMillis())
                    else -> {
                        aapsLogger.warn(LTag.WEAR, "Unknown Status dataset ${status.dataset}, ignoring")
                        current
                    }
                }
            }
        } catch (e: Exception) {
            aapsLogger.error(LTag.WEAR, "Failed to update status data", e)
        }
    }

    /**
     * Update Graph data from phone
     */
    suspend fun updateGraphData(graphData: EventData.GraphData) {
        try {
            dataStore.updateData { current ->
                current.copy(
                    graphData = graphData,
                    lastUpdateTimestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            aapsLogger.error(LTag.WEAR, "Failed to update graph data", e)
        }
    }

    /**
     * Update Treatment data from phone
     */
    suspend fun updateTreatmentData(treatmentData: EventData.TreatmentData) {
        try {
            dataStore.updateData { current ->
                current.copy(
                    treatmentData = treatmentData,
                    lastUpdateTimestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            aapsLogger.error(LTag.WEAR, "Failed to update treatment data", e)
        }
    }

    /**
     * Get current data timestamp (for stale data detection)
     */
    suspend fun getLastUpdateTimestamp(): Long {
        return try {
            var timestamp = 0L
            dataStore.updateData { current ->
                timestamp = current.lastUpdateTimestamp
                current
            }
            timestamp
        } catch (e: Exception) {
            aapsLogger.error(LTag.WEAR, "Failed to get last update timestamp", e)
            0L
        }
    }
}

/**
 * Serializer for ComplicationData using Protocol Buffers
 * Handles corruption gracefully by returning default data
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
private class ComplicationDataSerializer(
    private val aapsLogger: AAPSLogger
) : Serializer<ComplicationData> {

    override val defaultValue: ComplicationData = ComplicationData()

    override suspend fun readFrom(input: InputStream): ComplicationData {
        return try {
            ProtoBuf.decodeFromByteArray(
                ComplicationData.serializer(),
                input.readBytes()
            )
        } catch (e: SerializationException) {
            aapsLogger.error(LTag.WEAR, "Corrupted complication data, using default", e)
            defaultValue
        }
    }

    override suspend fun writeTo(t: ComplicationData, output: OutputStream) {
        output.write(
            ProtoBuf.encodeToByteArray(
                ComplicationData.serializer(),
                t
            )
        )
    }
}
