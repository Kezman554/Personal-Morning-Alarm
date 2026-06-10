package com.personalmorningalarm.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personalmorningalarm.data.entity.StretchExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface StretchExerciseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(exercise: StretchExercise): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(exercises: List<StretchExercise>)

    @Update
    suspend fun update(exercise: StretchExercise)

    /** Persists a reordered batch (display orders updated). */
    @Update
    suspend fun updateAll(exercises: List<StretchExercise>)

    @Delete
    suspend fun delete(exercise: StretchExercise)

    @Query("SELECT * FROM stretch_exercises WHERE routineId = :routineId ORDER BY displayOrder ASC")
    suspend fun getForRoutine(routineId: Long): List<StretchExercise>

    /** Reactive stream of a routine's exercises for the edit screen. */
    @Query("SELECT * FROM stretch_exercises WHERE routineId = :routineId ORDER BY displayOrder ASC")
    fun observeForRoutine(routineId: Long): Flow<List<StretchExercise>>

    @Query("SELECT COUNT(*) FROM stretch_exercises WHERE routineId = :routineId")
    suspend fun countForRoutine(routineId: Long): Int
}
