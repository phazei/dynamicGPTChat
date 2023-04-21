package com.phazei.dynamicgptchat.data.datastore

import androidx.datastore.core.Serializer
import com.google.gson.Gson
import java.io.InputStream
import java.io.OutputStream

class AppSettingsSerializer(
    private val cryptoManager: CryptoManager
) : Serializer<AppSettings> {

    private val gson = Gson()

    override val defaultValue: AppSettings
        get() = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        val decryptedBytes = cryptoManager.decrypt(input)
        return try {
            gson.fromJson(decryptedBytes.decodeToString(), AppSettings::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        cryptoManager.encrypt(
            bytes = gson.toJson(t).encodeToByteArray(),
            outputStream = output
        )
    }
}
