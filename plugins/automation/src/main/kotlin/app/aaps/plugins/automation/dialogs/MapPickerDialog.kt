package app.aaps.plugins.automation.dialogs

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.plugins.automation.R
import app.aaps.plugins.automation.databinding.AutomationDialogMapPickerBinding
import app.aaps.plugins.automation.events.EventPlaceSelected
import app.aaps.plugins.automation.services.LastLocationDataContainer
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject

class MapPickerDialog : BaseDialog() {

    @Inject lateinit var rxBus: RxBus
    @Inject lateinit var locationDataContainer: LastLocationDataContainer

    private var _binding: AutomationDialogMapPickerBinding? = null
    private val binding get() = _binding!!

    private var selectedLat: Double? = null
    private var selectedLon: Double? = null
    private var marker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        onCreateViewGeneral()
        _binding = AutomationDialogMapPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure osmdroid
        Configuration.getInstance().userAgentValue = "AndroidAPS/1.0"

        // Setup map
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(15.0)

        // Set initial location - use current location if available, otherwise default
        val initialLocation = locationDataContainer.lastLocation?.let {
            GeoPoint(it.latitude, it.longitude)
        } ?: GeoPoint(51.5074, -0.1278) // Default to London

        binding.map.controller.setCenter(initialLocation)

        // Add current location marker if available
        locationDataContainer.lastLocation?.let { location ->
            addCurrentLocationMarker(GeoPoint(location.latitude, location.longitude))
        }

        // Add tap listener
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                selectLocation(p.latitude, p.longitude)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }

        val eventsOverlay = MapEventsOverlay(mapEventsReceiver)
        binding.map.overlays.add(0, eventsOverlay)
    }

    private fun addCurrentLocationMarker(point: GeoPoint) {
        val currentMarker = Marker(binding.map)
        currentMarker.position = point
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        currentMarker.title = "Current Location"
        context?.let { ctx ->
            currentMarker.icon = ContextCompat.getDrawable(ctx, R.drawable.ic_my_location)
        }
        binding.map.overlays.add(currentMarker)
    }

    private fun selectLocation(lat: Double, lon: Double) {
        selectedLat = lat
        selectedLon = lon

        // Update text
        binding.selectedLocation.text = getString(R.string.selected_coords, lat, lon)

        // Update or create marker
        if (marker == null) {
            marker = Marker(binding.map)
            marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            binding.map.overlays.add(marker)
        }
        marker?.position = GeoPoint(lat, lon)
        marker?.title = "Selected Location"

        binding.map.invalidate()

        aapsLogger.debug(LTag.AUTOMATION, "Location selected: $lat, $lon")
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
