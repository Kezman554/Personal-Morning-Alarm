package com.personalmorningalarm.data

import android.util.Log
import androidx.room.TypeConverter
import com.personalmorningalarm.data.model.ContentType
import com.personalmorningalarm.data.model.MorningGoal

/**
 * Room type converters for the enums used by the entities.
 *
 * Both enums are stored by name, so a name this build doesn't have — a downgrade,
 * or a value renamed in a later version — reaches these converters. `valueOf`
 * would throw there and take the whole app down at launch, which for an alarm is
 * the worst possible failure. Each converter instead degrades in whichever
 * direction loses least.
 */
class Converters {

    @TypeConverter
    fun fromMorningGoal(value: MorningGoal?): String? = value?.name

    /**
     * An unrecognised goal falls back to the default rather than failing the read:
     * it rides on the alarm config, so refusing the row would take the alarm with
     * it. A wrong goal label is a far smaller loss than an alarm that never fires.
     */
    @TypeConverter
    fun toMorningGoal(value: String?): MorningGoal? {
        if (value == null) return null
        return MorningGoal.entries.firstOrNull { it.name == value } ?: run {
            Log.w(TAG, "Unknown MorningGoal '$value' — falling back to ${MorningGoal.EXERCISE}")
            MorningGoal.EXERCISE
        }
    }

    @TypeConverter
    fun fromContentType(value: ContentType): String = value.name

    /**
     * Kept strict, unlike [toMorningGoal]: a content toggle has no sensible
     * stand-in (guessing would show the wrong screen, and writing the guess back
     * would destroy the original name). Unknown rows are instead filtered out in
     * SQL before they ever reach here — see [com.personalmorningalarm.data.dao.ContentToggleDao].
     * Reaching this throw means a query forgot that filter.
     */
    @TypeConverter
    fun toContentType(value: String): ContentType =
        ContentType.entries.firstOrNull { it.name == value }
            ?: throw IllegalArgumentException(
                "Unknown ContentType '$value' — query must filter on ContentType.knownNames"
            )

    private companion object {
        const val TAG = "PMA"
    }
}
