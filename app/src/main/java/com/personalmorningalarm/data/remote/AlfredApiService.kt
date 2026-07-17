package com.personalmorningalarm.data.remote

import com.personalmorningalarm.data.model.ChalkboardTaskDto
import com.personalmorningalarm.data.model.ScheduleTaskDto
import retrofit2.http.GET

/**
 * The Alfred Vault API. One endpoint per content screen that reads from Alfred;
 * add new endpoints here rather than building a second client.
 */
interface AlfredApiService {

    /** Today's schedule. No completion state and no timestamps — display only. */
    @GET("daily-schedule")
    suspend fun getDailySchedule(): List<ScheduleTaskDto>

    /**
     * The rolling to-do. Alfred returns only the unchecked items, already
     * filtered — display only, the app never ticks anything off.
     */
    @GET("chalkboard")
    suspend fun getChalkboard(): List<ChalkboardTaskDto>
}
