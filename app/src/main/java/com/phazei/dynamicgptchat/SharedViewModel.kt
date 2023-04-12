package com.phazei.dynamicgptchat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.phazei.dynamicgptchat.data.*
import kotlinx.coroutines.launch

class SharedViewModel(appDatabase: AppDatabase, chatTreeDao: ChatTreeDao, chatNodeDao: ChatNodeDao) : ViewModel() {
    val chatRepository = ChatRepository(appDatabase, chatTreeDao, chatNodeDao)
    val chatTrees = MutableLiveData<MutableList<ChatTree>>()
    val modifiedChatTree = MutableLiveData<Pair<Int, ChatTree>>()
    var activeChatTree: ChatTree? = null

    init {
        fetchChatTrees()
    }

    fun fetchChatTrees() {
        viewModelScope.launch {
            chatTrees.value = chatRepository.getAllChatTrees()
        }
    }

    fun saveChatTree(chatTree: ChatTree) {
        viewModelScope.launch {
            chatRepository.saveChatTree(chatTree)
        }
    }

    fun addChatTree(chatTree: ChatTree) {
        viewModelScope.launch {
            chatRepository.saveChatTree(chatTree)
            chatTrees.value?.add(chatTree)
            // chatTrees.value = chatTrees.value //trigger live listener if wanted
        }
    }

    fun deleteChatTree(chatTree: ChatTree, position: Int) {
        viewModelScope.launch {
            chatRepository.deleteChatTree(chatTree)
            chatTrees.value?.removeAt(position)
            // chatTrees.value = chatTrees.value
        }
    }

    //factory for passing parameters to SharedViewModel upon creation
    companion object {
        @Suppress("UNCHECKED_CAST")
        class Factory(private val appDatabase: AppDatabase) : ViewModelProvider.Factory {
            private val chatTreeDao = appDatabase.chatTreeDao()
            private val chatNodeDao = appDatabase.chatNodeDao()
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
                    return SharedViewModel(appDatabase, chatTreeDao, chatNodeDao) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
