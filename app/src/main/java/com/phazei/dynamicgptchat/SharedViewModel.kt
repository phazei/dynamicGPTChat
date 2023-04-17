package com.phazei.dynamicgptchat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.phazei.dynamicgptchat.data.*
import kotlinx.coroutines.launch

class SharedViewModel(
    appDatabase: AppDatabase,
    chatTreeDao: ChatTreeDao,
    chatNodeDao: ChatNodeDao,
    gptSettingsDao: GPTSettingsDao
) : ViewModel() {
    val chatRepository = ChatRepository(appDatabase, chatTreeDao, chatNodeDao, gptSettingsDao)
    var activeChatTree: ChatTree? = null
    val onFabClick: MutableLiveData<(() -> Unit)?> = MutableLiveData(null)

    //factory for passing parameters to SharedViewModel upon creation
    companion object {
        @Suppress("UNCHECKED_CAST")
        class Factory(private val appDatabase: AppDatabase) : ViewModelProvider.Factory {
            private val chatTreeDao = appDatabase.chatTreeDao()
            private val chatNodeDao = appDatabase.chatNodeDao()
            private val gptSettingsDao = appDatabase.gptSettingsDao()
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
                    return SharedViewModel(appDatabase, chatTreeDao, chatNodeDao, gptSettingsDao) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}