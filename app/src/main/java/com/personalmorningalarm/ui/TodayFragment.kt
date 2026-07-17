package com.personalmorningalarm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.color.MaterialColors
import com.personalmorningalarm.R
import com.personalmorningalarm.data.model.ChalkboardTaskDto
import com.personalmorningalarm.data.model.DailySchedule
import com.personalmorningalarm.data.model.RollingTodo
import com.personalmorningalarm.data.model.ScheduleTaskDto
import com.personalmorningalarm.data.remote.AlfredRepository
import com.personalmorningalarm.data.remote.AlfredResult
import com.personalmorningalarm.databinding.FragmentTodayBinding
import kotlinx.coroutines.launch

/** Which feeds this instance shows — one per home tile, or both together. */
enum class TodaySection {
    BOTH,
    SCHEDULE,
    CHALKBOARD;

    companion object {
        /** Anything unrecognised shows everything, rather than an empty screen. */
        fun fromName(name: String?): TodaySection =
            entries.firstOrNull { it.name == name } ?: BOTH
    }
}

/**
 * Today: the schedule and the rolling to-do, on demand.
 *
 * A glance surface, outside the alarm entirely — no NFC gate, no dismiss timer,
 * nothing to tick off. The user opens it, reads it, and leaves. Rendering comes
 * from the same [ScheduleRenderer] / [ChalkboardRenderer] the Stage 2 screens use,
 * so the same content reads the same way wherever it's met.
 *
 * [ARG_SECTION] narrows it to a single feed, which is how the home tiles open it:
 * one screen per tile. Both feeds are still fetched either way — they share a
 * refresh, the cost is one extra request, and it keeps the caches warm for the
 * alarm's own content screens.
 */
class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TodayViewModel by viewModels {
        AlfredViewModelFactory(AlfredRepository(requireContext()))
    }

    private val section: TodaySection by lazy {
        TodaySection.fromName(arguments?.getString(ARG_SECTION))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // A tile opens a single feed; the Alarm page's button opens both.
        binding.sectionSchedule.isVisible = section != TodaySection.CHALKBOARD
        binding.sectionChalkboard.isVisible = section != TodaySection.SCHEDULE
        binding.sectionDivider.isVisible = section == TodaySection.BOTH

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: TodayViewModel.TodayState) {
        binding.swipeRefresh.isRefreshing = state.loading
        renderSchedule(state.schedule)
        renderChalkboard(state.chalkboard)
    }

    private fun renderSchedule(result: AlfredResult<List<ScheduleTaskDto>>?) {
        val tasks = sectionData(
            result = result,
            note = binding.tvScheduleNote,
            body = binding.tvScheduleBody,
            staleText = R.string.schedule_stale,
            loadingText = R.string.schedule_loading,
            unavailableText = R.string.schedule_unavailable
        ) ?: return

        val groups = DailySchedule.group(tasks)
        binding.tvScheduleBody.text = if (groups.isEmpty()) {
            getString(R.string.schedule_empty)
        } else {
            ScheduleRenderer.format(requireContext(), groups)
        }
    }

    private fun renderChalkboard(result: AlfredResult<List<ChalkboardTaskDto>>?) {
        val tasks = sectionData(
            result = result,
            note = binding.tvChalkboardNote,
            body = binding.tvChalkboardBody,
            staleText = R.string.chalkboard_stale,
            loadingText = R.string.chalkboard_loading,
            unavailableText = R.string.chalkboard_unavailable
        ) ?: return

        val items = RollingTodo.items(tasks)
        binding.tvChalkboardBody.text = if (items.isEmpty()) {
            getString(R.string.chalkboard_empty)
        } else {
            // Unlike the alarm panel's white-on-blue, this screen follows the theme.
            ChalkboardRenderer.format(items, dateColor())
        }
    }

    /**
     * The states both sections share: not fetched yet, unreachable with nothing
     * cached, or data (marked stale when it came from the cache). Returns the data
     * to render, or null when the section has already been given its message —
     * which is what keeps one feed's failure from touching the other.
     */
    private fun <T> sectionData(
        result: AlfredResult<List<T>>?,
        note: TextView,
        body: TextView,
        staleText: Int,
        loadingText: Int,
        unavailableText: Int
    ): List<T>? {
        note.visibility = View.GONE
        return when (result) {
            null -> {
                body.text = getString(loadingText)
                null
            }
            is AlfredResult.Fresh -> result.data
            is AlfredResult.Stale -> {
                note.visibility = View.VISIBLE
                note.text = getString(staleText)
                result.data
            }
            AlfredResult.Unavailable -> {
                body.text = getString(unavailableText)
                null
            }
        }
    }

    private fun dateColor(): Int =
        MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** A [TodaySection] name. Absent or unknown shows both feeds. */
        const val ARG_SECTION = "section"
    }
}
