package com.personalmorningalarm.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named stretch routine (an ordered set of [StretchExercise]s) shown on the
 * Stage 2 stretch content screen. One routine is the manually-selected active
 * one ([isActive]); alternatively the active routine can be derived from the
 * morning goal (see AlarmConfig.matchRoutineToGoal).
 */
@Entity(tableName = "stretch_routines")
data class StretchRoutine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    /** The manually-selected active routine. At most one row should be true. */
    val isActive: Boolean = false,

    /** Epoch millis the routine was created. */
    val createdAt: Long = System.currentTimeMillis()
)
