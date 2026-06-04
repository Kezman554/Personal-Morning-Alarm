package com.personalmorningalarm.data

import com.personalmorningalarm.data.entity.AlarmConfig
import com.personalmorningalarm.data.entity.AlarmEvent
import com.personalmorningalarm.data.entity.BundledQuote
import com.personalmorningalarm.data.entity.ContentToggle
import com.personalmorningalarm.data.entity.NfcTag
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

    // --- Alarm config ---
    suspend fun getCurrentConfig(): AlarmConfig? = alarmConfigDao.getCurrent()
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
    suspend fun getAllContentToggles(): List<ContentToggle> = contentToggleDao.getAll()
    suspend fun getEnabledContentToggles(): List<ContentToggle> =
        contentToggleDao.getEnabledContentToggles()

    // --- Bundled quotes ---
    suspend fun addQuote(quote: BundledQuote): Long = bundledQuoteDao.insert(quote)
    suspend fun addQuotes(quotes: List<BundledQuote>) = bundledQuoteDao.insertAll(quotes)
    suspend fun getAllQuotes(): List<BundledQuote> = bundledQuoteDao.getAll()
    suspend fun getRandomQuote(): BundledQuote? = bundledQuoteDao.getRandom()
    suspend fun getQuoteCount(): Int = bundledQuoteDao.count()

    // --- Stats ---

    /**
     * Number of consecutive successful days ending today (or yesterday, so a
     * not-yet-attempted morning doesn't break a live streak). 0 if the most
     * recent success is older than yesterday.
     */
    suspend fun getCurrentStreak(today: LocalDate = LocalDate.now()): Int {
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
}
