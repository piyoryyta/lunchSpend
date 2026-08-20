package com.piyoryyta.lunchspend.data.db

import androidx.room.TypeConverter
import com.piyoryyta.lunchspend.data.entity.LineItemType

class Converters {
    @TypeConverter
    fun fromLineItemType(value: LineItemType): String = value.name

    @TypeConverter
    fun toLineItemType(value: String): LineItemType = LineItemType.valueOf(value)
}
