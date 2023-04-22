package com.phazei.dynamicgptchat.prompts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PromptsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Prompts"
    }
    val text: LiveData<String> = _text
}