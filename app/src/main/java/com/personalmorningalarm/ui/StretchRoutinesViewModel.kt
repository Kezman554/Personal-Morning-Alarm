package com.personalmorningalarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.dao.RoutineWithCount
import com.personalmorningalarm.data.entity.AlarmConfig
import com.personalmorningalarm.data.entity.StretchRoutine
import com.personalmorningalarm.data.model.MorningGoal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StretchRoutinesViewModel(private val repository: AlarmRepository) : ViewModel() {

    val routines: StateFlow<List<RoutineWithCount>> = repository.observeRoutinesWithCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val config: StateFlow<AlarmConfig?> = repository.observeCurrentConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    fun setActive(id: Long) {
        viewModelScope.launch { repository.setActiveRoutine(id) }
    }

    fun setMatchToGoal(enabled: Boolean) = persistConfig { it.copy(matchRoutineToGoal = enabled) }

    fun setGoalRoutine(goal: MorningGoal, routineId: Long) = persistConfig {
        when (goal) {
            MorningGoal.EXERCISE -> it.copy(exerciseRoutineId = routineId)
            MorningGoal.PROJECT -> it.copy(projectRoutineId = routineId)
        }
    }

    /** Creates an empty routine and returns its id so the caller can open the editor. */
    fun createRoutine(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.addRoutine(StretchRoutine(name = name))
            onCreated(id)
        }
    }

    /** Deletes [routine] unless it's the last one (must keep at least one routine). */
    fun deleteRoutine(routine: StretchRoutine, onResult: (deleted: Boolean) -> Unit) {
        viewModelScope.launch {
            if (repository.getRoutineCount() <= 1) {
                _messages.emit("Keep at least one routine")
                onResult(false)
            } else {
                repository.deleteRoutine(routine)
                onResult(true)
            }
        }
    }

    private fun persistConfig(transform: (AlarmConfig) -> AlarmConfig) {
        viewModelScope.launch {
            val current = repository.getCurrentConfig()
            if (current == null) {
                repository.saveConfig(
                    transform(AlarmConfig(alarmTime = HomeViewModel.DEFAULT_ALARM_MINUTES, isEnabled = false))
                )
            } else {
                repository.updateConfig(transform(current))
            }
        }
    }
}
