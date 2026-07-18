package com.personalmorningalarm.data.remote

import com.personalmorningalarm.data.model.ChalkboardTaskDto
import com.personalmorningalarm.data.model.ScheduleTaskDto
import com.personalmorningalarm.data.model.WeekScheduleDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * The Alfred Vault API. One endpoint per content screen that reads from Alfred;
 * add new endpoints here rather than building a second client.
 *
 * The chalkboard writes return [Response] rather than a body so a 404 — a stale
 * targeting line — reaches [AlfredRepository] as data instead of an exception:
 * its error body carries the current list, which is how the app resyncs.
 */
interface AlfredApiService {

    /** Today's schedule. No completion state and no timestamps — display only. */
    @GET("daily-schedule")
    suspend fun getDailySchedule(): List<ScheduleTaskDto>

    /**
     * The whole plan week, every day keyed by ISO date. Used only by the week
     * screen — the alarm content screen and the Today screen stay on
     * [getDailySchedule], which is all a wake-up reminder needs.
     */
    @GET("daily-schedule/week")
    suspend fun getWeekSchedule(): WeekScheduleDto

    /**
     * The rolling to-do, including items ticked but not yet swept by the Pi's
     * overnight housekeeping. Each item carries its raw vault [ChalkboardTaskDto.line],
     * the targeting key for the writes below.
     */
    @GET("chalkboard")
    suspend fun getChalkboard(): List<ChalkboardTaskDto>

    /** Adds an item. The API appends "- [ ] text (today's date)" itself — no date is sent. */
    @POST("chalkboard")
    suspend fun addChalkboardItem(@Body body: ChalkboardAddRequest): Response<Unit>

    /** Flips the targeted item to done. It stays in the list, ticked, until the sweep. */
    @POST("chalkboard/tick")
    suspend fun tickChalkboardItem(@Body body: ChalkboardLineRequest): Response<Unit>

    /** Removes the targeted item as no longer relevant (the API archives it as dropped). */
    @POST("chalkboard/drop")
    suspend fun dropChalkboardItem(@Body body: ChalkboardLineRequest): Response<Unit>
}

/** Body for POST /chalkboard — the task text alone; Alfred adds the checkbox and date. */
data class ChalkboardAddRequest(val text: String)

/** Body for tick/drop — the item's raw vault line, sent back verbatim as the target. */
data class ChalkboardLineRequest(val line: String)
