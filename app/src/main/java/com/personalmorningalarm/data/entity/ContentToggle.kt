package com.personalmorningalarm.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.personalmorningalarm.data.model.ContentType

/**
 * Enable/disable state and ordering for a Stage 2 content screen type.
 * One row per [ContentType].
 */
@Entity(
    tableName = "content_toggles",
    indices = [Index(value = ["contentType"], unique = true)]
)
data class ContentToggle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val contentType: ContentType,

    val isEnabled: Boolean = true,

    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0,

    /** Duration in minutes; only meaningful for the STRETCH timer (default 5). */
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int = 5
)
