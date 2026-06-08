package com.personalmorningalarm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
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

        // v2: stretch duration on content_toggles. Non-destructive so registered
        // NFC tags and seeded quotes survive the upgrade.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE content_toggles " +
                        "ADD COLUMN duration_minutes INTEGER NOT NULL DEFAULT 5"
                )
            }
        }

        // v3: configurable NFC checkpoint count on alarm_config. Non-destructive
        // so the saved alarm, tags, and quotes survive the upgrade.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarm_config " +
                        "ADD COLUMN sequenceLength INTEGER NOT NULL DEFAULT 5"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
        }
    }
}
