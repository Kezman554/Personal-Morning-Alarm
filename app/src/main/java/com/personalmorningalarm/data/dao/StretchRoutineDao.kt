package com.personalmorningalarm.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personalmorningalarm.data.entity.StretchRoutine
import kotlinx.coroutines.flow.Flow

/** A routine plus its exercise count, for the management list. */
data class RoutineWithCount(
    @Embedded val routine: StretchRoutine,
    val exerciseCount: Int
)

@Dao
interface StretchRoutineDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(routine: StretchRoutine): Long

    @Update
    suspend fun update(routine: StretchRoutine)

    @Delete
    suspend fun delete(routine: StretchRoutine)

    @Query("SELECT * FROM stretch_routines WHERE id = :id")
    suspend fun getById(id: Long): StretchRoutine?

    @Query("SELECT * FROM stretch_routines ORDER BY createdAt ASC")
    suspend fun getAll(): List<StretchRoutine>

    /** Reactive stream of all routines for the management list. */
    @Query("SELECT * FROM stretch_routines ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<StretchRoutine>>

    /** Routines with their exercise counts, for the management list. */
    @Query(
        "SELECT r.*, (SELECT COUNT(*) FROM stretch_exercises e WHERE e.routineId = r.id) " +
            "AS exerciseCount FROM stretch_routines r ORDER BY r.createdAt ASC"
    )
    fun observeAllWithCounts(): Flow<List<RoutineWithCount>>

    @Query("SELECT COUNT(*) FROM stretch_routines")
    suspend fun count(): Int

    /** The manually-selected active routine, if any. */
    @Query("SELECT * FROM stretch_routines WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): StretchRoutine?

    /** Clears the active flag on every routine (used before setting a new one). */
    @Query("UPDATE stretch_routines SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE stretch_routines SET isActive = 1 WHERE id = :id")
    suspend fun markActive(id: Long)
}
