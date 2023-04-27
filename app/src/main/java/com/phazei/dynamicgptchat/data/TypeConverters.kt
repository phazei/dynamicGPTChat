package com.phazei.dynamicgptchat.data

import androidx.room.TypeConverter
import com.aallam.openai.api.core.Usage
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
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

class ListTypeConverter {
    private val moshi = Moshi.Builder().build()
    private val type = Types.newParameterizedType(MutableList::class.java, String::class.java)
    private val adapter = moshi.adapter<MutableList<String>>(type)

    @TypeConverter
    fun fromJson(json: String?): MutableList<String>? {
        return adapter.fromJson(json ?: "[]")
    }

    @TypeConverter
    fun toJson(list: MutableList<String>?): String? {
        return adapter.toJson(list)
    }
}

class MapConverter {
    private val moshi = Moshi.Builder().build()
    private val type = Types.newParameterizedType(Map::class.java, Integer::class.java, Integer::class.java)
    private val adapter = moshi.adapter<MutableMap<Int, Int>>(type)

    @TypeConverter
    fun fromStringToMap(value: String?): MutableMap<Int, Int>? {
        return adapter.fromJson(value ?: "{}")
    }

    @TypeConverter
    fun fromMapToString(map: MutableMap<Int, Int>?): String? {
        return adapter.toJson(map)
    }
}