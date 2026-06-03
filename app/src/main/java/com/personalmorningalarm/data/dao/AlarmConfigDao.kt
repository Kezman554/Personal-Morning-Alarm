package com.personalmorningalarm.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personalmorningalarm.data.entity.AlarmConfig

@Dao
interface AlarmConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: AlarmConfig): Long

    @Update
    suspend fun update(config: AlarmConfig)

    @Delete
    suspend fun delete(config: AlarmConfig)

    /** Most recently created config row (the one currently in effect). */
    @Query("SELECT * FROM alarm_config ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun getCurrent(): AlarmConfig?

    @Query("SELECT * FROM alarm_config ORDER BY createdAt DESC")
    suspend fun getAll(): List<AlarmConfig>
}
