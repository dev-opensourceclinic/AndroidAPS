package app.aaps.plugins.sync.nsShared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.text.toSpanned
import androidx.core.view.MenuCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.setViewTreeLifecycleOwner
import app.aaps.core.data.ue.Action
import app.aaps.core.data.ue.Sources
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.logging.UserEntryLogger
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.interfaces.plugin.PluginFragment
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.events.EventNSClientRestart
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy
import app.aaps.core.ui.dialogs.OKDialog
import app.aaps.core.utils.HtmlHelper
import app.aaps.plugins.sync.R
import app.aaps.plugins.sync.di.SyncPluginQualifier
import app.aaps.plugins.sync.nsShared.ui.NSClientScreen
import app.aaps.plugins.sync.nsShared.viewmodel.NSClientViewModel
import dagger.android.support.DaggerFragment
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

class NSClientFragment : DaggerFragment(), MenuProvider, PluginFragment {

    @Inject lateinit var rh: ResourceHelper
    @Inject lateinit var rxBus: RxBus
    @Inject lateinit var fabricPrivacy: FabricPrivacy
    @Inject lateinit var aapsSchedulers: AapsSchedulers
    @Inject lateinit var uel: UserEntryLogger
    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var activePlugin: ActivePlugin
    @Inject lateinit var config: Config
    @Inject lateinit var persistenceLayer: PersistenceLayer

    @Inject
    @SyncPluginQualifier
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: NSClientViewModel

    companion object {
        const val ID_MENU_CLEAR_LOG = 507
        const val ID_MENU_RESTART = 508
        const val ID_MENU_SEND_NOW = 509
        const val ID_MENU_FULL_SYNC = 510
    }

    override var plugin: PluginBase? = null
    private val nsClientPlugin
        get() = activePlugin.activeNsClient

    private val disposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Initialize ViewModel BEFORE creating the view
        viewModel = ViewModelProvider(this, viewModelFactory)[NSClientViewModel::class.java]

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            // IMPORTANT: Set lifecycle owner so Compose can observe LiveData
            setViewTreeLifecycleOwner(viewLifecycleOwner)
            setContent {
                MaterialTheme {
                    NSClientScreen(
                        viewModel = viewModel,
                        onPausedChanged = { isChecked ->
                            uel.log(action = if (isChecked) Action.NS_PAUSED else Action.NS_RESUME, source = Sources.NSClient)
                            viewModel.setPaused(isChecked)
                        }
                    )
                }
            }
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(Menu.FIRST, ID_MENU_CLEAR_LOG, 0, rh.gs(R.string.clear_log)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.FIRST, ID_MENU_RESTART, 0, rh.gs(R.string.restart)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.FIRST, ID_MENU_SEND_NOW, 0, rh.gs(R.string.deliver_now)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.FIRST, ID_MENU_FULL_SYNC, 0, rh.gs(R.string.full_sync)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        MenuCompat.setGroupDividerEnabled(menu, true)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            ID_MENU_CLEAR_LOG -> {
                viewModel.clearLogs()
                true
            }

            ID_MENU_RESTART   -> {
                rxBus.send(EventNSClientRestart())
                true
            }

            ID_MENU_SEND_NOW  -> {
                viewModel.resendData("GUI")
                true
            }

            ID_MENU_FULL_SYNC -> {
                var result = ""
                context?.let { context ->
                    OKDialog.showConfirmation(
                        context, rh.gs(R.string.ns_client), rh.gs(R.string.full_sync_comment),
                        {
                            OKDialog.showConfirmation(requireContext(), rh.gs(R.string.ns_client), rh.gs(app.aaps.core.ui.R.string.cleanup_db_confirm_sync), {
                                disposable += Completable.fromAction { result = persistenceLayer.cleanupDatabase(93, deleteTrackedChanges = true) }
                                    .subscribeOn(aapsSchedulers.io)
                                    .observeOn(aapsSchedulers.main)
                                    .subscribeBy(
                                        onError = { aapsLogger.error("Error cleaning up databases", it) },
                                        onComplete = {
                                            if (result.isNotEmpty())
                                                OKDialog.show(
                                                    requireContext(),
                                                    rh.gs(app.aaps.core.ui.R.string.result),
                                                    HtmlHelper.fromHtml("<b>" + rh.gs(app.aaps.core.ui.R.string.cleared_entries) + "</b><br>" + result).toSpanned()
                                                )
                                            aapsLogger.info(LTag.CORE, "Cleaned up databases with result: $result")
                                            viewModel.resetToFullSync()
                                        }
                                    )
                                uel.log(action = Action.CLEANUP_DATABASES, source = Sources.NSClient)
                            }, {
                                viewModel.resetToFullSync()
                            })
                        }
                    )
                }
                true
            }

            else              -> false
        }

    @Synchronized
    override fun onResume() {
        super.onResume()
        // ViewModel handles RxBus subscriptions, just refresh data
        viewModel.updateAllData()
    }

    @Synchronized
    override fun onPause() {
        super.onPause()
        disposable.clear()
    }
}
