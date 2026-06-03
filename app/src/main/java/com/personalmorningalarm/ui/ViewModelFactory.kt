package com.personalmorningalarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.personalmorningalarm.data.AlarmRepository

/**
 * Constructs ViewModels that take an [AlarmRepository] as their sole constructor
 * argument, e.g. `class HomeViewModel(repo: AlarmRepository) : ViewModel()`.
 */
class ViewModelFactory(private val repository: AlarmRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return try {
            modelClass.getConstructor(AlarmRepository::class.java).newInstance(repository) as T
        } catch (e: NoSuchMethodException) {
            throw IllegalArgumentException(
                "${modelClass.name} must have a constructor taking AlarmRepository", e
            )
        }
    }
}
