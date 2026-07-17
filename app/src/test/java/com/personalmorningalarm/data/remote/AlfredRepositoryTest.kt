package com.personalmorningalarm.data.remote

import androidx.test.core.app.ApplicationProvider
import com.personalmorningalarm.data.model.ChalkboardTaskDto
import com.personalmorningalarm.data.model.ScheduleTaskDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * The cache-and-fallback behaviour every Alfred screen leans on: Alfred being
 * unreachable is a normal morning, so no call here may throw.
 */
@RunWith(RobolectricTestRunner::class)
class AlfredRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val settings = AlfredSettings(context)
    private val cache = AlfredResponseCache(context)

    /** An Alfred that answers each endpoint with the list given, or throws if null. */
    private fun repository(
        response: List<ScheduleTaskDto>? = null,
        chalkboardResponse: List<ChalkboardTaskDto>? = null
    ) = AlfredRepository(
        settings,
        cache,
        serviceProvider = {
            object : AlfredApiService {
                override suspend fun getDailySchedule(): List<ScheduleTaskDto> =
                    response ?: throw IOException("Alfred unreachable")

                override suspend fun getChalkboard(): List<ChalkboardTaskDto> =
                    chalkboardResponse ?: throw IOException("Alfred unreachable")
            }
        }
    )

    private val schedule = listOf(ScheduleTaskDto("Gym", "am"), ScheduleTaskDto("Read", null))
    private val chalkboard =
        listOf(ChalkboardTaskDto("Plant the bamboo", "2026-07-05"), ChalkboardTaskDto("Fix fence", null))

    @Test
    fun `a reachable Alfred returns fresh data`() = runBlocking {
        val result = repository(schedule).getDailySchedule()

        assertTrue(result is AlfredResult.Fresh)
        assertEquals(schedule, (result as AlfredResult.Fresh).data)
    }

    @Test
    fun `an unreachable Alfred with no cache is unavailable, not an exception`() = runBlocking {
        val result = repository(null).getDailySchedule()

        assertEquals(AlfredResult.Unavailable, result)
    }

    @Test
    fun `an unreachable Alfred serves the last successful response as stale`() = runBlocking {
        repository(schedule).getDailySchedule() // populates the cache

        val result = repository(null).getDailySchedule()

        assertTrue(result is AlfredResult.Stale)
        assertEquals(schedule, (result as AlfredResult.Stale).data)
        assertTrue(result.cachedAtMillis > 0)
    }

    @Test
    fun `a later success replaces the cached response`() = runBlocking {
        repository(schedule).getDailySchedule()
        val newer = listOf(ScheduleTaskDto("Dentist", "pm"))
        repository(newer).getDailySchedule()

        val result = repository(null).getDailySchedule()

        assertEquals(newer, (result as AlfredResult.Stale).data)
    }

    // --- /chalkboard: same fetch(), so it inherits caching and fallback unchanged ---

    @Test
    fun `chalkboard returns fresh data from a reachable Alfred`() = runBlocking {
        val result = repository(chalkboardResponse = chalkboard).getChalkboard()

        assertTrue(result is AlfredResult.Fresh)
        assertEquals(chalkboard, (result as AlfredResult.Fresh).data)
    }

    @Test
    fun `chalkboard with no cache is unavailable, not an exception`() = runBlocking {
        assertEquals(AlfredResult.Unavailable, repository().getChalkboard())
    }

    @Test
    fun `chalkboard falls back to the last successful response, nulls intact`() = runBlocking {
        repository(chalkboardResponse = chalkboard).getChalkboard() // populates the cache

        val result = repository().getChalkboard()

        assertTrue(result is AlfredResult.Stale)
        assertEquals(chalkboard, (result as AlfredResult.Stale).data)
        // The absent date must survive the cache round-trip as null, not "null".
        assertEquals(null, result.data[1].date)
    }

    @Test
    fun `the two endpoints cache independently`() = runBlocking {
        // Only the schedule has ever succeeded.
        repository(response = schedule).getDailySchedule()

        // A schedule in the cache must not answer a chalkboard request.
        assertEquals(AlfredResult.Unavailable, repository().getChalkboard())

        // And caching the chalkboard must not disturb the schedule's entry.
        repository(chalkboardResponse = chalkboard).getChalkboard()
        val stale = repository().getDailySchedule()
        assertEquals(schedule, (stale as AlfredResult.Stale).data)
    }

    @Test
    fun `an unparseable cache is discarded rather than poisoning later mornings`() = runBlocking {
        cache.put(AlfredRepository.ENDPOINT_DAILY_SCHEDULE, "{not json")

        assertEquals(AlfredResult.Unavailable, repository(null).getDailySchedule())
        // Dropped, so it can't be retried forever.
        assertEquals(null, cache.get(AlfredRepository.ENDPOINT_DAILY_SCHEDULE))
    }
}
