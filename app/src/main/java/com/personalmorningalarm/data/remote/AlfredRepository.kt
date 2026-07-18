package com.personalmorningalarm.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.personalmorningalarm.data.model.ChalkboardTaskDto
import com.personalmorningalarm.data.model.ScheduleTaskDto
import com.personalmorningalarm.data.model.WeekScheduleDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.lang.reflect.Type

/**
 * Single entry point to the Alfred Vault API. Every endpoint goes through [fetch],
 * which adds the behaviour each Alfred screen needs: on success the response is
 * cached; when Alfred is unreachable the cached response is served instead, and
 * only if there's nothing cached does the caller get [AlfredResult.Unavailable].
 *
 * Alfred being down is an ordinary morning, not an error — no call here throws.
 */
class AlfredRepository(
    private val settings: AlfredSettings,
    private val cache: AlfredResponseCache,
    private val serviceProvider: (AlfredSettings) -> AlfredApiService = AlfredClient::service
) {

    constructor(context: Context) : this(
        AlfredSettings(context),
        AlfredResponseCache(context)
    )

    private val gson = Gson()

    /** Today's schedule from Alfred, falling back to the last one it served. */
    suspend fun getDailySchedule(): AlfredResult<List<ScheduleTaskDto>> =
        fetch(
            endpoint = ENDPOINT_DAILY_SCHEDULE,
            type = object : TypeToken<List<ScheduleTaskDto>>() {}.type
        ) { it.getDailySchedule() }

    /** The whole plan week, falling back to the last one Alfred served. */
    suspend fun getWeekSchedule(): AlfredResult<WeekScheduleDto> =
        fetch(
            endpoint = ENDPOINT_WEEK_SCHEDULE,
            type = WeekScheduleDto::class.java
        ) { it.getWeekSchedule() }

    /** The rolling to-do from Alfred, falling back to the last one it served. */
    suspend fun getChalkboard(): AlfredResult<List<ChalkboardTaskDto>> =
        fetch(
            endpoint = ENDPOINT_CHALKBOARD,
            type = CHALKBOARD_TYPE
        ) { it.getChalkboard() }

    /** Adds an item to the rolling to-do. Alfred appends the checkbox and today's date. */
    suspend fun addChalkboardItem(task: String): AlfredWriteResult =
        write("add") { it.addChalkboardItem(ChalkboardAddRequest(task)) }

    /** Ticks the item whose raw vault line is [line]. It stays listed until the sweep. */
    suspend fun tickChalkboardItem(line: String): AlfredWriteResult =
        write("tick") { it.tickChalkboardItem(ChalkboardLineRequest(line)) }

    /** Drops the item whose raw vault line is [line] — no longer relevant, not done. */
    suspend fun dropChalkboardItem(line: String): AlfredWriteResult =
        write("drop") { it.dropChalkboardItem(ChalkboardLineRequest(line)) }

    /**
     * Runs a chalkboard write. A 404 means the targeting line went stale; its body
     * carries the current list, which is cached (it is the freshest state we have)
     * and handed back so the caller can resync in the same round trip.
     */
    private suspend fun write(
        verb: String,
        request: suspend (AlfredApiService) -> Response<Unit>
    ): AlfredWriteResult = withContext(Dispatchers.IO) {
        try {
            val response = request(serviceProvider(settings))
            when {
                response.isSuccessful -> AlfredWriteResult.Done
                response.code() == 404 -> staleTarget(verb, response)
                else -> {
                    Log.w(TAG, "Chalkboard $verb rejected: HTTP ${response.code()}")
                    AlfredWriteResult.Unreachable
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Log.d(TAG, "Alfred unreachable for chalkboard $verb (${e.javaClass.simpleName})")
            AlfredWriteResult.Unreachable
        }
    }

    /** Shape of a tick/drop 404 (FastAPI wraps it): {"detail":{"error":…,"items":[…]}}. */
    private data class StaleTargetBody(val detail: Detail?) {
        data class Detail(val items: List<ChalkboardTaskDto>?)
    }

    private fun staleTarget(verb: String, response: Response<Unit>): AlfredWriteResult {
        val current: List<ChalkboardTaskDto>? = try {
            gson.fromJson(response.errorBody()?.string(), StaleTargetBody::class.java)
                ?.detail?.items
        } catch (e: Exception) {
            null
        }
        // A 404 whose body isn't the promised list is just a failed write.
        if (current == null) {
            Log.w(TAG, "Chalkboard $verb 404 without a usable list body")
            return AlfredWriteResult.Unreachable
        }
        Log.d(TAG, "Chalkboard $verb hit a stale line — resynced ${current.size} items")
        cache.put(ENDPOINT_CHALKBOARD, gson.toJson(current))
        return AlfredWriteResult.StaleTarget(current)
    }

    /**
     * Calls [request], caching its response under [endpoint]. [type] is how the
     * cached JSON is read back, so it must match [request]'s return type.
     */
    private suspend fun <T : Any> fetch(
        endpoint: String,
        type: Type,
        request: suspend (AlfredApiService) -> T
    ): AlfredResult<T> = withContext(Dispatchers.IO) {
        try {
            val data = request(serviceProvider(settings))
            cache.put(endpoint, gson.toJson(data))
            AlfredResult.Fresh(data)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Log.d(TAG, "Alfred unreachable for $endpoint (${e.javaClass.simpleName}) — trying cache")
            readCache(endpoint, type)
        }
    }

    private fun <T : Any> readCache(endpoint: String, type: Type): AlfredResult<T> {
        val cached = cache.get(endpoint) ?: return AlfredResult.Unavailable
        return try {
            val data = gson.fromJson<T>(cached.json, type)
                ?: return AlfredResult.Unavailable
            Log.d(TAG, "Serving cached $endpoint from ${cached.cachedAtMillis}")
            AlfredResult.Stale(data, cached.cachedAtMillis)
        } catch (e: Exception) {
            // A cache we can't parse is worse than none — drop it so it can't
            // poison every future morning.
            Log.w(TAG, "Cached $endpoint unreadable — discarding", e)
            cache.clear(endpoint)
            AlfredResult.Unavailable
        }
    }

    companion object {
        private const val TAG = "PMA"

        const val ENDPOINT_DAILY_SCHEDULE = "daily-schedule"
        const val ENDPOINT_WEEK_SCHEDULE = "daily-schedule/week"
        const val ENDPOINT_CHALKBOARD = "chalkboard"

        private val CHALKBOARD_TYPE: Type =
            object : TypeToken<List<ChalkboardTaskDto>>() {}.type
    }
}
