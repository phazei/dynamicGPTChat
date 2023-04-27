package com.phazei.dynamicgptchat.data.datastore

import androidx.datastore.core.Serializer
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.InputStream
import java.io.OutputStream

class AppSettingsSerializer(
    private val cryptoManager: CryptoManager
) : Serializer<AppSettings> {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val appSettingsAdapter = moshi.adapter(AppSettings::class.java)

    override val defaultValue: AppSettings
        get() = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        val decryptedBytes = cryptoManager.decrypt(input)
        return try {
            appSettingsAdapter.fromJson(decryptedBytes.decodeToString()) ?: defaultValue
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        cryptoManager.encrypt(
            bytes = appSettingsAdapter.toJson(t).encodeToByteArray(),
            outputStream = output
        )
    }
}
