package app.aaps.plugins.sync.nsShared.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.events.EventNSClientNewLog
import app.aaps.core.interfaces.rx.events.EventNSClientRestart
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.plugins.sync.nsShared.events.EventNSClientUpdateGuiData
import app.aaps.plugins.sync.nsShared.events.EventNSClientUpdateGuiQueue
import app.aaps.plugins.sync.nsShared.events.EventNSClientUpdateGuiStatus
import app.aaps.plugins.sync.nsclientV3.keys.NsclientBooleanKey
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NSClientViewModel @Inject constructor(
    private val preferences: Preferences,
    private val rh: ResourceHelper,
    private val rxBus: RxBus,
    private val fabricPrivacy: FabricPrivacy,
    private val aapsSchedulers: AapsSchedulers,
    private val aapsLogger: AAPSLogger,
    private val activePlugin: ActivePlugin,
    private val persistenceLayer: PersistenceLayer
) : ViewModel() {

    private val disposable = CompositeDisposable()

    private val nsClientPlugin
        get() = activePlugin.activeNsClient

    // LiveData for UI state
    private val _isPaused = MutableLiveData<Boolean>()
    val isPaused: LiveData<Boolean> = _isPaused

    private val _url = MutableLiveData<String>()
    val url: LiveData<String> = _url

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    private val _queue = MutableLiveData<String>()
    val queue: LiveData<String> = _queue

    private val _logList = MutableLiveData<List<EventNSClientNewLog>>()
    val logList: LiveData<List<EventNSClientNewLog>> = _logList

    // Event for showing full sync result dialog
    data class FullSyncResult(val success: Boolean, val message: String)
    private val _fullSyncResult = MutableLiveData<FullSyncResult?>()
    val fullSyncResult: LiveData<FullSyncResult?> = _fullSyncResult

    init {
        aapsLogger.debug(LTag.CORE, "NSClientViewModel initialized")
        subscribeToEvents()
        updateAllData()
    }

    private fun subscribeToEvents() {
        // Subscribe to RxBus events and convert to LiveData
        disposable += rxBus
            .toObservable(EventNSClientUpdateGuiData::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe(
                { updateLogData() },
                { fabricPrivacy.logException(it) }
            )

        disposable += rxBus
            .toObservable(EventNSClientUpdateGuiQueue::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe(
                { updateQueue() },
                { fabricPrivacy.logException(it) }
            )

        disposable += rxBus
            .toObservable(EventNSClientUpdateGuiStatus::class.java)
            .debounce(3L, TimeUnit.SECONDS)
            .observeOn(aapsSchedulers.main)
            .subscribe(
                { updateStatus() },
                { fabricPrivacy.logException(it) }
            )
    }

    fun updateAllData() {
        updateStatus()
        updateQueue()
        updateLogData()
    }

    private fun updateStatus() {
        _isPaused.value = preferences.get(NsclientBooleanKey.NsPaused)
        _url.value = nsClientPlugin?.address ?: ""
        _status.value = nsClientPlugin?.status ?: ""
    }

    private fun updateQueue() {
        val size = nsClientPlugin?.dataSyncSelector?.queueSize() ?: 0L
        _queue.value = if (size >= 0) size.toString() else rh.gs(app.aaps.core.ui.R.string.value_unavailable_short)
    }

    private fun updateLogData() {
        _logList.value = nsClientPlugin?.listLog ?: emptyList()
    }

    fun setPaused(paused: Boolean) {
        _isPaused.value = paused
        nsClientPlugin?.pause(paused)
    }

    fun clearLogs() {
        nsClientPlugin?.listLog?.let {
            synchronized(it) {
                it.clear()
                updateLogData()
            }
        }
    }

    fun resendData(reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            nsClientPlugin?.resend(reason)
        }
    }

    fun resetToFullSync() {
        viewModelScope.launch(Dispatchers.IO) {
            nsClientPlugin?.resetToFullSync()
            nsClientPlugin?.resend("FULL_SYNC")
        }
    }

    fun restartNSClient() {
        rxBus.send(EventNSClientRestart())
    }

    fun performFullSync(cleanupDatabase: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var result = ""
                if (cleanupDatabase) {
                    result = persistenceLayer.cleanupDatabase(93, deleteTrackedChanges = true)
                    aapsLogger.info(LTag.CORE, "Cleaned up databases with result: $result")
                }

                nsClientPlugin?.resetToFullSync()
                nsClientPlugin?.resend("FULL_SYNC")

                withContext(Dispatchers.Main) {
                    if (result.isNotEmpty()) {
                        _fullSyncResult.value = FullSyncResult(
                            success = true,
                            message = result
                        )
                    }
                }
            } catch (e: Exception) {
                aapsLogger.error("Error during full sync", e)
                withContext(Dispatchers.Main) {
                    _fullSyncResult.value = FullSyncResult(
                        success = false,
                        message = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun clearFullSyncResult() {
        _fullSyncResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        aapsLogger.debug(LTag.CORE, "NSClientViewModel cleared")
        disposable.clear()
    }
}
