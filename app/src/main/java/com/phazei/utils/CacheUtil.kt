package com.phazei.utils

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

import io.paperdb.Paper
import java.lang.reflect.Type

// import kotlinx.serialization.decodeFromString
// import kotlinx.serialization.serializer
// import kotlinx.serialization.json.Json

// import com.esotericsoftware.kryo.kryo5.Kryo
// import com.esotericsoftware.kryo.kryo5.io.Input
// import com.esotericsoftware.kryo.kryo5.io.Output


/**
 * I have learned that there is nothing that will do serialization without being told
 * what the object is...
 */
class CacheUtil @Inject constructor(
    @ApplicationContext val context: Context,
    val moshi: Moshi,
    // val json: Json,
    // val kryo: Kryo
) {

    init {
        Paper.init(context)
    }

    inline fun <reified T> getObjectPage(key: String, durationTillExpired: Long): T? {
        val cacheData: CacheData<T>? = Paper.book().read<CacheData<T>>(key, null)

        return if (cacheData != null && cacheData.timestamp > System.currentTimeMillis() - durationTillExpired) {
            // Cache is valid, return the data
            cacheData.data
        } else {
            null
        }
    }

    inline fun <reified T> setObjectPage(key: String, data: T) {
        val cacheData = CacheData(data, System.currentTimeMillis())
        Paper.book().write(key, cacheData)
    }




    inline fun <reified T> getObjectJson(filename: String, durationTillExpired: Long, type: Type): T? {
        val cacheFile = File(context.cacheDir, "$filename.json")
        return if (cacheFile.exists() && cacheFile.lastModified() > System.currentTimeMillis() - durationTillExpired) {
            // Cache is valid, read from file
            cacheFile.inputStream().use { input ->
                val json = input.bufferedReader().use { it.readText() }
                if (json.isBlank()) {
                    null
                } else {
                    // val type = Types.newParameterizedType(List::class.java, Model::class.java)
                    val adapter = moshi.adapter<T>(type)
                    // val adapter = moshi.adapter<T>(T::class.java)
                    val result = adapter.fromJson(json)
                    result
                }
            }
        } else {
            null
        }
    }

    inline fun <reified T> setObjectJson(filename: String, data: T) {
        val cacheFile = File(context.cacheDir, "$filename.json")
        cacheFile.outputStream().use { output ->
            val adapter = moshi.adapter<T>(T::class.java)
            val json = adapter.toJson(data)
            output.bufferedWriter().use { it.write(json) }
        }
    }

    // Same thing but using Kryo
    //
    // inline fun <reified T> getObjectKy(filename: String, durationTillExpired: Long): T? {
    //     val cacheFile = File(context.cacheDir, "${filename}ky")
    //     return if (cacheFile.exists() && cacheFile.lastModified() > System.currentTimeMillis() - durationTillExpired) {
    //         // Cache is valid, read from file
    //         cacheFile.inputStream().use { input ->
    //             Input(input).use { kryoInput ->
    //                 kryo.readObject(kryoInput, T::class.java)
    //             }
    //         }
    //     } else {
    //         null
    //     }
    // }
    //
    // inline fun <reified T> setObjectKy(filename: String, data: T) {
    //     val cacheFile = File(context.cacheDir, "${filename}ky")
    //     cacheFile.outputStream().use { output ->
    //         Output(output).use { kryoOutput ->
    //             kryo.writeObject(kryoOutput, data)
    //         }
    //     }
    // }


    //Same thing but using Kotlin Serialization
    //
    // inline fun <reified T> getKObject(filename: String, durationTillExpired: Long): T? {
    //     val cacheFile = File(context.cacheDir, filename)
    //     return if (cacheFile.exists() && cacheFile.lastModified() > System.currentTimeMillis() - durationTillExpired) {
    //         // Cache is valid, read from file
    //         cacheFile.inputStream().use { input ->
    //             val jsonString = input.bufferedReader().use { it.readText() }
    //             json.decodeFromString<T>(jsonString)
    //         }
    //     } else {
    //         null
    //     }
    // }
    //
    // inline fun <reified T> setKObject(filename: String, data: T) {
    //     val cacheFile = File(context.cacheDir, filename)
    //     cacheFile.outputStream().use { output ->
    //         val jsonString = json.encodeToString(data)
    //         output.bufferedWriter().use { it.write(jsonString) }
    //     }
    // }

}

data class CacheData<T>(
    val data: T,
    val timestamp: Long
)
