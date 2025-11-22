package app.aaps.plugins.automation.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy
import app.aaps.core.ui.toast.ToastUtils
import app.aaps.plugins.automation.R
import app.aaps.plugins.automation.databinding.AutomationDialogPlaceSearchBinding
import app.aaps.plugins.automation.events.EventPlaceSelected
import app.aaps.plugins.automation.location.NominatimPlace
import app.aaps.plugins.automation.location.NominatimService
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import javax.inject.Inject

class PlaceSearchDialog : BaseDialog() {

    @Inject lateinit var rxBus: RxBus
    @Inject lateinit var nominatimService: NominatimService
    @Inject lateinit var aapsSchedulers: AapsSchedulers
    @Inject lateinit var fabricPrivacy: FabricPrivacy

    private var _binding: AutomationDialogPlaceSearchBinding? = null
    private val binding get() = _binding!!

    private val disposable = CompositeDisposable()
    private var selectedPlace: NominatimPlace? = null
    private val places = mutableListOf<NominatimPlace>()
    private lateinit var adapter: PlaceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        onCreateViewGeneral()
        _binding = AutomationDialogPlaceSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PlaceAdapter(places) { place ->
            selectedPlace = place
            adapter.setSelected(place)
        }

        binding.resultsList.layoutManager = LinearLayoutManager(context)
        binding.resultsList.adapter = adapter

        binding.searchButton.setOnClickListener {
            performSearch()
        }

        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val query = binding.searchInput.text?.toString()?.trim() ?: return
        if (query.isEmpty()) return

        binding.progress.visibility = View.VISIBLE
        binding.noResults.visibility = View.GONE
        binding.resultsList.visibility = View.GONE

        disposable += nominatimService.search(query, limit = 10)
            .observeOn(aapsSchedulers.main)
            .subscribe({ results ->
                binding.progress.visibility = View.GONE
                places.clear()
                places.addAll(results)
                adapter.notifyDataSetChanged()

                if (results.isEmpty()) {
                    binding.noResults.visibility = View.VISIBLE
                    binding.resultsList.visibility = View.GONE
                } else {
                    binding.noResults.visibility = View.GONE
                    binding.resultsList.visibility = View.VISIBLE
                }
            }, { error ->
                binding.progress.visibility = View.GONE
                binding.noResults.visibility = View.VISIBLE
                aapsLogger.error(LTag.AUTOMATION, "Search error", error)
                context?.let { ToastUtils.errorToast(it, R.string.search_error) }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.clear()
        _binding = null
    }

    override fun submit(): Boolean {
        selectedPlace?.let { place ->
            rxBus.send(EventPlaceSelected(place.latitude, place.longitude, place.displayName))
            return true
        }
        return false
    }

    private inner class PlaceAdapter(
        private val items: List<NominatimPlace>,
        private val onItemClick: (NominatimPlace) -> Unit
    ) : RecyclerView.Adapter<PlaceAdapter.ViewHolder>() {

        private var selectedPosition = -1

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val placeName: TextView = view.findViewById(R.id.place_name)
            val placeCoords: TextView = view.findViewById(R.id.place_coords)

            init {
                view.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(items[position])
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.automation_place_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val place = items[position]
            holder.placeName.text = place.displayName
            holder.placeCoords.text = String.format("%.6f, %.6f", place.latitude, place.longitude)

            // Highlight selected item
            holder.itemView.isSelected = position == selectedPosition
            holder.itemView.setBackgroundResource(
                if (position == selectedPosition)
                    app.aaps.core.ui.R.color.colorLightGray
                else
                    android.R.color.transparent
            )
        }

        override fun getItemCount() = items.size

        fun setSelected(place: NominatimPlace) {
            val newPosition = items.indexOf(place)
            val oldPosition = selectedPosition
            selectedPosition = newPosition
            if (oldPosition >= 0) notifyItemChanged(oldPosition)
            if (newPosition >= 0) notifyItemChanged(newPosition)
        }
    }
}
