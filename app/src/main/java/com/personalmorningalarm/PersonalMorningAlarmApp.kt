package com.personalmorningalarm

import android.app.Application
import android.util.Log
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.AppDatabase
import com.personalmorningalarm.data.QuoteSeedData
import com.personalmorningalarm.data.StretchSeedData
import com.personalmorningalarm.data.entity.ContentToggle
import com.personalmorningalarm.data.entity.StretchExercise
import com.personalmorningalarm.data.entity.StretchRoutine
import com.personalmorningalarm.data.model.ContentType
import com.personalmorningalarm.util.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Seeds bundled quotes and default content toggles on first launch (idempotent). */
class PersonalMorningAlarmApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Apply the saved theme before any activity is created.
        ThemeManager(this).applySaved()

        val repository = AlarmRepository(AppDatabase.getInstance(this))
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (repository.getQuoteCount() == 0) {
                repository.addQuotes(QuoteSeedData.quotes)
                Log.d(TAG, "Seeded ${QuoteSeedData.quotes.size} bundled quotes")
            }
            if (repository.getAllContentToggles().isEmpty()) {
                repository.saveContentToggles(
                    listOf(
                        ContentToggle(contentType = ContentType.QUOTE, isEnabled = true, displayOrder = 0),
                        ContentToggle(contentType = ContentType.STRETCH, isEnabled = true, displayOrder = 1),
                        ContentToggle(contentType = ContentType.PLACEHOLDER, isEnabled = false, displayOrder = 2)
                    )
                )
                Log.d(TAG, "Seeded default content toggles")
            }
            if (repository.getRoutineCount() == 0) {
                seedStretchRoutines(repository)
                Log.d(TAG, "Seeded ${StretchSeedData.routines.size} stretch routines")
            }
        }
    }

    /**
     * Inserts each preset routine, then its exercises with the new routine id and a
     * display order. The default routine (StretchSeedData.DEFAULT_ACTIVE_INDEX) is
     * marked active.
     */
    private suspend fun seedStretchRoutines(repository: AlarmRepository) {
        StretchSeedData.routines.forEachIndexed { index, seed ->
            val routineId = repository.addRoutine(
                StretchRoutine(
                    name = seed.name,
                    isActive = index == StretchSeedData.DEFAULT_ACTIVE_INDEX
                )
            )
            repository.addExercises(
                seed.exercises.mapIndexed { order, ex ->
                    StretchExercise(
                        routineId = routineId,
                        name = ex.name,
                        durationSeconds = ex.durationSeconds,
                        instructions = ex.instructions,
                        displayOrder = order
                    )
                }
            )
        }
    }

    companion object {
        private const val TAG = "PMA"
    }
}
