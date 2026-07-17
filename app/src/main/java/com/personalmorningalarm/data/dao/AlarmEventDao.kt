package com.personalmorningalarm.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personalmorningalarm.data.entity.AlarmEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: AlarmEvent): Long

    @Update
    suspend fun update(event: AlarmEvent)

    @Delete
    suspend fun delete(event: AlarmEvent)

    @Query("SELECT * FROM alarm_events ORDER BY date DESC, timestamp DESC")
    suspend fun getAll(): List<AlarmEvent>

    @Query("SELECT * FROM alarm_events WHERE date = :date ORDER BY timestamp DESC LIMIT 1")
    suspend fun getByDate(date: String): AlarmEvent?

    /** Reactive stream of all events; used to recompute home-screen stats. */
    @Query("SELECT * FROM alarm_events ORDER BY date DESC, timestamp DESC")
    fun observeAll(): Flow<List<AlarmEvent>>

    /** Distinct calendar days with any event on/after [cutoffDate]. */
    @Query("SELECT COUNT(DISTINCT date) FROM alarm_events WHERE date >= :cutoffDate")
    suspend fun countAttemptedDaysSince(cutoffDate: String): Int

    /** Distinct successful days (Stage 2 completed) on/after [cutoffDate]. */
    @Query("SELECT COUNT(DISTINCT date) FROM alarm_events WHERE stage2Success = 1 AND date >= :cutoffDate")
    suspend fun countSuccessDaysSince(cutoffDate: String): Int

    /**
     * Distinct successful days (Stage 2 completed), newest first. Used by the
     * repository to compute the current streak in Kotlin.
     */
    @Query("SELECT DISTINCT date FROM alarm_events WHERE stage2Success = 1 ORDER BY date DESC")
    suspend fun getSuccessDatesDesc(): List<String>
}
