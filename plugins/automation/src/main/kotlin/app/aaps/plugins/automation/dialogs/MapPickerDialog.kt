package app.aaps.plugins.automation.dialogs

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.plugins.automation.R
import app.aaps.plugins.automation.events.EventPlaceSelected
import app.aaps.plugins.automation.services.LastLocationDataContainer
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
        val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AapsTheme(darkTheme = isDarkTheme) {
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
private fun AapsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val lightColors = lightColorScheme(
        primary = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_primary),
        onPrimary = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_onPrimary),
        primaryContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_primaryContainer),
        onPrimaryContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_onPrimaryContainer),
        secondary = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_secondary),
        onSecondary = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_onSecondary),
        secondaryContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_secondaryContainer),
        onSecondaryContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_onSecondaryContainer),
        tertiary = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_tertiary),
        onTertiary = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_onTertiary),
        tertiaryContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_tertiaryContainer),
        onTertiaryContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_onTertiaryContainer),
        error = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_error),
        onError = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_onError),
        errorContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_errorContainer),
        onErrorContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_onErrorContainer),
        background = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_background),
        onBackground = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_onBackground),
        surface = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_surface),
        onSurface = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_onSurface),
        surfaceVariant = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_surfaceVariant),
        onSurfaceVariant = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_onSurfaceVariant),
        outline = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_outline),
        inverseSurface = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_inverseSurface),
        inverseOnSurface = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_inverseOnSurface),
        inversePrimary = colorResource(app.aaps.core.ui.R.color.aaps_theme_light_inversePrimary),
    )

    val darkColors = darkColorScheme(
        primary = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_primary),
        onPrimary = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_onPrimary),
        primaryContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_primaryContainer),
        onPrimaryContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_onPrimaryContainer),
        secondary = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_secondary),
        onSecondary = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_onSecondary),
        secondaryContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_secondaryContainer),
        onSecondaryContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_onSecondaryContainer),
        tertiary = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_tertiary),
        onTertiary = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_onTertiary),
        tertiaryContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_tertiaryContainer),
        onTertiaryContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_onTertiaryContainer),
        error = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_error),
        onError = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_onError),
        errorContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_errorContainer),
        onErrorContainer = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_onErrorContainer),
        background = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_background),
        onBackground = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_onBackground),
        surface = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_surface),
        onSurface = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_onSurface),
        surfaceVariant = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_surfaceVariant),
        onSurfaceVariant = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_onSurfaceVariant),
        outline = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_outline),
        inverseSurface = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_inverseSurface),
        inverseOnSurface = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_inverseOnSurface),
        inversePrimary = colorResource(app.aaps.core.ui.R.color.aaps_theme_dark_inversePrimary),
    )

    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        content = content
    )
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
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = stringResource(R.string.pick_from_map),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Map - takes up available space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons - always at bottom
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
                    enabled = selectedCoords != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
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
            org.osmdroid.config.Configuration.getInstance().userAgentValue = "AndroidAPS/1.0"

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
