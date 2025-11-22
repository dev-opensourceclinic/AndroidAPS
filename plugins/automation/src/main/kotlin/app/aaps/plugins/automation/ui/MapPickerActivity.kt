package app.aaps.plugins.automation.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.ui.locale.LocaleHelper
import app.aaps.plugins.automation.R
import app.aaps.plugins.automation.events.EventPlaceSelected
import app.aaps.plugins.automation.services.LastLocationDataContainer
import dagger.android.support.DaggerAppCompatActivity
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject

class MapPickerActivity : DaggerAppCompatActivity() {

    @Inject lateinit var rxBus: RxBus
    @Inject lateinit var locationDataContainer: LastLocationDataContainer

    private var selectedLat: Double? = null
    private var selectedLon: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        val initialLat = intent.getDoubleExtra(EXTRA_LATITUDE, Double.NaN)
        val initialLon = intent.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN)
        val initialLocation = if (!initialLat.isNaN() && !initialLon.isNaN()) {
            GeoPoint(initialLat, initialLon)
        } else {
            locationDataContainer.lastLocation?.let {
                GeoPoint(it.latitude, it.longitude)
            }
        }

        setContent {
            AapsTheme(darkTheme = isDarkTheme) {
                MapPickerScreen(
                    initialLocation = initialLocation,
                    onLocationSelected = { lat, lon ->
                        selectedLat = lat
                        selectedLon = lon
                    },
                    onConfirm = {
                        val lat = selectedLat
                        val lon = selectedLon
                        if (lat != null && lon != null) {
                            rxBus.send(EventPlaceSelected(lat, lon, "$lat, $lon"))
                        }
                        finish()
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    companion object {
        private const val EXTRA_LATITUDE = "latitude"
        private const val EXTRA_LONGITUDE = "longitude"

        fun createIntent(context: Context, latitude: Double? = null, longitude: Double? = null): Intent {
            return Intent(context, MapPickerActivity::class.java).apply {
                latitude?.let { putExtra(EXTRA_LATITUDE, it) }
                longitude?.let { putExtra(EXTRA_LONGITUDE, it) }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapPickerScreen(
    initialLocation: GeoPoint?,
    onLocationSelected: (Double, Double) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pick_from_map)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(app.aaps.core.ui.R.string.cancel)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onConfirm,
                        enabled = selectedCoords != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(app.aaps.core.ui.R.string.ok),
                            tint = if (selectedCoords != null)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map takes all available space
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

            // Coordinates display at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedCoords?.let { (lat, lon) ->
                        stringResource(R.string.selected_coords, lat, lon)
                    } ?: stringResource(R.string.tap_to_select_location),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
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
    val currentOnLocationSelected by rememberUpdatedState(onLocationSelected)
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var selectedMarker: Marker? = remember { null }

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
            org.osmdroid.config.Configuration.getInstance().userAgentValue = "AndroidAPS/1.0"

            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)

                val startPoint = initialLocation ?: GeoPoint(51.5074, -0.1278)
                controller.setCenter(startPoint)

                initialLocation?.let { point ->
                    val currentMarker = Marker(this)
                    currentMarker.position = point
                    currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    currentMarker.title = "Current Location"
                    currentMarker.icon = ContextCompat.getDrawable(context, R.drawable.ic_my_location)
                    overlays.add(currentMarker)
                }

                val mapViewRef = this
                val mapEventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        if (selectedMarker == null) {
                            selectedMarker = Marker(mapViewRef).apply {
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            mapViewRef.overlays.add(selectedMarker)
                        }
                        selectedMarker?.position = p
                        selectedMarker?.title = "Selected Location"
                        mapViewRef.invalidate()

                        currentOnLocationSelected(p.latitude, p.longitude)
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint): Boolean = false
                }

                val eventsOverlay = MapEventsOverlay(mapEventsReceiver)
                overlays.add(0, eventsOverlay)

                mapView = this
            }
        }
    )
}
