package app.aaps.plugins.automation.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.plugins.automation.R
import app.aaps.plugins.automation.events.EventPlaceSelected
import app.aaps.plugins.automation.services.LastLocationDataContainer
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject

class MapPickerDialog : BaseDialog() {

    @Inject lateinit var rxBus: RxBus
    @Inject lateinit var locationDataContainer: LastLocationDataContainer

    private var selectedLat: Double? = null
    private var selectedLon: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        onCreateViewGeneral()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    MapPickerContent(
                        initialLocation = locationDataContainer.lastLocation?.let {
                            GeoPoint(it.latitude, it.longitude)
                        },
                        onLocationSelected = { lat, lon ->
                            selectedLat = lat
                            selectedLon = lon
                            aapsLogger.debug(LTag.AUTOMATION, "Location selected: $lat, $lon")
                        },
                        onOkClick = {
                            if (submit()) {
                                dismiss()
                            }
                        },
                        onCancelClick = {
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    override fun submit(): Boolean {
        val lat = selectedLat
        val lon = selectedLon

        if (lat == null || lon == null) {
            return false
        }

        rxBus.send(EventPlaceSelected(lat, lon, "$lat, $lon"))
        return true
    }
}

@Composable
private fun MapPickerContent(
    initialLocation: GeoPoint?,
    onLocationSelected: (Double, Double) -> Unit,
    onOkClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    var selectedCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            Text(
                text = stringResource(R.string.pick_from_map),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                OsmdroidMapView(
                    initialLocation = initialLocation,
                    onLocationSelected = { lat, lon ->
                        selectedCoords = lat to lon
                        onLocationSelected(lat, lon)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selected coordinates text
            Text(
                text = selectedCoords?.let { (lat, lon) ->
                    stringResource(R.string.selected_coords, lat, lon)
                } ?: stringResource(R.string.tap_to_select_location),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(app.aaps.core.ui.R.string.cancel))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onOkClick,
                    modifier = Modifier.weight(1f),
                    enabled = selectedCoords != null
                ) {
                    Text(stringResource(app.aaps.core.ui.R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun OsmdroidMapView(
    initialLocation: GeoPoint?,
    onLocationSelected: (Double, Double) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var selectedMarker by remember { mutableStateOf<Marker?>(null) }

    // Handle lifecycle events for MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            // Configure osmdroid
            Configuration.getInstance().userAgentValue = "AndroidAPS/1.0"

            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)

                // Set initial location - use current location if available, otherwise default to London
                val startPoint = initialLocation ?: GeoPoint(51.5074, -0.1278)
                controller.setCenter(startPoint)

                // Add current location marker if available
                initialLocation?.let { point ->
                    val currentMarker = Marker(this)
                    currentMarker.position = point
                    currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    currentMarker.title = "Current Location"
                    currentMarker.icon = ContextCompat.getDrawable(context, R.drawable.ic_my_location)
                    overlays.add(currentMarker)
                }

                // Add tap listener
                val mapViewRef = this
                val mapEventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        // Update or create selected marker
                        if (selectedMarker == null) {
                            selectedMarker = Marker(mapViewRef).apply {
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            mapViewRef.overlays.add(selectedMarker)
                        }
                        selectedMarker?.position = p
                        selectedMarker?.title = "Selected Location"
                        mapViewRef.invalidate()

                        onLocationSelected(p.latitude, p.longitude)
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint): Boolean = false
                }

                val eventsOverlay = MapEventsOverlay(mapEventsReceiver)
                overlays.add(0, eventsOverlay)

                mapView = this
            }
        },
        update = { view ->
            mapView = view
        }
    )
}
