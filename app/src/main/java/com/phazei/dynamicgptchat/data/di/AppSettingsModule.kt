package com.phazei.dynamicgptchat.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import androidx.datastore.dataStoreFile
import com.phazei.dynamicgptchat.data.datastore.AppSettings
import com.phazei.dynamicgptchat.data.datastore.AppSettingsSerializer
import com.phazei.dynamicgptchat.data.datastore.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppSettingsModule {

    private const val DATA_STORE_FILE_NAME = "app_settings.pb"

    @Singleton
    @Provides
    fun provideProtoDataStore(@ApplicationContext appContext: Context): DataStore<AppSettings> {
        return DataStoreFactory.create(
            serializer = AppSettingsSerializer(CryptoManager()),
            produceFile = { appContext.dataStoreFile(DATA_STORE_FILE_NAME) },
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { AppSettings() }
            ),
        )
    }
}
