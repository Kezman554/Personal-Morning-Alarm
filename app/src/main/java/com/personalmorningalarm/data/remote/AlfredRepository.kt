package com.personalmorningalarm.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.personalmorningalarm.data.model.ChalkboardTaskDto
import com.personalmorningalarm.data.model.ScheduleTaskDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    /** The rolling to-do from Alfred, falling back to the last one it served. */
    suspend fun getChalkboard(): AlfredResult<List<ChalkboardTaskDto>> =
        fetch(
            endpoint = ENDPOINT_CHALKBOARD,
            type = object : TypeToken<List<ChalkboardTaskDto>>() {}.type
        ) { it.getChalkboard() }

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
        const val ENDPOINT_CHALKBOARD = "chalkboard"
    }
}
