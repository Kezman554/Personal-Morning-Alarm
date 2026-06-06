package com.personalmorningalarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.entity.ContentToggle
import com.personalmorningalarm.data.model.ContentType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: AlarmRepository) : ViewModel() {

    /** Content-screen toggles for the settings switches. */
    val contentToggles: StateFlow<List<ContentToggle>> = repository.observeContentToggles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
}
