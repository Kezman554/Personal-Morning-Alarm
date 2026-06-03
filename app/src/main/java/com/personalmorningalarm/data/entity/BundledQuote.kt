package com.personalmorningalarm.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A motivational quote from the app's bundled pool (shown on quote content screens). */
@Entity(tableName = "bundled_quotes")
data class BundledQuote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val quoteText: String,

    val author: String? = null
)
