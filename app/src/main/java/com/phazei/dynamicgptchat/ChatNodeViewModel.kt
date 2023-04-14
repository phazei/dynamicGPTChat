package com.phazei.dynamicgptchat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.phazei.dynamicgptchat.data.*
import kotlinx.coroutines.launch

class ChatNodeViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    //factory for passing parameters to SharedViewModel upon creation
    companion object {
        @Suppress("UNCHECKED_CAST")
        class Factory(private val chatRepository: ChatRepository) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(ChatNodeViewModel::class.java)) {
                    return ChatNodeViewModel(chatRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
