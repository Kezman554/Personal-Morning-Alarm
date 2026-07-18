package com.personalmorningalarm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.personalmorningalarm.R
import com.personalmorningalarm.data.model.WeekSchedule
import com.personalmorningalarm.data.model.WeekScheduleDto
import com.personalmorningalarm.data.remote.AlfredRepository
import com.personalmorningalarm.data.remote.AlfredResult
import com.personalmorningalarm.databinding.FragmentWeekScheduleBinding
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The Daily Schedule tile's screen: the whole plan week, one swipeable page per
 * day, opening on today. The plan's first and last days are hard edges — no
 * paging into other weeks. The alarm-flow content screen and the Today screen
 * are deliberately not this: they stay today-only on GET /daily-schedule.
 */
class WeekScheduleFragment : Fragment() {

    private var _binding: FragmentWeekScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeekScheduleViewModel by viewModels {
        AlfredViewModelFactory(AlfredRepository(requireContext()))
    }

    private val dayAdapter = WeekDayAdapter()

    /** Anchor on today once per fetch, not on every re-render mid-swipe. */
    private var anchored = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeekScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pagerWeek.adapter = dayAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: WeekScheduleViewModel.WeekState) {
        binding.tvWeekNote.isVisible = false
        when (val result = state.week) {
            null -> showStatus(R.string.schedule_loading)
            AlfredResult.Unavailable -> showStatus(R.string.schedule_unavailable)
            is AlfredResult.Fresh -> showWeek(result.data)
            is AlfredResult.Stale -> {
                binding.tvWeekNote.isVisible = true
                binding.tvWeekNote.text = getString(R.string.schedule_stale)
                showWeek(result.data)
            }
        }
    }

    private fun showWeek(dto: WeekScheduleDto) {
        val today = LocalDate.now()
        val days = WeekSchedule.days(dto)
        if (days.isEmpty()) {
            showStatus(R.string.week_no_plan)
            return
        }
        binding.tvWeekStatus.isVisible = false
        binding.pagerWeek.isVisible = true
        dayAdapter.setDays(days, today)
        if (!anchored) {
            anchored = true
            binding.pagerWeek.setCurrentItem(WeekSchedule.anchorIndex(days, today), false)
        }
    }

    private fun showStatus(text: Int) {
        binding.pagerWeek.isVisible = false
        binding.tvWeekStatus.isVisible = true
        binding.tvWeekStatus.text = getString(text)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
