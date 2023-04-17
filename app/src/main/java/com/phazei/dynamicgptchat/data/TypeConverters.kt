package com.phazei.dynamicgptchat.data

import androidx.room.TypeConverter
import com.aallam.openai.api.core.Usage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun usageToString(usage: Usage?): String? {
        return usage?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun stringToUsage(jsonString: String?): Usage? {
        return jsonString?.let { json.decodeFromString(it) }
    }
}
// In case kotlin serialization doesn't work
// class UsageTypeConverter {
//     private val gson = Gson()
//
//     @TypeConverter
//     fun usageToString(usage: Usage?): String? {
//         return gson.toJson(usage)
//     }
//
//     @TypeConverter
//     fun stringToUsage(json: String?): Usage? {
//         return gson.fromJson(json, Usage::class.java)
//     }
// }