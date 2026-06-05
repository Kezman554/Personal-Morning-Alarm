package com.personalmorningalarm.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personalmorningalarm.data.entity.ContentToggle
import com.personalmorningalarm.data.model.ContentType
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentToggleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(toggle: ContentToggle): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(toggles: List<ContentToggle>)

    @Update
    suspend fun update(toggle: ContentToggle)

    @Delete
    suspend fun delete(toggle: ContentToggle)

    @Query("SELECT * FROM content_toggles ORDER BY display_order ASC")
    suspend fun getAll(): List<ContentToggle>

    @Query("SELECT * FROM content_toggles WHERE isEnabled = 1 ORDER BY display_order ASC")
    suspend fun getEnabledContentToggles(): List<ContentToggle>

    @Query("SELECT * FROM content_toggles WHERE contentType = :type LIMIT 1")
    suspend fun getByType(type: ContentType): ContentToggle?

    /** Reactive stream of all toggles for the settings UI. */
    @Query("SELECT * FROM content_toggles ORDER BY display_order ASC")
    fun observeAll(): Flow<List<ContentToggle>>
}
