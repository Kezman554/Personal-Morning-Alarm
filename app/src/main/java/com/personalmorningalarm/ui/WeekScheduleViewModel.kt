package com.personalmorningalarm.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalmorningalarm.data.model.WeekScheduleDto
import com.personalmorningalarm.data.remote.AlfredRepository
import com.personalmorningalarm.data.remote.AlfredResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the week schedule screen: one fetch of the whole plan week per screen
 * visit (the ViewModel's lifetime), cached and falling back like every other
 * Alfred feed. Page swipes are pure UI — they never touch the network.
 */
class WeekScheduleViewModel(private val alfred: AlfredRepository) : ViewModel() {

    /** Null week means "not fetched yet", shown as loading rather than failure. */
    data class WeekState(
        val loading: Boolean = true,
        val week: AlfredResult<WeekScheduleDto>? = null
    )

    private val _state = MutableStateFlow(WeekState())
    val state: StateFlow<WeekState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Never throws — unreachable comes back as Stale or Unavailable.
            val result = alfred.getWeekSchedule()
            _state.value = WeekState(loading = false, week = result)
            Log.d(TAG, "Week schedule fetched: ${result.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "PMA"
    }
}
