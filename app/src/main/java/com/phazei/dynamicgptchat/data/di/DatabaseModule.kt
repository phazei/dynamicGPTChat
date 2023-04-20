package com.phazei.dynamicgptchat.data.di

import android.content.Context
import androidx.room.Room
import com.phazei.dynamicgptchat.data.AppDatabase
import com.phazei.dynamicgptchat.data.dao.ChatNodeDao
import com.phazei.dynamicgptchat.data.dao.ChatTreeDao
import com.phazei.dynamicgptchat.data.dao.GPTSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    fun provideChatTreeDao(appDatabase: AppDatabase): ChatTreeDao {
        return appDatabase.chatTreeDao()
    }

    @Provides
    fun provideChatNodeDao(appDatabase: AppDatabase): ChatNodeDao {
        return appDatabase.chatNodeDao()
    }

    @Provides
    fun provideGPTSettingsDao(appDatabase: AppDatabase): GPTSettingsDao {
        return appDatabase.gptSettingsDao()
    }
}
