package com.personalmorningalarm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.personalmorningalarm.R
import com.personalmorningalarm.data.model.ChalkboardTaskDto
import com.personalmorningalarm.data.model.DailySchedule
import com.personalmorningalarm.data.model.RollingTodo
import com.personalmorningalarm.data.model.RollingTodoItem
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
 * Outside the alarm entirely — no NFC gate, no dismiss timer. The schedule is a
 * glance surface; the rolling to-do is read-write: tap ticks, long-press (with a
 * confirm) drops, and the field below adds. Writes only while the list came from
 * a live fetch — on cache fallback the list stays readable and the controls go
 * quiet. The alarm-flow content screen keeps [ChalkboardRenderer] and stays
 * strictly read-only; it reminds during wake-up, it never edits.
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

    private val todoAdapter by lazy {
        RollingTodoAdapter(
            onTick = { item -> item.line?.let(viewModel::tickItem) },
            onDrop = ::confirmDrop
        )
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

        // A tile opens a single feed; BOTH is only the fallback for a missing/unknown arg.
        binding.sectionSchedule.isVisible = section != TodaySection.CHALKBOARD
        binding.sectionChalkboard.isVisible = section != TodaySection.SCHEDULE
        binding.sectionDivider.isVisible = section == TodaySection.BOTH

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        binding.rvChalkboard.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChalkboard.adapter = todoAdapter
        binding.btnChalkboardAdd.setOnClickListener { submitAdd() }
        binding.etChalkboardAdd.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitAdd()
                true
            } else {
                false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.events.collect(::showEvent) }
            }
        }
    }

    /** Adds what's in the field. Empty or whitespace-only input is ignored, not sent. */
    private fun submitAdd() {
        val text = binding.etChalkboardAdd.text?.toString().orEmpty()
        if (text.isBlank()) return
        viewModel.addItem(text)
        binding.etChalkboardAdd.setText("")
    }

    /**
     * Drop is the heavier verb — long-press got the user here, and this confirm
     * makes "no longer relevant" impossible to fat-finger.
     */
    private fun confirmDrop(item: RollingTodoItem) {
        val line = item.line ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.chalkboard_drop_title)
            .setMessage(getString(R.string.chalkboard_drop_message, item.task))
            .setPositiveButton(R.string.chalkboard_drop_confirm) { _, _ -> viewModel.dropItem(line) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showEvent(event: TodayViewModel.TodayEvent) {
        val message = when (event) {
            TodayViewModel.TodayEvent.LIST_REFRESHED -> R.string.chalkboard_list_refreshed
            TodayViewModel.TodayEvent.WRITE_FAILED -> R.string.chalkboard_write_failed
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
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
        // Writes need the live list — a cached copy's targeting lines may be days
        // old. Disabled, not hidden: the quiet state the stale note explains.
        val writable = result is AlfredResult.Fresh
        todoAdapter.writable = writable
        binding.tilChalkboardAdd.isEnabled = writable
        binding.btnChalkboardAdd.isEnabled = writable
        binding.tilChalkboardAdd.hint =
            getString(if (writable) R.string.chalkboard_add_hint else R.string.chalkboard_read_only_hint)

        val tasks = sectionData(
            result = result,
            note = binding.tvChalkboardNote,
            body = binding.tvChalkboardBody,
            staleText = R.string.chalkboard_stale,
            loadingText = R.string.chalkboard_loading,
            unavailableText = R.string.chalkboard_unavailable
        )
        if (tasks == null) {
            binding.tvChalkboardBody.isVisible = true
            binding.rvChalkboard.isVisible = false
            todoAdapter.submitList(emptyList())
            return
        }

        val items = RollingTodo.items(tasks)
        binding.rvChalkboard.isVisible = items.isNotEmpty()
        binding.tvChalkboardBody.isVisible = items.isEmpty()
        if (items.isEmpty()) {
            binding.tvChalkboardBody.text = getString(R.string.chalkboard_empty)
        }
        todoAdapter.submitList(items)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** A [TodaySection] name. Absent or unknown shows both feeds. */
        const val ARG_SECTION = "section"
    }
}
