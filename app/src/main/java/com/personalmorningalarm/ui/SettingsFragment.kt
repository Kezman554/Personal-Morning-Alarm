package com.personalmorningalarm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.personalmorningalarm.R
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.AppDatabase
import com.personalmorningalarm.data.model.ContentType
import com.personalmorningalarm.databinding.FragmentSettingsBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        ViewModelFactory(AlarmRepository(AppDatabase.getInstance(requireContext())))
    }

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

        binding.btnManageTags.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_nfcTags)
        }

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

    private companion object {
        /** Picker ceiling (also allows more if the user registers more tags). */
        const val MAX_SEQUENCE_LENGTH = 10
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
