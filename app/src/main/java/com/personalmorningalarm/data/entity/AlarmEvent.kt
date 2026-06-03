package com.personalmorningalarm.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.personalmorningalarm.data.model.MorningGoal

/**
 * One morning's wake-up outcome. Drives streak and success-rate stats.
 *
 * A "successful" day for streak purposes is [stage2Success] == true.
 */
@Entity(
    tableName = "alarm_events",
    indices = [Index(value = ["date"])]
)
data class AlarmEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Calendar day of the wake-up, ISO format "yyyy-MM-dd". */
    val date: String,

    val stage1Success: Boolean = false,

    val stage2Success: Boolean = false,

    /** Seconds taken to complete Stage 2 (0 if not completed). */
    val stage2TimeSeconds: Int = 0,

    val nuclearTriggered: Boolean = false,

    val morningGoal: MorningGoal = MorningGoal.EXERCISE,

    /** Epoch millis the event was recorded. */
    val timestamp: Long = System.currentTimeMillis()
)
