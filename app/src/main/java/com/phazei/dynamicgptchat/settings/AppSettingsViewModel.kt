package com.phazei.dynamicgptchat.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AppSettingsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is App Settings"
    }
    val text: LiveData<String> = _text
}