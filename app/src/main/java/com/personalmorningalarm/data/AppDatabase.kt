package com.personalmorningalarm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.personalmorningalarm.data.dao.AlarmConfigDao
import com.personalmorningalarm.data.dao.AlarmEventDao
import com.personalmorningalarm.data.dao.BundledQuoteDao
import com.personalmorningalarm.data.dao.ContentToggleDao
import com.personalmorningalarm.data.dao.NfcTagDao
import com.personalmorningalarm.data.entity.AlarmConfig
import com.personalmorningalarm.data.entity.AlarmEvent
import com.personalmorningalarm.data.entity.BundledQuote
import com.personalmorningalarm.data.entity.ContentToggle
import com.personalmorningalarm.data.entity.NfcTag

@Database(
    entities = [
        AlarmConfig::class,
        AlarmEvent::class,
        NfcTag::class,
        ContentToggle::class,
        BundledQuote::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alarmConfigDao(): AlarmConfigDao
    abstract fun alarmEventDao(): AlarmEventDao
    abstract fun nfcTagDao(): NfcTagDao
    abstract fun contentToggleDao(): ContentToggleDao
    abstract fun bundledQuoteDao(): BundledQuoteDao

    companion object {
        private const val DB_NAME = "personal_morning_alarm.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build().also { INSTANCE = it }
            }
        }
    }
}
