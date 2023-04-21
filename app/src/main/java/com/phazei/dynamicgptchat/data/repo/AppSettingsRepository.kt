package com.phazei.dynamicgptchat.data.repo

import androidx.datastore.core.DataStore
import com.phazei.dynamicgptchat.data.datastore.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepository @Inject constructor(private val dataStore: DataStore<AppSettings>) {

    val appSettingsFlow: Flow<AppSettings> = dataStore.data

    suspend fun updateAppSettings(appSettings: AppSettings) {
        dataStore.updateData { appSettings }
    }
}