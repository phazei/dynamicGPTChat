package com.phazei.dynamicgptchat.data.di

import android.content.Context
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
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideChatTreeDao(appDatabase: AppDatabase): ChatTreeDao {
        return appDatabase.chatTreeDao()
    }

    @Provides
    @Singleton
    fun provideChatNodeDao(appDatabase: AppDatabase): ChatNodeDao {
        return appDatabase.chatNodeDao()
    }

    @Provides
    @Singleton
    fun provideGPTSettingsDao(appDatabase: AppDatabase): GPTSettingsDao {
        return appDatabase.gptSettingsDao()
    }
}
