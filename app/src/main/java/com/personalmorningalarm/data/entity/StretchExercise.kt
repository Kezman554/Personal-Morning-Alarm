package com.personalmorningalarm.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One exercise within a [StretchRoutine]: a name, how long to hold/perform it,
 * instructions, and its position in the routine. Deleting a routine cascades to
 * its exercises.
 */
@Entity(
    tableName = "stretch_exercises",
    foreignKeys = [
        ForeignKey(
            entity = StretchRoutine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId")]
)
data class StretchExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val routineId: Long,

    val name: String,

    /** How long this stretch runs, in seconds (drives the per-stretch countdown). */
    val durationSeconds: Int,

    val instructions: String,

    /** Position within the routine (ascending). */
    val displayOrder: Int
)
