package app.aaps.plugins.insulin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import app.aaps.core.data.model.ICfg
import app.aaps.core.data.ue.Action
import app.aaps.core.data.ue.Sources
import app.aaps.core.data.ue.ValueWithUnit
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.insulin.ConcentrationType
import app.aaps.core.interfaces.insulin.InsulinType
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.UserEntryLogger
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.ui.UiInteraction
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.interfaces.utils.HardLimits
import app.aaps.core.interfaces.utils.SafeParse
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.ui.dialogs.OKDialog
import app.aaps.core.ui.extensions.toVisibility
import app.aaps.plugins.insulin.databinding.InsulinNewFragmentBinding
import dagger.android.support.DaggerFragment
import java.text.DecimalFormat
import javax.inject.Inject

class InsulinNewFragment : DaggerFragment() {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var activePlugin: ActivePlugin
    @Inject lateinit var hardLimits: HardLimits
    @Inject lateinit var rh: ResourceHelper
    @Inject lateinit var insulinPlugin: InsulinPlugin
    @Inject lateinit var profileFunction: ProfileFunction
    @Inject lateinit var uel: UserEntryLogger
    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var uiInteraction: UiInteraction
    @Inject lateinit var dateUtil: DateUtil
    @Inject lateinit var config: Config
    @Inject lateinit var rxBus: RxBus
    @Inject lateinit var aapsSchedulers: AapsSchedulers
    @Inject lateinit var fabricPrivacy: FabricPrivacy


    private var _binding: InsulinNewFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val currentInsulin: ICfg get() = insulinPlugin.currentInsulin
    private var selectedTemplate = InsulinType.OREF_RAPID_ACTING    // Default Insulin (should only be used on new install)
    private var selectedConcentration = ConcentrationType.U100    // Default Concentration
    private val insulinList: List<CharSequence> get() = insulinPlugin.insulinList(0.0)
    private val minPeak: Double get() = hardLimits.minPeak().toDouble()
    private val maxPeak: Double get() = hardLimits.maxPeak().toDouble()

    private val textWatch = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            currentInsulin.insulinLabel = binding.name.text.toString()
            currentInsulin.setPeak(SafeParse.stringToInt(binding.peak.text))
            currentInsulin.setDia(SafeParse.stringToDouble(binding.dia.text))
            updateGui(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = InsulinNewFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        if (insulinPlugin.numOfInsulins == 0) {
            insulinPlugin.loadSettings()
        }
        insulinPlugin.setCurrent(insulinPlugin.iCfg)

        val insulinTemplateList: List<CharSequence> = insulinPlugin.insulinTemplateLabelList()
        setUpSpinnerAdapter(binding.insulinTemplate, insulinTemplateList)
        binding.insulinTemplate.setText(rh.gs(InsulinType.fromPeak(currentInsulin.insulinPeakTime).label), false)
        binding.insulinTemplateText.text = rh.gs(InsulinType.fromPeak(currentInsulin.insulinPeakTime).label)

        val concentrationList: List<CharSequence> = insulinPlugin.concentrationLabelList()
        setUpSpinnerAdapter(binding.concentrationList, concentrationList)
        binding.concentrationList.setText(rh.gs(ConcentrationType.U100.label), false)
    }

    @Synchronized
    override fun onResume() {
        super.onResume()
        updateGui()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateGui(withTextFields: Boolean = true) {
        if (_binding == null) return
        if (withTextFields) updateTextField()
        updateFormFields()
        updateButtonVisibility()
        updateValidationState()
        updateGraph()
        setUpSpinnerAdapter(binding.insulinList, insulinList)
    }

    private fun setupClickListeners() {

        binding.insulinList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (insulinPlugin.hasUnsavedChanges) { // Switch insulin Confirmation
                activity?.let { activity ->
                    OKDialog.showConfirmation(
                        activity, rh.gs(R.string.do_you_want_switch_insulin),
                        {
                            insulinPlugin.currentInsulinIndex = position
                            insulinPlugin.currentInsulin = insulinPlugin.deepClone(insulinPlugin.currentInsulin())
                            updateGui()
                        }, null
                    )
                }
            } else {
                insulinPlugin.currentInsulinIndex = position
                insulinPlugin.currentInsulin = insulinPlugin.deepClone(insulinPlugin.currentInsulin())
                selectedTemplate = InsulinType.fromInt(insulinPlugin.currentInsulin.insulinTemplate)
                updateGui()
            }
        }

        binding.insulinTemplate.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            selectedTemplate = insulinPlugin.insulinTemplateList()[position]
            insulinPlugin.currentInsulin = insulinPlugin.deepClone(selectedTemplate.getICfg(rh), true).also {
                it.insulinLabel = insulinPlugin.createNewInsulinLabel(it, insulinPlugin.currentInsulinIndex)
                it.concentration = selectedConcentration.value
                it.isNew = true
            }
            updateGui()
        }

        binding.concentrationList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            selectedConcentration = insulinPlugin.concentrationList()[position]
            insulinPlugin.currentInsulin.concentration = selectedConcentration.value
            updateGui()
        }

        setUpSpinnerAdapter(binding.insulinList, insulinList)

        binding.insulinAdd.setOnClickListener {
            if (insulinPlugin.hasUnsavedChanges) {
                activity?.let { OKDialog.show(it, "", rh.gs(R.string.save_or_reset_changes_first)) }
            } else {
                selectedTemplate = InsulinType.OREF_RAPID_ACTING
                insulinPlugin.addNewInsulin(insulinPlugin.deepClone(selectedTemplate.iCfg, true).also { it.isNew = true})
                binding.concentrationList.setText(rh.gs(ConcentrationType.U100.label), false)
                updateGui()
            }
        }
        binding.insulinRemove.setOnClickListener {
            insulinPlugin.removeCurrentInsulin(activity)
            selectedTemplate = InsulinType.fromInt(currentInsulin.insulinTemplate)
            updateGui()
        }
        binding.reset.setOnClickListener {
            insulinPlugin.currentInsulin = insulinPlugin.deepClone(insulinPlugin.currentInsulin())
            selectedTemplate = InsulinType.fromInt(currentInsulin.insulinTemplate)
            updateGui()
        }
        binding.save.setOnClickListener {
            if (!insulinPlugin.isValidEditState(activity)) return@setOnClickListener
            saveCurrentInsulin()
        }
        binding.autoName.setOnClickListener {
            binding.name.setText(insulinPlugin.createNewInsulinLabel(currentInsulin, insulinPlugin.currentInsulinIndex, selectedTemplate))
            updateGui()
        }
    }

    private fun updateTextField() {
        binding.name.removeTextChangedListener(textWatch)
        binding.name.setText(currentInsulin.insulinLabel)
        binding.name.addTextChangedListener(textWatch)
    }

    private fun updateFormFields() {
        binding.insulinList.setText(insulinPlugin.currentInsulinDisplayName, false)
        binding.insulinTemplate.setText(insulinPlugin.getTemplateDisplayName(selectedTemplate), false)
        binding.insulinTemplateText.text = insulinPlugin.getTemplateDisplayName(selectedTemplate)

        binding.peak.setParams(currentInsulin.peak.toDouble(), minPeak, maxPeak, 1.0, DecimalFormat("0"), false, null, textWatch)
        binding.dia.setParams(currentInsulin.dia, hardLimits.minDia(), hardLimits.maxDia(), 0.1, DecimalFormat("0.0"), false, null, textWatch)
        binding.concentrationR.text = rh.gs(R.string.concentration_with_unit, currentInsulin.concentration * 100)
        binding.peakR.text = rh.gs(app.aaps.core.ui.R.string.format_mins, currentInsulin.peak)

        binding.peakEdit.visibility = insulinPlugin.isPeakEditable.toVisibility()
        binding.peakRead.visibility = (!insulinPlugin.isPeakEditable).toVisibility()
    }

    private fun updateButtonVisibility() {
        binding.save.visibility = (insulinPlugin.hasUnsavedChanges && insulinPlugin.isValidEditState(activity)).toVisibility()
        binding.reset.visibility = (insulinPlugin.hasUnsavedChanges && !currentInsulin.isNew).toVisibility()
        binding.insulinRemove.visibility = insulinPlugin.canRemoveCurrentInsulin.toVisibility()
        binding.insulinTemplateMenu.visibility = (currentInsulin.isNew).toVisibility()
        binding.insulinTemplateRead.visibility = (!currentInsulin.isNew).toVisibility()
        binding.concentrationListMenu.visibility = (currentInsulin.isNew).toVisibility()
        binding.concentrationRead.visibility = (!currentInsulin.isNew).toVisibility()
        binding.peakEdit.visibility = (selectedTemplate == InsulinType.OREF_FREE_PEAK).toVisibility()
        binding.peakRead.visibility = (selectedTemplate != InsulinType.OREF_FREE_PEAK).toVisibility()
    }

    private fun updateValidationState() {
        val isValid = insulinPlugin.isValidEditState(activity, false)
        val bgColor = if (isValid) app.aaps.core.ui.R.attr.okBackgroundColor else app.aaps.core.ui.R.attr.errorBackgroundColor
        view?.setBackgroundColor(rh.gac(context, bgColor))
        binding.insulinList.isEnabled = isValid
    }

    private fun saveCurrentInsulin() {
        currentInsulin.isNew = false
        uel.log(
            action = Action.STORE_INSULIN,
            source = Sources.Insulin,
            value = ValueWithUnit.SimpleString(currentInsulin.insulinLabel)
        )
        insulinPlugin.insulins[insulinPlugin.currentInsulinIndex] = insulinPlugin.deepClone(currentInsulin).also { it.insulinTemplate = selectedTemplate.value }
        insulinPlugin.storeSettings()
        updateGui()
    }

    private fun updateGraph() {
        binding.graph.show(currentInsulin)
    }

    private fun setUpSpinnerAdapter(spinner: AutoCompleteTextView, items: List<CharSequence>) {
        context?.let { context ->
            spinner.setAdapter(ArrayAdapter(context, app.aaps.core.ui.R.layout.spinner_centered, items))
        }
    }
}