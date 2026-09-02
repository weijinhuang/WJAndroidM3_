package com.wj.androidm3.business.countdown.data

import androidx.room.TypeConverter

class CountdownConverters {
    @TypeConverter
    fun fromStatus(status: CountdownStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): CountdownStatus = CountdownStatus.valueOf(value)
}
