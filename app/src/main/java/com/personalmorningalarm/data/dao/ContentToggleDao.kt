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

    /**
     * Every query that reads whole rows filters on [knownTypes] — pass
     * [ContentType.knownNames]. A row naming a type this build doesn't have would
     * otherwise reach the enum converter and throw, taking the app down at launch;
     * filtering in SQL means such a row is never read, and never rewritten either,
     * so a newer build's data survives untouched. Callers go through
     * [com.personalmorningalarm.data.AlarmRepository], which supplies the names.
     */
    @Query("SELECT * FROM content_toggles WHERE contentType IN (:knownTypes) ORDER BY display_order ASC")
    suspend fun getAll(knownTypes: List<String>): List<ContentToggle>

    @Query(
        "SELECT * FROM content_toggles WHERE isEnabled = 1 AND contentType IN (:knownTypes) " +
            "ORDER BY display_order ASC"
    )
    suspend fun getEnabledContentToggles(knownTypes: List<String>): List<ContentToggle>

    /** Safe without a filter: [type] is itself a known value. */
    @Query("SELECT * FROM content_toggles WHERE contentType = :type LIMIT 1")
    suspend fun getByType(type: ContentType): ContentToggle?

    /** Reactive stream of all toggles for the settings UI. */
    @Query("SELECT * FROM content_toggles WHERE contentType IN (:knownTypes) ORDER BY display_order ASC")
    fun observeAll(knownTypes: List<String>): Flow<List<ContentToggle>>
}
