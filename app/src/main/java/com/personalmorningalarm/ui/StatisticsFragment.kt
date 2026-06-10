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
import androidx.recyclerview.widget.LinearLayoutManager
import com.personalmorningalarm.R
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.AppDatabase
import com.personalmorningalarm.databinding.FragmentStatisticsBinding
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels {
        ViewModelFactory(AlarmRepository(AppDatabase.getInstance(requireContext())))
    }

    private val historyAdapter = EventHistoryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = historyAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: StatisticsUiState) {
        binding.tvCurrentStreak.text = state.currentStreak.toString()
        binding.tvLongestStreak.text = state.longestStreak.toString()
        binding.tvCurrentStreakUnit.text = streakUnit(state.currentStreak)
        binding.tvLongestStreakUnit.text = streakUnit(state.longestStreak)

        binding.weeklyChart.setData(state.weeks)
        binding.dayHeatmap.setData(state.days)

        historyAdapter.submitList(state.history)
        binding.tvHistoryEmpty.visibility = if (state.history.isEmpty()) View.VISIBLE else View.GONE
        binding.rvHistory.visibility = if (state.history.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun streakUnit(count: Int): String =
        getString(if (count == 1) R.string.stats_day_unit else R.string.stats_days_unit)

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvHistory.adapter = null // avoid leaking the view through the adapter
        _binding = null
    }
}
