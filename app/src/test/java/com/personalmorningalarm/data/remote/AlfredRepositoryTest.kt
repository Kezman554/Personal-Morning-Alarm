package com.personalmorningalarm.data.remote

import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.personalmorningalarm.data.model.ChalkboardTaskDto
import com.personalmorningalarm.data.model.ScheduleTaskDto
import com.personalmorningalarm.data.model.ShoppingItemDto
import com.personalmorningalarm.data.model.ShoppingListDetailDto
import com.personalmorningalarm.data.model.ShoppingListSummaryDto
import com.personalmorningalarm.data.model.WeekScheduleDto
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
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

    /** An Alfred that answers each endpoint with the value given, or throws if null. */
    private fun repository(
        response: List<ScheduleTaskDto>? = null,
        chalkboardResponse: List<ChalkboardTaskDto>? = null,
        weekResponse: WeekScheduleDto? = null,
        writeResponse: (() -> Response<Unit>)? = null,
        shoppingListsResponse: List<ShoppingListSummaryDto>? = null,
        shoppingListResponse: ShoppingListDetailDto? = null,
        createShoppingListResponse: (() -> Response<ShoppingListSummaryDto>)? = null,
        shoppingWriteResponse: (() -> Response<Unit>)? = null
    ) = AlfredRepository(
        settings,
        cache,
        serviceProvider = {
            object : AlfredApiService {
                override suspend fun getDailySchedule(): List<ScheduleTaskDto> =
                    response ?: throw IOException("Alfred unreachable")

                override suspend fun getWeekSchedule(): WeekScheduleDto =
                    weekResponse ?: throw IOException("Alfred unreachable")

                override suspend fun getChalkboard(): List<ChalkboardTaskDto> =
                    chalkboardResponse ?: throw IOException("Alfred unreachable")

                override suspend fun addChalkboardItem(body: ChalkboardAddRequest): Response<Unit> = write()

                override suspend fun tickChalkboardItem(body: ChalkboardLineRequest): Response<Unit> = write()

                override suspend fun dropChalkboardItem(body: ChalkboardLineRequest): Response<Unit> = write()

                override suspend fun getShoppingLists(): List<ShoppingListSummaryDto> =
                    shoppingListsResponse ?: throw IOException("Alfred unreachable")

                override suspend fun getShoppingList(listId: String): ShoppingListDetailDto =
                    shoppingListResponse ?: throw IOException("Alfred unreachable")

                override suspend fun createShoppingList(body: ShoppingCreateRequest): Response<ShoppingListSummaryDto> =
                    createShoppingListResponse?.invoke() ?: throw IOException("Alfred unreachable")

                override suspend fun addShoppingItem(listId: String, body: ShoppingAddRequest): Response<Unit> =
                    shoppingWrite()

                override suspend fun tickShoppingItem(listId: String, body: ShoppingLineRequest): Response<Unit> =
                    shoppingWrite()

                override suspend fun dropShoppingItem(listId: String, body: ShoppingLineRequest): Response<Unit> =
                    shoppingWrite()

                private fun write(): Response<Unit> =
                    writeResponse?.invoke() ?: throw IOException("Alfred unreachable")

                private fun shoppingWrite(): Response<Unit> =
                    shoppingWriteResponse?.invoke() ?: throw IOException("Alfred unreachable")
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

    // --- /daily-schedule/week: same fetch(), same caching and fallback ---

    private val week = WeekScheduleDto(
        week = "2026-W29",
        start = "2026-07-13",
        end = "2026-07-14",
        days = mapOf(
            "2026-07-13" to listOf(ScheduleTaskDto("Gym", "am")),
            "2026-07-14" to emptyList()
        )
    )

    @Test
    fun `week schedule returns fresh data from a reachable Alfred`() = runBlocking {
        val result = repository(weekResponse = week).getWeekSchedule()

        assertTrue(result is AlfredResult.Fresh)
        assertEquals(week, (result as AlfredResult.Fresh).data)
    }

    @Test
    fun `week schedule falls back to the last successful response`() = runBlocking {
        repository(weekResponse = week).getWeekSchedule() // populates the cache

        val result = repository().getWeekSchedule()

        assertTrue(result is AlfredResult.Stale)
        assertEquals(week, (result as AlfredResult.Stale).data)
    }

    @Test
    fun `week schedule caches independently of the today endpoint`() = runBlocking {
        repository(weekResponse = week).getWeekSchedule()

        // A cached week must not answer a today request, and vice versa.
        assertEquals(AlfredResult.Unavailable, repository().getDailySchedule())
        repository(response = schedule).getDailySchedule()
        assertEquals(week, (repository().getWeekSchedule() as AlfredResult.Stale).data)
    }

    // --- chalkboard writes: like the reads, nothing here may throw ---

    private val gson = Gson()

    private fun jsonBody(json: String): ResponseBody =
        ResponseBody.create(MediaType.parse("application/json"), json)

    /** A tick/drop 404 as the live API sends it: the list wrapped in FastAPI's detail. */
    private fun staleTargetBody(list: List<ChalkboardTaskDto>): ResponseBody =
        jsonBody("""{"detail":{"error":"item not found","items":${gson.toJson(list)}}}""")

    @Test
    fun `a landed write reports done`() = runBlocking {
        val repo = repository(writeResponse = { Response.success(Unit) })

        assertEquals(AlfredWriteResult.Done, repo.addChalkboardItem("Fix fence"))
        assertEquals(AlfredWriteResult.Done, repo.tickChalkboardItem("- [ ] Fix fence"))
        assertEquals(AlfredWriteResult.Done, repo.dropChalkboardItem("- [ ] Fix fence"))
    }

    @Test
    fun `a write against an unreachable Alfred fails quietly, not an exception`() = runBlocking {
        assertEquals(AlfredWriteResult.Unreachable, repository().tickChalkboardItem("- [ ] Fix fence"))
    }

    @Test
    fun `a stale target returns the current list and refreshes the cache in the same round trip`() =
        runBlocking {
            val current = listOf(ChalkboardTaskDto("Fix fence", null, "- [ ] Fix fence"))
            val repo = repository(writeResponse = { Response.error(404, staleTargetBody(current)) })

            val result = repo.tickChalkboardItem("- [ ] Long gone")

            assertEquals(current, (result as AlfredWriteResult.StaleTarget).current)
            // The 404 body refreshed the cache: a now-dead Alfred serves that list stale.
            val stale = repository().getChalkboard()
            assertEquals(current, (stale as AlfredResult.Stale).data)
        }

    @Test
    fun `a 404 without a usable list body is just a failed write`() = runBlocking {
        val repo = repository(writeResponse = { Response.error(404, jsonBody("{not json")) })

        assertEquals(AlfredWriteResult.Unreachable, repo.dropChalkboardItem("- [ ] Fix fence"))
    }

    @Test
    fun `an unexpected server error is a failed write, not an exception`() = runBlocking {
        val repo = repository(writeResponse = { Response.error(500, jsonBody("boom")) })

        assertEquals(AlfredWriteResult.Unreachable, repo.addChalkboardItem("Fix fence"))
    }

    @Test
    fun `an unparseable cache is discarded rather than poisoning later mornings`() = runBlocking {
        cache.put(AlfredRepository.ENDPOINT_DAILY_SCHEDULE, "{not json")

        assertEquals(AlfredResult.Unavailable, repository(null).getDailySchedule())
        // Dropped, so it can't be retried forever.
        assertEquals(null, cache.get(AlfredRepository.ENDPOINT_DAILY_SCHEDULE))
    }

    // --- /shopping discovery + per-list get: same fetch(), independent cache keys per list ---

    private val fitnessId = "6-life/shopping/fitness.md"
    private val lists = listOf(
        ShoppingListSummaryDto(fitnessId, "Fitness Equipment", 2, 2),
        ShoppingListSummaryDto("6-life/shopping/fashion.md", "Fashion", 0, 0)
    )
    private val fitnessDetail = ShoppingListDetailDto(
        list = lists[0],
        items = listOf(ShoppingItemDto("Dip bars", "- [ ] Dip bars", false))
    )

    @Test
    fun `shopping discovery returns fresh data from a reachable Alfred`() = runBlocking {
        val result = repository(shoppingListsResponse = lists).getShoppingLists()

        assertTrue(result is AlfredResult.Fresh)
        assertEquals(lists, (result as AlfredResult.Fresh).data)
    }

    @Test
    fun `shopping discovery falls back to the last successful response`() = runBlocking {
        repository(shoppingListsResponse = lists).getShoppingLists()

        val result = repository().getShoppingLists()

        assertTrue(result is AlfredResult.Stale)
        assertEquals(lists, (result as AlfredResult.Stale).data)
    }

    @Test
    fun `one shopping list's cache is independent of another's and of discovery`() = runBlocking {
        repository(shoppingListResponse = fitnessDetail).getShoppingList(fitnessId)

        // Discovery and a different list id must not see the fitness cache.
        assertEquals(AlfredResult.Unavailable, repository().getShoppingLists())
        assertEquals(AlfredResult.Unavailable, repository().getShoppingList("6-life/shopping/fashion.md"))

        val stale = repository().getShoppingList(fitnessId)
        assertEquals(fitnessDetail, (stale as AlfredResult.Stale).data)
    }

    // --- create-list: online-only, its own result type ---

    @Test
    fun `creating a list reports the created summary`() = runBlocking {
        val repo = repository(createShoppingListResponse = { Response.success(lists[0]) })

        val result = repo.createShoppingList("Fitness Equipment")

        assertEquals(lists[0], (result as ShoppingCreateResult.Created).list)
    }

    @Test
    fun `a 409 reports a conflict, not an exception`() = runBlocking {
        val repo = repository(
            createShoppingListResponse = { Response.error(409, jsonBody("""{"detail":"already exists"}""")) }
        )

        assertEquals(ShoppingCreateResult.Conflict, repo.createShoppingList("Fitness Equipment"))
    }

    @Test
    fun `create against an unreachable Alfred fails quietly, not an exception`() = runBlocking {
        assertEquals(ShoppingCreateResult.Unreachable, repository().createShoppingList("New List"))
    }

    // --- shopping writes: same shape as chalkboard's, scoped per list ---

    private fun shoppingStaleTargetBody(list: List<ShoppingItemDto>): ResponseBody =
        jsonBody("""{"detail":{"error":"item not found","list_id":"$fitnessId","items":${gson.toJson(list)}}}""")

    @Test
    fun `a landed shopping write reports done`() = runBlocking {
        val repo = repository(shoppingWriteResponse = { Response.success(Unit) })

        assertEquals(ShoppingWriteResult.Done, repo.addShoppingItem(fitnessId, "Dip bars"))
        assertEquals(ShoppingWriteResult.Done, repo.tickShoppingItem(fitnessId, "- [ ] Dip bars"))
        assertEquals(ShoppingWriteResult.Done, repo.dropShoppingItem(fitnessId, "- [ ] Dip bars"))
    }

    @Test
    fun `a shopping write against an unreachable Alfred fails quietly`() = runBlocking {
        assertEquals(ShoppingWriteResult.Unreachable, repository().tickShoppingItem(fitnessId, "- [ ] Dip bars"))
    }

    @Test
    fun `a stale shopping target returns the current items and refreshes that list's cache`() = runBlocking {
        val current = listOf(ShoppingItemDto("Dip bars", "- [ ] Dip bars", false))
        val repo = repository(shoppingWriteResponse = { Response.error(404, shoppingStaleTargetBody(current)) })

        val result = repo.tickShoppingItem(fitnessId, "- [ ] Long gone")

        assertEquals(current, (result as ShoppingWriteResult.StaleTarget).current)
        val stale = repository().getShoppingList(fitnessId)
        assertEquals(current, (stale as AlfredResult.Stale).data.items)
    }

    @Test
    fun `a stale shopping target preserves the previously cached list summary`() = runBlocking {
        // Populate the cache with a full detail (list + items) first…
        repository(shoppingListResponse = fitnessDetail).getShoppingList(fitnessId)
        val current = listOf(ShoppingItemDto("Dip bars", "- [ ] Dip bars", true))
        val repo = repository(shoppingWriteResponse = { Response.error(404, shoppingStaleTargetBody(current)) })

        repo.tickShoppingItem(fitnessId, "- [ ] Long gone")

        // …the 404 body carries only items, so the cache write must carry the
        // previous summary forward rather than dropping it.
        val stale = repository().getShoppingList(fitnessId) as AlfredResult.Stale
        assertEquals(lists[0], stale.data.list)
        assertEquals(current, stale.data.items)
    }

    @Test
    fun `a shopping 404 without a usable items body is just a failed write`() = runBlocking {
        val repo = repository(shoppingWriteResponse = { Response.error(404, jsonBody("{not json")) })

        assertEquals(ShoppingWriteResult.Unreachable, repo.dropShoppingItem(fitnessId, "- [ ] Dip bars"))
    }
}
