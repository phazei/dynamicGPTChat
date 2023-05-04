package com.phazei.dynamicgptchat.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.phazei.dynamicgptchat.data.datastore.AppSettings
import com.phazei.dynamicgptchat.data.datastore.AppSettingsSerializer
import com.phazei.dynamicgptchat.data.datastore.CryptoManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// import com.aallam.openai.api.model.Model
// import com.aallam.openai.api.model.ModelId
// import com.aallam.openai.api.model.ModelPermission
// import com.esotericsoftware.kryo.kryo5.Kryo
// import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object DataProviderModule {

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

    @Singleton
    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    // @Singleton
    // @Provides
    // fun provideJson(): Json {
    //     return Json {
    //         ignoreUnknownKeys = true
    //         isLenient = true
    //         encodeDefaults = true
    //     }
    // }

    // @Singleton
    // @Provides
    // fun provideKryo(): Kryo {
        // val kryo = Kryo()
        // kryo.register(ArrayList::class.java)
        // kryo.register(List::class.java)
        // kryo.register(Model::class.java)
        // kryo.register(ModelId::class.java)
        // kryo.register(ModelPermission::class.java)
        // Configure Kryo as needed
        // return kryo
    // }

}
