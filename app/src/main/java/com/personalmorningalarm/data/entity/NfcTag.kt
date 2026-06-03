package com.personalmorningalarm.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A registered NFC checkpoint tag placed around the home for Stage 2.
 *
 * [tagId] is the hardware tag UID and must be unique. [order] is the user's
 * configured base ordering; the actual tap order is randomised each morning.
 */
@Entity(
    tableName = "nfc_tags",
    indices = [Index(value = ["tagId"], unique = true)]
)
data class NfcTag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Hardware tag UID (hex string). */
    val tagId: String,

    val label: String,

    val location: String,

    /** User-defined base ordering. Stored as "tag_order" ("order" is a SQL keyword). */
    @ColumnInfo(name = "tag_order")
    val order: Int = 0,

    val isActive: Boolean = true,

    /** Epoch millis the tag was registered. */
    val registeredAt: Long = System.currentTimeMillis()
)
