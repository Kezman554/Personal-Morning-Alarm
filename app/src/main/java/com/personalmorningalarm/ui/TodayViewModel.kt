package com.personalmorningalarm.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalmorningalarm.data.model.ChalkboardTaskDto
import com.personalmorningalarm.data.model.ScheduleTaskDto
import com.personalmorningalarm.data.remote.AlfredRepository
import com.personalmorningalarm.data.remote.AlfredResult
import com.personalmorningalarm.data.remote.AlfredWriteResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the Today screen: both Alfred feeds, refreshed on demand, and the
 * chalkboard's three writes — add, tick, drop.
 *
 * Writes are optimistic: the list changes on screen immediately, then a refetch
 * reconciles it with what Alfred actually holds. They're only offered while the
 * chalkboard is [AlfredResult.Fresh] — a cached copy can't be written against,
 * so away from home the screen quietly falls back to read-only.
 */
class TodayViewModel(private val alfred: AlfredRepository) : ViewModel() {

    /**
     * Each feed is held separately and rendered separately: one endpoint being
     * unreachable must not blank the other. Null means "not fetched yet", which the
     * screen shows as loading rather than as a failure.
     */
    data class TodayState(
        val loading: Boolean = false,
        val schedule: AlfredResult<List<ScheduleTaskDto>>? = null,
        val chalkboard: AlfredResult<List<ChalkboardTaskDto>>? = null
    )

    /** One-shot outcomes of a write that the user should hear about. */
    enum class TodayEvent {
        /** A tick/drop hit a stale line; the list on screen was replaced with Alfred's. */
        LIST_REFRESHED,
        /** Alfred didn't take the write; the list was refetched to undo the optimism. */
        WRITE_FAILED
    }

    private val _state = MutableStateFlow(TodayState(loading = true))
    val state: StateFlow<TodayState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TodayEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<TodayEvent> = _events.asSharedFlow()

    private var inFlight: Job? = null

    init {
        refresh()
    }

    /** Ignored while a refresh is already running, so repeated pulls don't stack. */
    fun refresh() {
        if (inFlight?.isActive == true) {
            Log.d(TAG, "Today: refresh already in flight — ignored")
            return
        }
        inFlight = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            coroutineScope {
                // Both feeds at once — measured on device, they do start together
                // (1ms apart, different threads). Note that offline this still takes
                // ~6s rather than ~3s: OkHttp doesn't give each attempt on the same
                // unreachable host its own timeout window, so the second waits out the
                // first. Tolerable here — the user asked for this screen and the
                // spinner shows the wait — and it can't touch the alarm, where each
                // content screen makes a single call.
                val schedule = async { alfred.getDailySchedule() }
                val chalkboard = async { alfred.getChalkboard() }
                // Neither call throws — Alfred being unreachable comes back as a
                // result, so there's no partial-failure state to unpick here.
                val scheduleResult = schedule.await()
                val chalkboardResult = chalkboard.await()
                _state.value = TodayState(
                    loading = false,
                    schedule = scheduleResult,
                    chalkboard = chalkboardResult
                )
                Log.d(
                    TAG,
                    "Today: refreshed — schedule=${scheduleResult.label()}, " +
                        "chalkboard=${chalkboardResult.label()}"
                )
            }
        }
    }

    /** Appends [text] to the rolling to-do. Blank input is the caller's to reject; re-checked here. */
    fun addItem(text: String) {
        // The vault is line-oriented — an embedded newline would split into two items.
        val task = text.replace(NEWLINES, " ").trim()
        if (task.isEmpty()) return
        // No line yet — Alfred composes the real one; the refetch brings it back.
        applyOptimistically { it + ChalkboardTaskDto(task = task, date = null, line = null) }
        runWrite("add") { alfred.addChalkboardItem(task) }
    }

    /** Ticks the item targeted by [line] — done, greyed out, swept overnight. */
    fun tickItem(line: String) {
        applyOptimistically { list ->
            list.map { if (it.line == line) it.copy(line = tickedLine(line)) else it }
        }
        // No reconcile on success: Alfred's listings exclude ticked items straight
        // away, so a refetch would hide the greyed row the user should still see.
        // The flipped local copy is exactly what the vault now holds; the next
        // refresh (or the overnight sweep) is what makes the item leave.
        runWrite("tick", reconcileOnSuccess = false) { alfred.tickChalkboardItem(line) }
    }

    /** Drops the item targeted by [line] — no longer relevant, removed outright. */
    fun dropItem(line: String) {
        applyOptimistically { list -> list.filterNot { it.line == line } }
        runWrite("drop") { alfred.dropChalkboardItem(line) }
    }

    /**
     * Shows the write's effect immediately, while the chalkboard is Fresh — the
     * only state writes are offered in. The follow-up refetch is what makes the
     * optimism honest either way.
     */
    private fun applyOptimistically(mutate: (List<ChalkboardTaskDto>) -> List<ChalkboardTaskDto>) {
        val fresh = _state.value.chalkboard as? AlfredResult.Fresh ?: return
        _state.value = _state.value.copy(chalkboard = AlfredResult.Fresh(mutate(fresh.data)))
    }

    private fun runWrite(
        verb: String,
        reconcileOnSuccess: Boolean = true,
        write: suspend () -> AlfredWriteResult
    ) {
        viewModelScope.launch {
            when (val result = write()) {
                AlfredWriteResult.Done -> if (reconcileOnSuccess) reconcileChalkboard()
                is AlfredWriteResult.StaleTarget -> {
                    // The current list came back in the same round trip — use it.
                    _state.value = _state.value.copy(chalkboard = AlfredResult.Fresh(result.current))
                    _events.emit(TodayEvent.LIST_REFRESHED)
                }
                AlfredWriteResult.Unreachable -> {
                    Log.d(TAG, "Today: chalkboard $verb didn't land")
                    reconcileChalkboard()
                    _events.emit(TodayEvent.WRITE_FAILED)
                }
            }
        }
    }

    /**
     * Refetches the chalkboard alone — the schedule hasn't changed, and a full
     * refresh() would flash the spinner over what should feel like an in-place edit.
     */
    private suspend fun reconcileChalkboard() {
        _state.value = _state.value.copy(chalkboard = alfred.getChalkboard())
    }

    /** "[ ]" → "[x]" for the optimistic tick; the refetch supplies Alfred's actual line. */
    private fun tickedLine(line: String): String = line.replaceFirst("[ ]", "[x]")

    /** Fresh/Stale/Unavailable, for the log — the whole point is they differ per feed. */
    private fun AlfredResult<List<*>>.label(): String = when (this) {
        is AlfredResult.Fresh -> "fresh(${data.size})"
        is AlfredResult.Stale -> "stale(${data.size})"
        AlfredResult.Unavailable -> "unavailable"
    }

    private companion object {
        const val TAG = "PMA"
        val NEWLINES = Regex("""\R+""")
    }
}
