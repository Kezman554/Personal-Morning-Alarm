package com.personalmorningalarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.entity.AlarmEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Outcome of a single calendar day, for the heatmap grid. */
enum class DayOutcome { NONE, SUCCESS, FAILURE }

/** One day cell in the heatmap. */
data class DayCell(val date: LocalDate, val outcome: DayOutcome)

/** One 7-day bucket in the weekly bar chart. */
data class WeekBar(
    /** Short label for the week's start, e.g. "5 May". */
    val label: String,
    val successDays: Int,
    val attemptedDays: Int
) {
    /** Success fraction 0..1; 0 when nothing was attempted that week. */
    val rate: Float get() = if (attemptedDays == 0) 0f else successDays.toFloat() / attemptedDays
}

/** Everything the statistics screen renders, derived from the event log. */
data class StatisticsUiState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val weeks: List<WeekBar> = emptyList(),
    val days: List<DayCell> = emptyList(),
    /** Full event history, newest first. */
    val history: List<AlarmEvent> = emptyList()
)

/**
 * Statistics-screen state, recomputed whenever the event log changes. All
 * aggregation runs in [buildState] off the reactive [AlarmRepository.observeEvents]
 * stream, mirroring [HomeViewModel]'s approach.
 */
class StatisticsViewModel(private val repository: AlarmRepository) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = repository.observeEvents()
        .map { buildState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    private suspend fun buildState(events: List<AlarmEvent>): StatisticsUiState {
        val today = LocalDate.now()
        // Collapse to one representative event per day (the latest by timestamp),
        // so multiple attempts in a day count as a single calendar outcome.
        val byDate: Map<LocalDate, AlarmEvent> = events
            .groupBy { LocalDate.parse(it.date) }
            .mapValues { (_, list) -> list.maxByOrNull { it.timestamp }!! }

        return StatisticsUiState(
            currentStreak = repository.getCurrentStreak(today),
            longestStreak = repository.getLongestStreak(),
            weeks = computeWeeks(byDate, today),
            days = computeDays(byDate, today),
            history = events // already ordered date DESC, timestamp DESC by the DAO
        )
    }

    /** Four trailing 7-day windows, oldest first, so the chart reads left→right. */
    private fun computeWeeks(byDate: Map<LocalDate, AlarmEvent>, today: LocalDate): List<WeekBar> =
        (0 until WEEKS_SHOWN).map { w ->
            val end = today.minusDays(((WEEKS_SHOWN - 1 - w) * 7).toLong())
            val start = end.minusDays(6)
            val inWindow = byDate.filterKeys { !it.isBefore(start) && !it.isAfter(end) }.values
            WeekBar(
                label = start.format(WEEK_LABEL_FORMAT),
                successDays = inWindow.count { it.stage2Success },
                attemptedDays = inWindow.size
            )
        }

    /** Last [DAYS_SHOWN] days, oldest first (top-left) → today (bottom-right). */
    private fun computeDays(byDate: Map<LocalDate, AlarmEvent>, today: LocalDate): List<DayCell> =
        (0 until DAYS_SHOWN).map { offset ->
            val date = today.minusDays((DAYS_SHOWN - 1 - offset).toLong())
            val outcome = when {
                byDate[date] == null -> DayOutcome.NONE
                byDate[date]!!.stage2Success -> DayOutcome.SUCCESS
                else -> DayOutcome.FAILURE
            }
            DayCell(date, outcome)
        }

    companion object {
        const val WEEKS_SHOWN = 4
        const val DAYS_SHOWN = 28 // 4 weeks × 7, fills a 7-wide grid evenly
        private val WEEK_LABEL_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    }
}
