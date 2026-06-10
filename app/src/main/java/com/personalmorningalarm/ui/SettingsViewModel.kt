package com.personalmorningalarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.entity.AlarmConfig
import com.personalmorningalarm.data.entity.ContentToggle
import com.personalmorningalarm.data.model.ContentType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: AlarmRepository) : ViewModel() {

    /** Content-screen toggles for the settings switches. */
    val contentToggles: StateFlow<List<ContentToggle>> = repository.observeContentToggles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** How many NFC checkpoints Stage 2 requires (the saved config value). */
    val sequenceLength: StateFlow<Int> = repository.observeCurrentConfig()
        .map { it?.sequenceLength ?: DEFAULT_SEQUENCE_LENGTH }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_SEQUENCE_LENGTH)

    /** Number of registered, active tags — caps the picker's range. */
    val activeTagCount: StateFlow<Int> = repository.observeActiveTagCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Stage 2 countdown length in minutes (the saved config value). */
    val stage2Duration: StateFlow<Int> = repository.observeCurrentConfig()
        .map { it?.stage2DurationMinutes ?: DEFAULT_STAGE2_DURATION }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_STAGE2_DURATION)

    fun setSequenceLength(length: Int) {
        viewModelScope.launch {
            val current = repository.getCurrentConfig()
            if (current == null) {
                repository.saveConfig(
                    AlarmConfig(
                        alarmTime = HomeViewModel.DEFAULT_ALARM_MINUTES,
                        isEnabled = false,
                        sequenceLength = length
                    )
                )
            } else {
                repository.updateConfig(current.copy(sequenceLength = length))
            }
        }
    }

    fun setStage2Duration(minutes: Int) {
        viewModelScope.launch {
            val current = repository.getCurrentConfig()
            if (current == null) {
                repository.saveConfig(
                    AlarmConfig(
                        alarmTime = HomeViewModel.DEFAULT_ALARM_MINUTES,
                        isEnabled = false,
                        stage2DurationMinutes = minutes
                    )
                )
            } else {
                repository.updateConfig(current.copy(stage2DurationMinutes = minutes))
            }
        }
    }

    fun setContentEnabled(type: ContentType, enabled: Boolean) {
        viewModelScope.launch {
            repository.getContentToggle(type)?.let {
                repository.updateContentToggle(it.copy(isEnabled = enabled))
            }
        }
    }

    fun setStretchDuration(minutes: Int) {
        viewModelScope.launch {
            repository.getContentToggle(ContentType.STRETCH)?.let {
                repository.updateContentToggle(it.copy(durationMinutes = minutes))
            }
        }
    }

    companion object {
        private const val DEFAULT_SEQUENCE_LENGTH = 5
        private const val DEFAULT_STAGE2_DURATION = 10
    }
}
