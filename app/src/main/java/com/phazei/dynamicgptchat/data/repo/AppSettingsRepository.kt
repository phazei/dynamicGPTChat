package com.phazei.dynamicgptchat.data.repo

import androidx.datastore.core.DataStore
import com.phazei.dynamicgptchat.data.datastore.AppSettings
import com.phazei.dynamicgptchat.data.datastore.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
// import kotlinx.coroutines.flow.transform
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepository @Inject constructor(private val dataStore: DataStore<AppSettings>) {

    val appSettingsFlow: Flow<AppSettings> = dataStore.data
                                                    // .transform { appSettings ->
                                                    //     // Perform checks or modifications to the appSettings object
                                                    //     emit(appSettings)
                                                    // }
    val themeFlow: Flow<Theme> = appSettingsFlow.map { it.theme }.distinctUntilChanged()
    val openAIkeyFlow: Flow<String> = appSettingsFlow.map { it.openAIkey ?: "" }.distinctUntilChanged()

    suspend fun updateAppSettings(appSettings: AppSettings) {
        dataStore.updateData { appSettings }
    }
}