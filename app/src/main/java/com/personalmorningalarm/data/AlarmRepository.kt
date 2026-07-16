package com.personalmorningalarm.data

import com.personalmorningalarm.data.dao.RoutineWithCount
import com.personalmorningalarm.data.entity.AlarmConfig
import com.personalmorningalarm.data.entity.AlarmEvent
import com.personalmorningalarm.data.entity.BundledQuote
import com.personalmorningalarm.data.entity.ContentToggle
import com.personalmorningalarm.data.entity.NfcTag
import com.personalmorningalarm.data.entity.StretchExercise
import com.personalmorningalarm.data.entity.StretchRoutine
import com.personalmorningalarm.data.model.ContentType
import com.personalmorningalarm.data.model.MorningGoal
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Single entry point to the persistence layer. Wraps all DAOs in suspend
 * functions so callers (ViewModels) never touch Room directly.
 */
class AlarmRepository(private val db: AppDatabase) {

    private val alarmConfigDao = db.alarmConfigDao()
    private val alarmEventDao = db.alarmEventDao()
    private val nfcTagDao = db.nfcTagDao()
    private val contentToggleDao = db.contentToggleDao()
    private val bundledQuoteDao = db.bundledQuoteDao()
    private val stretchRoutineDao = db.stretchRoutineDao()
    private val stretchExerciseDao = db.stretchExerciseDao()

    // --- Alarm config ---
    suspend fun getCurrentConfig(): AlarmConfig? = alarmConfigDao.getCurrent()
    fun observeCurrentConfig(): Flow<AlarmConfig?> = alarmConfigDao.observeCurrent()
    suspend fun getAllConfigs(): List<AlarmConfig> = alarmConfigDao.getAll()
    suspend fun saveConfig(config: AlarmConfig): Long = alarmConfigDao.insert(config)
    suspend fun updateConfig(config: AlarmConfig) = alarmConfigDao.update(config)
    suspend fun deleteConfig(config: AlarmConfig) = alarmConfigDao.delete(config)

    // --- Alarm events ---
    suspend fun recordEvent(event: AlarmEvent): Long = alarmEventDao.insert(event)
    suspend fun updateEvent(event: AlarmEvent) = alarmEventDao.update(event)
    suspend fun deleteEvent(event: AlarmEvent) = alarmEventDao.delete(event)
    suspend fun getAllEvents(): List<AlarmEvent> = alarmEventDao.getAll()
    suspend fun getEventByDate(date: String): AlarmEvent? = alarmEventDao.getByDate(date)
    fun observeEvents(): Flow<List<AlarmEvent>> = alarmEventDao.observeAll()

    // --- NFC tags ---
    suspend fun registerTag(tag: NfcTag): Long = nfcTagDao.insert(tag)
    suspend fun updateTag(tag: NfcTag) = nfcTagDao.update(tag)
    suspend fun deleteTag(tag: NfcTag) = nfcTagDao.delete(tag)
    suspend fun getAllTags(): List<NfcTag> = nfcTagDao.getAll()
    suspend fun getTagByHardwareId(tagId: String): NfcTag? = nfcTagDao.getByTagId(tagId)
    suspend fun getActiveNfcTags(): List<NfcTag> = nfcTagDao.getActiveNfcTags()
    fun observeTags(): Flow<List<NfcTag>> = nfcTagDao.observeAll()
    fun observeActiveTagCount(): Flow<Int> = nfcTagDao.observeActiveCount()

    // --- Content toggles ---
    suspend fun saveContentToggle(toggle: ContentToggle): Long = contentToggleDao.insert(toggle)
    suspend fun saveContentToggles(toggles: List<ContentToggle>) = contentToggleDao.insertAll(toggles)
    suspend fun updateContentToggle(toggle: ContentToggle) = contentToggleDao.update(toggle)
    // Reads pass the types this build knows, so a toggle row written by a newer
    // build is skipped rather than crashing the read. See ContentToggleDao.
    suspend fun getAllContentToggles(): List<ContentToggle> =
        contentToggleDao.getAll(ContentType.knownNames)
    suspend fun getEnabledContentToggles(): List<ContentToggle> =
        contentToggleDao.getEnabledContentToggles(ContentType.knownNames)
    suspend fun getContentToggle(type: ContentType): ContentToggle? =
        contentToggleDao.getByType(type)
    fun observeContentToggles(): Flow<List<ContentToggle>> =
        contentToggleDao.observeAll(ContentType.knownNames)

    // --- Bundled quotes ---
    suspend fun addQuote(quote: BundledQuote): Long = bundledQuoteDao.insert(quote)
    suspend fun addQuotes(quotes: List<BundledQuote>) = bundledQuoteDao.insertAll(quotes)
    suspend fun updateQuote(quote: BundledQuote) = bundledQuoteDao.update(quote)
    suspend fun deleteQuote(quote: BundledQuote) = bundledQuoteDao.delete(quote)
    suspend fun getAllQuotes(): List<BundledQuote> = bundledQuoteDao.getAll()
    fun observeQuotes(): Flow<List<BundledQuote>> = bundledQuoteDao.observeAll()
    suspend fun getRandomQuote(): BundledQuote? = bundledQuoteDao.getRandom()
    suspend fun getQuoteCount(): Int = bundledQuoteDao.count()

    // --- Stretch routines & exercises ---
    suspend fun addRoutine(routine: StretchRoutine): Long = stretchRoutineDao.insert(routine)
    suspend fun updateRoutine(routine: StretchRoutine) = stretchRoutineDao.update(routine)
    suspend fun deleteRoutine(routine: StretchRoutine) = stretchRoutineDao.delete(routine)
    suspend fun getRoutine(id: Long): StretchRoutine? = stretchRoutineDao.getById(id)
    suspend fun getAllRoutines(): List<StretchRoutine> = stretchRoutineDao.getAll()
    fun observeRoutines(): Flow<List<StretchRoutine>> = stretchRoutineDao.observeAll()
    fun observeRoutinesWithCounts(): Flow<List<RoutineWithCount>> =
        stretchRoutineDao.observeAllWithCounts()
    suspend fun getRoutineCount(): Int = stretchRoutineDao.count()

    suspend fun addExercise(exercise: StretchExercise): Long = stretchExerciseDao.insert(exercise)
    suspend fun addExercises(exercises: List<StretchExercise>) =
        stretchExerciseDao.insertAll(exercises)
    suspend fun updateExercise(exercise: StretchExercise) = stretchExerciseDao.update(exercise)
    suspend fun updateExercises(exercises: List<StretchExercise>) =
        stretchExerciseDao.updateAll(exercises)
    suspend fun deleteExercise(exercise: StretchExercise) = stretchExerciseDao.delete(exercise)
    suspend fun getExercisesForRoutine(routineId: Long): List<StretchExercise> =
        stretchExerciseDao.getForRoutine(routineId)
    fun observeExercisesForRoutine(routineId: Long): Flow<List<StretchExercise>> =
        stretchExerciseDao.observeForRoutine(routineId)

    /** Marks [id] the active routine, clearing the flag on all others first. */
    suspend fun setActiveRoutine(id: Long) {
        stretchRoutineDao.clearActive()
        stretchRoutineDao.markActive(id)
    }

    /**
     * The routine to use for Stage 2's stretch screen, given the morning [goal].
     * If the config matches routines to goals, uses the goal's mapped routine;
     * otherwise the manually marked-active routine. Falls back to the first
     * routine so the stretch screen always has something to show.
     */
    suspend fun getStretchRoutineForGoal(goal: MorningGoal): StretchRoutine? {
        val config = alarmConfigDao.getCurrent()
        if (config?.matchRoutineToGoal == true) {
            val mappedId = when (goal) {
                MorningGoal.EXERCISE -> config.exerciseRoutineId
                MorningGoal.PROJECT -> config.projectRoutineId
            }
            stretchRoutineDao.getById(mappedId)?.let { return it }
        }
        return stretchRoutineDao.getActive() ?: stretchRoutineDao.getAll().firstOrNull()
    }

    // --- Stats ---

    /**
     * Number of consecutive successful days ending today (or yesterday, so a
     * not-yet-attempted morning doesn't break a live streak). 0 if the most
     * recent success is older than yesterday.
     */
    suspend fun getCurrentStreak(today: LocalDate = LocalDate.now()): Int {
        // A nuclear failure today breaks the streak immediately — even if
        // yesterday was a success, the live streak is over.
        if (alarmEventDao.getByDate(today.toString())?.nuclearTriggered == true) return 0

        val successDays = alarmEventDao.getSuccessDatesDesc().map(LocalDate::parse)
        if (successDays.isEmpty()) return 0

        // Anchor the streak at the most recent success only if it's today/yesterday.
        var expected = when (successDays.first()) {
            today -> today
            today.minusDays(1) -> today.minusDays(1)
            else -> return 0
        }

        var streak = 0
        for (day in successDays) {
            when {
                day == expected -> {
                    streak++
                    expected = expected.minusDays(1)
                }
                day.isBefore(expected) -> break // gap found
                // day after expected => duplicate already consumed; skip
            }
        }
        return streak
    }

    /** Longest run of consecutive successful days ever recorded. */
    suspend fun getLongestStreak(): Int {
        val successDays = alarmEventDao.getSuccessDatesAsc().map(LocalDate::parse)
        if (successDays.isEmpty()) return 0

        var longest = 1
        var run = 1
        for (i in 1 until successDays.size) {
            run = if (successDays[i] == successDays[i - 1].plusDays(1)) run + 1 else 1
            if (run > longest) longest = run
        }
        return longest
    }

    /**
     * Stage 2 success rate (0.0-1.0) over the trailing 7 days, today inclusive.
     */
    suspend fun getWeeklySuccessRate(today: LocalDate = LocalDate.now()): Float {
        val cutoff = today.minusDays(6).toString()
        return alarmEventDao.getSuccessRateSince(cutoff)
    }

    /**
     * Successful days and attempted days over the trailing 7 days (today
     * inclusive), as a (successDays, attemptedDays) pair — e.g. 4 to 5 renders
     * as "4/5 days" on the home screen.
     */
    suspend fun getWeeklySuccessCounts(today: LocalDate = LocalDate.now()): Pair<Int, Int> {
        val cutoff = today.minusDays(6).toString()
        val success = alarmEventDao.countSuccessDaysSince(cutoff)
        val attempted = alarmEventDao.countAttemptedDaysSince(cutoff)
        return success to attempted
    }
}
