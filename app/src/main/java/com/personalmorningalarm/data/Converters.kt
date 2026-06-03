package com.personalmorningalarm.data

import androidx.room.TypeConverter
import com.personalmorningalarm.data.model.ContentType
import com.personalmorningalarm.data.model.MorningGoal

/** Room type converters for the enums used by the entities. */
class Converters {

    @TypeConverter
    fun fromMorningGoal(value: MorningGoal?): String? = value?.name

    @TypeConverter
    fun toMorningGoal(value: String?): MorningGoal? = value?.let { MorningGoal.valueOf(it) }

    @TypeConverter
    fun fromContentType(value: ContentType): String = value.name

    @TypeConverter
    fun toContentType(value: String): ContentType = ContentType.valueOf(value)
}
