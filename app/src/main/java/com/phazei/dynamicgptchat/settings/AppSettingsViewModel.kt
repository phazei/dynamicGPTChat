package com.phazei.dynamicgptchat.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phazei.dynamicgptchat.data.datastore.AppSettings
import com.phazei.dynamicgptchat.data.repo.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(private val appSettingsRepository: AppSettingsRepository) : ViewModel() {

    val appSettingsFlow: Flow<AppSettings> = appSettingsRepository.appSettingsFlow

    fun updateAppSettings(appSettings: AppSettings) {
        viewModelScope.launch {
            appSettingsRepository.updateAppSettings(appSettings)
        }
    }

    private val _text = MutableLiveData<String>().apply {
        value = "This is App Settings"
    }
    val text: LiveData<String> = _text
}