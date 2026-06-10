package com.personalmorningalarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.entity.BundledQuote
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuoteManagementViewModel(private val repository: AlarmRepository) : ViewModel() {

    /** All quotes, kept in sync with the database. */
    val quotes: StateFlow<List<BundledQuote>> = repository.observeQuotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** One-off user messages (toasts). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    fun addQuote(text: String, author: String?) {
        viewModelScope.launch {
            repository.addQuote(BundledQuote(quoteText = text.trim(), author = author.normalised()))
            _messages.emit("Quote added")
        }
    }

    fun updateQuote(quote: BundledQuote, text: String, author: String?) {
        viewModelScope.launch {
            repository.updateQuote(
                quote.copy(quoteText = text.trim(), author = author.normalised())
            )
            _messages.emit("Quote updated")
        }
    }

    /**
     * Deletes [quote] unless it's the only one left — the pool must keep at least
     * one quote so the quote content screen always has something to show. Reports
     * the outcome via [onResult] so the caller can restore a swiped-away row.
     */
    fun deleteQuote(quote: BundledQuote, onResult: (deleted: Boolean) -> Unit) {
        viewModelScope.launch {
            if (repository.getQuoteCount() <= 1) {
                _messages.emit("Keep at least one quote")
                onResult(false)
            } else {
                repository.deleteQuote(quote)
                _messages.emit("Quote deleted")
                onResult(true)
            }
        }
    }

    /** Trims and turns a blank author into null (the entity's "no author"). */
    private fun String?.normalised(): String? = this?.trim()?.ifBlank { null }
}
