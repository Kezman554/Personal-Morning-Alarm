package com.personalmorningalarm.data.remote

import androidx.test.core.app.ApplicationProvider
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

    /** An Alfred that answers with [response], or throws if it's null (unreachable). */
    private fun repository(response: List<ScheduleTaskDto>?) = AlfredRepository(
        settings,
        cache,
        serviceProvider = {
            object : AlfredApiService {
                override suspend fun getDailySchedule(): List<ScheduleTaskDto> =
                    response ?: throw IOException("Alfred unreachable")
            }
        }
    )

    private val schedule = listOf(ScheduleTaskDto("Gym", "am"), ScheduleTaskDto("Read", null))

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

    @Test
    fun `an unparseable cache is discarded rather than poisoning later mornings`() = runBlocking {
        cache.put(AlfredRepository.ENDPOINT_DAILY_SCHEDULE, "{not json")

        assertEquals(AlfredResult.Unavailable, repository(null).getDailySchedule())
        // Dropped, so it can't be retried forever.
        assertEquals(null, cache.get(AlfredRepository.ENDPOINT_DAILY_SCHEDULE))
    }
}
