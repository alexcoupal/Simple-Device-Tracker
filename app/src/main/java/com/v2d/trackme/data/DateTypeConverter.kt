package com.v2d.trackme.data

import androidx.room.TypeConverter
import java.util.*

/**
 * Created by acoupal on 3/8/2019.
 */
class DateTypeConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}