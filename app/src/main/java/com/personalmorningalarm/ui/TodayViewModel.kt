package com.personalmorningalarm.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalmorningalarm.data.model.ChalkboardTaskDto
import com.personalmorningalarm.data.model.ScheduleTaskDto
import com.personalmorningalarm.data.remote.AlfredRepository
import com.personalmorningalarm.data.remote.AlfredResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the Today screen: both Alfred feeds, refreshed on demand.
 *
 * Unlike the Stage 2 content screens there's no alarm driving this, so the user
 * decides when it reloads.
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

    private val _state = MutableStateFlow(TodayState(loading = true))
    val state: StateFlow<TodayState> = _state.asStateFlow()

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

    /** Fresh/Stale/Unavailable, for the log — the whole point is they differ per feed. */
    private fun AlfredResult<List<*>>.label(): String = when (this) {
        is AlfredResult.Fresh -> "fresh(${data.size})"
        is AlfredResult.Stale -> "stale(${data.size})"
        AlfredResult.Unavailable -> "unavailable"
    }

    private companion object {
        const val TAG = "PMA"
    }
}
