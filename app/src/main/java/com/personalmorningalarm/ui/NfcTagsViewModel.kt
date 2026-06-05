package com.personalmorningalarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.entity.NfcTag
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NfcTagsViewModel(private val repository: AlarmRepository) : ViewModel() {

    /** All registered tags, kept in sync with the database. */
    val tags: StateFlow<List<NfcTag>> = repository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Count of active tags. */
    val activeTagCount: StateFlow<Int> = repository.observeActiveTagCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** One-off user messages (toasts). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    fun registerTag(tagId: String, label: String, location: String) {
        viewModelScope.launch {
            val existing = repository.getTagByHardwareId(tagId)
            if (existing != null) {
                _messages.emit("Tag already registered as \"${existing.label}\"")
                return@launch
            }
            val order = repository.getAllTags().size
            repository.registerTag(
                NfcTag(tagId = tagId, label = label, location = location, order = order)
            )
            _messages.emit("Saved \"$label\"")
        }
    }

    fun deleteTag(tag: NfcTag) {
        viewModelScope.launch {
            repository.deleteTag(tag)
            _messages.emit("Deleted \"${tag.label}\"")
        }
    }
}
