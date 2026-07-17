package com.personalmorningalarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.personalmorningalarm.data.remote.AlfredRepository

/**
 * Constructs ViewModels that take an [AlfredRepository] as their sole constructor
 * argument, e.g. `class TodayViewModel(alfred: AlfredRepository) : ViewModel()`.
 * The sibling of [ViewModelFactory], which does the same for AlarmRepository.
 */
class AlfredViewModelFactory(private val repository: AlfredRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return try {
            modelClass.getConstructor(AlfredRepository::class.java).newInstance(repository) as T
        } catch (e: NoSuchMethodException) {
            throw IllegalArgumentException(
                "${modelClass.name} must have a constructor taking AlfredRepository", e
            )
        }
    }
}
