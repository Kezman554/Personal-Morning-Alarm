package com.personalmorningalarm.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personalmorningalarm.data.entity.BundledQuote
import kotlinx.coroutines.flow.Flow

@Dao
interface BundledQuoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quote: BundledQuote): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotes: List<BundledQuote>)

    @Update
    suspend fun update(quote: BundledQuote)

    @Delete
    suspend fun delete(quote: BundledQuote)

    @Query("SELECT * FROM bundled_quotes ORDER BY id ASC")
    suspend fun getAll(): List<BundledQuote>

    /** Reactive stream of all quotes; drives the quote-management list. */
    @Query("SELECT * FROM bundled_quotes ORDER BY id ASC")
    fun observeAll(): Flow<List<BundledQuote>>

    @Query("SELECT COUNT(*) FROM bundled_quotes")
    suspend fun count(): Int

    /** A single random quote, or null if the pool is empty. */
    @Query("SELECT * FROM bundled_quotes ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(): BundledQuote?
}
