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

        // onClick fires only on user taps (not programmatic setChecked), so no feedback loop.
        binding.switchQuote.setOnClickListener {
            viewModel.setContentEnabled(ContentType.QUOTE, binding.switchQuote.isChecked)
        }
        binding.switchStretch.setOnClickListener {
            viewModel.setContentEnabled(ContentType.STRETCH, binding.switchStretch.isChecked)
        }
        binding.switchPlaceholder.setOnClickListener {
            viewModel.setContentEnabled(ContentType.PLACEHOLDER, binding.switchPlaceholder.isChecked)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.contentToggles.collect { toggles ->
                    toggles.forEach { t ->
                        when (t.contentType) {
                            ContentType.QUOTE -> binding.switchQuote.isChecked = t.isEnabled
                            ContentType.STRETCH -> binding.switchStretch.isChecked = t.isEnabled
                            ContentType.PLACEHOLDER -> binding.switchPlaceholder.isChecked = t.isEnabled
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
