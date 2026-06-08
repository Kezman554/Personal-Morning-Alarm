package com.personalmorningalarm.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.personalmorningalarm.data.model.MorningGoal

/**
 * User-configured alarm settings. A single active row is expected, but the table
 * supports history so changes can be tracked over time.
 */
@Entity(tableName = "alarm_config")
data class AlarmConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Scheduled alarm time as minutes since midnight (e.g. 06:30 -> 390). */
    val alarmTime: Int,

    val isEnabled: Boolean = true,

    /** Stage 2 countdown length, in minutes (PRD allows 5-15). */
    val stage2DurationMinutes: Int = 10,

    val morningGoal: MorningGoal = MorningGoal.EXERCISE,

    /**
     * Number of NFC checkpoints required to clear Stage 2 each morning. If this
     * exceeds the number of registered tags, tags repeat (never twice in a row);
     * otherwise N unique tags are picked. Default 5.
     */
    val sequenceLength: Int = 5,

    /** Epoch millis the config was created. */
    val createdAt: Long = System.currentTimeMillis()
)
