package com.personalmorningalarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.entity.StretchExercise
import com.personalmorningalarm.data.entity.StretchRoutine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives one routine's edit screen. The routine id arrives after construction via
 * [setRoutine] (the standard ViewModelFactory only injects the repository), so the
 * routine and its exercises are exposed as flows keyed off an id [MutableStateFlow].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoutineEditViewModel(private val repository: AlarmRepository) : ViewModel() {

    private val routineId = MutableStateFlow(UNSET)

    val routine: StateFlow<StretchRoutine?> = routineId
        .flatMapLatest { id ->
            if (id == UNSET) flowOf(null)
            else repository.observeRoutines().map { list -> list.firstOrNull { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises: StateFlow<List<StretchExercise>> = routineId
        .flatMapLatest { id ->
            if (id == UNSET) flowOf(emptyList()) else repository.observeExercisesForRoutine(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    fun setRoutine(id: Long) {
        if (routineId.value == UNSET) routineId.value = id
    }

    fun rename(name: String) {
        val current = routine.value ?: return
        viewModelScope.launch { repository.updateRoutine(current.copy(name = name.trim())) }
    }

    fun addExercise(name: String, durationSeconds: Int, instructions: String) {
        val id = routineId.value
        if (id == UNSET) return
        viewModelScope.launch {
            repository.addExercise(
                StretchExercise(
                    routineId = id,
                    name = name.trim(),
                    durationSeconds = durationSeconds,
                    instructions = instructions.trim(),
                    displayOrder = exercises.value.size
                )
            )
        }
    }

    fun updateExercise(exercise: StretchExercise, name: String, durationSeconds: Int, instructions: String) {
        viewModelScope.launch {
            repository.updateExercise(
                exercise.copy(
                    name = name.trim(),
                    durationSeconds = durationSeconds,
                    instructions = instructions.trim()
                )
            )
        }
    }

    fun deleteExercise(exercise: StretchExercise) {
        viewModelScope.launch { repository.deleteExercise(exercise) }
    }

    /** Swaps the exercise at [position] with the one above it, persisting both orders. */
    fun moveUp(position: Int) = swap(position, position - 1)

    /** Swaps the exercise at [position] with the one below it, persisting both orders. */
    fun moveDown(position: Int) = swap(position, position + 1)

    private fun swap(a: Int, b: Int) {
        val list = exercises.value
        if (a !in list.indices || b !in list.indices) return
        // Swap their displayOrder values so the ordered query reflects the new order.
        val first = list[a].copy(displayOrder = list[b].displayOrder)
        val second = list[b].copy(displayOrder = list[a].displayOrder)
        viewModelScope.launch { repository.updateExercises(listOf(first, second)) }
    }

    /** Deletes the whole routine unless it's the last one. */
    fun deleteRoutine(onResult: (deleted: Boolean) -> Unit) {
        val current = routine.value ?: return
        viewModelScope.launch {
            if (repository.getRoutineCount() <= 1) {
                _messages.emit("Keep at least one routine")
                onResult(false)
            } else {
                repository.deleteRoutine(current)
                onResult(true)
            }
        }
    }

    private companion object {
        const val UNSET = -1L
    }
}
