package com.personalmorningalarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.personalmorningalarm.data.remote.AlfredRepository
import com.personalmorningalarm.data.remote.ChalkboardSync

/**
 * Constructs the Alfred-backed ViewModels: `(AlfredRepository, ChalkboardSync)`
 * when the ViewModel writes (and so needs the offline queue), else the plain
 * `(AlfredRepository)` shape. The sibling of [ViewModelFactory], which does the
 * same for AlarmRepository.
 */
class AlfredViewModelFactory(
    private val repository: AlfredRepository,
    private val sync: ChalkboardSync? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (sync != null) {
            try {
                return modelClass
                    .getConstructor(AlfredRepository::class.java, ChalkboardSync::class.java)
                    .newInstance(repository, sync) as T
            } catch (e: NoSuchMethodException) {
                // Fall through to the single-argument shape.
            }
        }
        return try {
            modelClass.getConstructor(AlfredRepository::class.java).newInstance(repository) as T
        } catch (e: NoSuchMethodException) {
            throw IllegalArgumentException(
                "${modelClass.name} must have a constructor taking AlfredRepository", e
            )
        }
    }
}
