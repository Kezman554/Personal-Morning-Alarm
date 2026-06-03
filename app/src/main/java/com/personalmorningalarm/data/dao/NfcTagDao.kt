package com.personalmorningalarm.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personalmorningalarm.data.entity.NfcTag

@Dao
interface NfcTagDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: NfcTag): Long

    @Update
    suspend fun update(tag: NfcTag)

    @Delete
    suspend fun delete(tag: NfcTag)

    @Query("SELECT * FROM nfc_tags ORDER BY tag_order ASC")
    suspend fun getAll(): List<NfcTag>

    @Query("SELECT * FROM nfc_tags WHERE id = :id")
    suspend fun getById(id: Long): NfcTag?

    @Query("SELECT * FROM nfc_tags WHERE tagId = :tagId LIMIT 1")
    suspend fun getByTagId(tagId: String): NfcTag?

    @Query("SELECT * FROM nfc_tags WHERE isActive = 1 ORDER BY tag_order ASC")
    suspend fun getActiveNfcTags(): List<NfcTag>
}
