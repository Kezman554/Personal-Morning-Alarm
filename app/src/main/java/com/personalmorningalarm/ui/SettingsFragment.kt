package com.personalmorningalarm.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.Slider
import com.personalmorningalarm.R
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.AppDatabase
import com.personalmorningalarm.data.entity.AlarmConfig
import com.personalmorningalarm.data.model.AlarmSound
import com.personalmorningalarm.data.model.AlarmSounds
import com.personalmorningalarm.data.model.ContentType
import com.personalmorningalarm.databinding.DialogDurationSliderBinding
import com.personalmorningalarm.databinding.FragmentSettingsBinding
import com.personalmorningalarm.util.BatteryOptimisationHelper
import com.personalmorningalarm.util.PinManager
import com.personalmorningalarm.util.SoundPreviewPlayer
import com.personalmorningalarm.util.ThemeManager
import com.personalmorningalarm.util.ThemeMode
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        ViewModelFactory(AlarmRepository(AppDatabase.getInstance(requireContext())))
    }

    private lateinit var pinManager: PinManager
    private lateinit var themeManager: ThemeManager
    private val preview = SoundPreviewPlayer()

    /** Cached so the Stage 2 picker dialog opens on the current saved value. */
    private var stage2Duration: Int = DEFAULT_STAGE2_DURATION

    // Cached current sound keys so the row Preview buttons play the saved choice.
    private var stage1SoundKey: String = AlarmSounds.defaultStage1.key
    private var nuclearSoundKey: String = AlarmSounds.defaultNuclear.key

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pinManager = PinManager(requireContext())
        themeManager = ThemeManager(requireContext())

        // Theme: per-button onClick (not the group listener) so setting the initial
        // checked state below doesn't loop. Changing it recreates the activity, so the
        // new theme shows immediately.
        binding.rgTheme.check(
            when (themeManager.getThemeMode()) {
                ThemeMode.LIGHT -> R.id.rb_theme_light
                ThemeMode.DARK -> R.id.rb_theme_dark
                ThemeMode.SYSTEM -> R.id.rb_theme_system
            }
        )
        binding.rbThemeLight.setOnClickListener { themeManager.setThemeMode(ThemeMode.LIGHT) }
        binding.rbThemeDark.setOnClickListener { themeManager.setThemeMode(ThemeMode.DARK) }
        binding.rbThemeSystem.setOnClickListener { themeManager.setThemeMode(ThemeMode.SYSTEM) }

        binding.btnBattery.setOnClickListener {
            BatteryOptimisationHelper.showExplanationDialog(requireContext())
        }

        binding.btnManageTags.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_nfcTags)
        }

        binding.btnManageQuotes.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_quotes)
        }

        binding.btnManageRoutines.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_routines)
        }

        // Alarm sounds: tap a row to pick (previewing each), or Preview the saved one.
        binding.rowStage1Sound.setOnClickListener {
            pickSound(R.string.sound_pick_stage1_title, AlarmSounds.stage1, stage1SoundKey, stage1Volume()) {
                viewModel.setStage1Sound(it)
            }
        }
        binding.btnPreviewStage1.setOnClickListener {
            preview.play(requireContext(), AlarmSounds.stage1ByKey(stage1SoundKey), stage1Volume())
        }
        binding.rowNuclearSound.setOnClickListener {
            // Nuclear always plays at full volume, so preview it at full too.
            pickSound(R.string.sound_pick_nuclear_title, AlarmSounds.nuclear, nuclearSoundKey, 1f) {
                viewModel.setNuclearSound(it)
            }
        }
        binding.btnPreviewNuclear.setOnClickListener {
            preview.play(requireContext(), AlarmSounds.nuclearByKey(nuclearSoundKey), 1f)
        }

        // Volume: persist once on release (not on every drag tick).
        binding.sliderVolume.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.setStage1Volume(slider.value.toInt())
            }
        })

        binding.switchVibration.setOnClickListener {
            viewModel.setVibrationEnabled(binding.switchVibration.isChecked)
        }

        // Stage 2 time limit — PIN-protected before the slider opens.
        binding.rowStage2Duration.setOnClickListener {
            PinPrompts.guard(requireContext(), pinManager) { showStage2DurationDialog() }
        }

        binding.btnSetPin.setOnClickListener {
            PinPrompts.setup(requireContext(), pinManager) { ok ->
                if (ok) {
                    toast(getString(R.string.pin_set_done))
                    renderPinSection()
                }
            }
        }
        binding.btnChangePin.setOnClickListener {
            PinPrompts.change(requireContext(), pinManager) { ok ->
                if (ok) toast(getString(R.string.pin_changed))
            }
        }
        binding.btnDisablePin.setOnClickListener {
            PinPrompts.disable(requireContext(), pinManager) { ok ->
                if (ok) {
                    toast(getString(R.string.pin_disabled))
                    renderPinSection()
                }
            }
        }
        renderPinSection()

        binding.pickerNfcTaps.minValue = 1
        // setValue (used when rendering) doesn't fire this, so no feedback loop.
        binding.pickerNfcTaps.setOnValueChangedListener { _, _, newVal ->
            viewModel.setSequenceLength(newVal)
        }

        // onClick fires only on user taps (not programmatic setChecked), so no feedback loop.
        binding.switchQuote.setOnClickListener {
            viewModel.setContentEnabled(ContentType.QUOTE, binding.switchQuote.isChecked)
        }
        binding.switchStretch.setOnClickListener {
            val enabled = binding.switchStretch.isChecked
            viewModel.setContentEnabled(ContentType.STRETCH, enabled)
            binding.stretchDurationGroup.visibility = if (enabled) View.VISIBLE else View.GONE
        }
        binding.switchPlaceholder.setOnClickListener {
            viewModel.setContentEnabled(ContentType.PLACEHOLDER, binding.switchPlaceholder.isChecked)
        }

        // onClick (not the group's checked-change) so programmatic updates don't loop.
        binding.rbStretch5.setOnClickListener { viewModel.setStretchDuration(5) }
        binding.rbStretch10.setOnClickListener { viewModel.setStretchDuration(10) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.sequenceLength,
                    viewModel.activeTagCount
                ) { length, tagCount -> length to tagCount }
                    .collect { (length, tagCount) -> renderTapPicker(length, tagCount) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stage2Duration.collect { minutes ->
                    stage2Duration = minutes
                    binding.tvStage2DurationValue.text =
                        getString(R.string.stage2_duration_value, minutes)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.config.collect(::renderSoundSettings)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.contentToggles.collect { toggles ->
                    toggles.forEach { t ->
                        when (t.contentType) {
                            ContentType.QUOTE -> binding.switchQuote.isChecked = t.isEnabled
                            ContentType.STRETCH -> {
                                binding.switchStretch.isChecked = t.isEnabled
                                binding.stretchDurationGroup.visibility =
                                    if (t.isEnabled) View.VISIBLE else View.GONE
                                binding.rgStretchDuration.check(
                                    if (t.durationMinutes == 10) R.id.rb_stretch_10
                                    else R.id.rb_stretch_5
                                )
                            }
                            ContentType.PLACEHOLDER -> binding.switchPlaceholder.isChecked = t.isEnabled
                        }
                    }
                }
            }
        }
    }

    /**
     * Reflects the saved checkpoint count against the registered-tag pool. The
     * ceiling is fixed (every registered tag, plus headroom for repeats) so it
     * never collapses when the user dials the value down below the tag count.
     * Values above the tag count are allowed — the hint warns those taps repeat.
     */
    private fun renderTapPicker(length: Int, tagCount: Int) {
        val picker = binding.pickerNfcTaps
        if (tagCount == 0) {
            picker.isEnabled = false
            picker.maxValue = 1
            picker.value = 1
            binding.nfcTapsHint.text = getString(R.string.nfc_taps_no_tags)
            return
        }
        picker.isEnabled = true
        val max = maxOf(tagCount, MAX_SEQUENCE_LENGTH)
        // maxValue must be set before value; neither setter fires the listener.
        picker.maxValue = max
        val value = length.coerceIn(1, max)
        picker.value = value
        binding.nfcTapsHint.text = if (value > tagCount) {
            getString(R.string.nfc_taps_hint_repeats, value, tagCount)
        } else {
            getString(R.string.nfc_taps_hint, value, tagCount)
        }
    }

    /** Shows the Set / Change+Disable buttons depending on whether a PIN exists. */
    private fun renderPinSection() {
        val pinSet = pinManager.isPinSet()
        binding.tvPinStatus.text =
            getString(if (pinSet) R.string.pin_status_on else R.string.pin_status_off)
        binding.btnSetPin.visibility = if (pinSet) View.GONE else View.VISIBLE
        binding.btnChangePin.visibility = if (pinSet) View.VISIBLE else View.GONE
        binding.btnDisablePin.visibility = if (pinSet) View.VISIBLE else View.GONE
    }

    /** Reflects saved sound choices, volume, and vibration onto the controls. */
    private fun renderSoundSettings(config: AlarmConfig?) {
        val stage1 = AlarmSounds.stage1ByKey(config?.stage1SoundId)
        val nuclear = AlarmSounds.nuclearByKey(config?.nuclearSoundId)
        stage1SoundKey = stage1.key
        nuclearSoundKey = nuclear.key
        binding.tvStage1Sound.text = stage1.displayName
        binding.tvNuclearSound.text = nuclear.displayName

        val volume = (config?.stage1Volume ?: 100).coerceIn(0, 100).toFloat()
        if (binding.sliderVolume.value != volume) binding.sliderVolume.value = volume

        binding.switchVibration.isChecked = config?.vibrationEnabled ?: true
    }

    /** The live Stage 1 volume slider value as a 0f-1f fraction, for previews. */
    private fun stage1Volume(): Float = (binding.sliderVolume.value / 100f).coerceIn(0f, 1f)

    /**
     * Single-choice sound picker that previews each sound as it's tapped, saving
     * only on confirm. Preview stops when the dialog closes.
     */
    private fun pickSound(
        @StringRes title: Int,
        sounds: List<AlarmSound>,
        currentKey: String,
        previewVolume: Float,
        onPick: (String) -> Unit
    ) {
        val names = sounds.map { it.displayName }.toTypedArray()
        var selected = sounds.indexOfFirst { it.key == currentKey }.coerceAtLeast(0)
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(names, selected) { _, which ->
                selected = which
                preview.play(requireContext(), sounds[which], previewVolume)
            }
            .setPositiveButton(R.string.save) { _, _ -> onPick(sounds[selected].key) }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener { preview.stop() }
            .show()
    }

    /** Slider dialog (5–15 min) for the Stage 2 time limit. */
    private fun showStage2DurationDialog() {
        val dialogBinding = DialogDurationSliderBinding.inflate(layoutInflater)
        val start = stage2Duration.coerceIn(MIN_STAGE2_DURATION, MAX_STAGE2_DURATION)
        dialogBinding.sliderDuration.value = start.toFloat()
        dialogBinding.tvDurationValue.text = getString(R.string.stage2_duration_value, start)
        dialogBinding.sliderDuration.addOnChangeListener { _, value, _ ->
            dialogBinding.tvDurationValue.text =
                getString(R.string.stage2_duration_value, value.toInt())
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.stage2_duration_dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.setStage2Duration(dialogBinding.sliderDuration.value.toInt())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        /** Picker ceiling (also allows more if the user registers more tags). */
        const val MAX_SEQUENCE_LENGTH = 10

        /** Stage 2 time-limit bounds, per the PRD (5–15 min). */
        const val MIN_STAGE2_DURATION = 5
        const val MAX_STAGE2_DURATION = 15
        const val DEFAULT_STAGE2_DURATION = 10
    }

    override fun onResume() {
        super.onResume()
        renderBatteryStatus()
    }

    /** Reflects the current battery-optimisation state (refreshed on return). */
    private fun renderBatteryStatus() {
        val ignoring = BatteryOptimisationHelper.isIgnoringBatteryOptimizations(requireContext())
        binding.tvBatteryStatus.text = getString(
            if (ignoring) R.string.battery_status_whitelisted else R.string.battery_status_optimised
        )
        binding.btnBattery.isEnabled = !ignoring
    }

    override fun onPause() {
        super.onPause()
        preview.stop() // don't let a preview keep playing after leaving Settings
    }

    override fun onDestroyView() {
        super.onDestroyView()
        preview.release()
        _binding = null
    }
}
