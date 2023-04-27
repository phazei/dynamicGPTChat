package com.phazei.dynamicgptchat.data

import androidx.room.TypeConverter
import com.aallam.openai.api.core.Usage
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.squareup.moshi.Moshi
import java.util.*

class DateConverter {
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return if (timestamp != null) Date(timestamp) else null
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return date?.time
    }
}

class UsageTypeConverter {
    private val moshi = Moshi.Builder().build()

    @ToJson
    @TypeConverter
    fun usageToJson(usage: Usage?): String? {
        return usage?.let { moshi.adapter(Usage::class.java).toJson(it) }
    }

    @FromJson
    @TypeConverter
    fun jsonToUsage(jsonString: String?): Usage? {
        return jsonString?.let { moshi.adapter(Usage::class.java).fromJson(it) }
    }
}