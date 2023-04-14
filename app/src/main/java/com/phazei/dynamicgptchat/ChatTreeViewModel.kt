package com.phazei.dynamicgptchat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.phazei.dynamicgptchat.data.*
import kotlinx.coroutines.launch

class ChatTreeViewModel(private val chatRepository: ChatRepository) : ViewModel() {
    val chatTrees = MutableLiveData<MutableList<ChatTree>>()

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
            chatTrees.value?.add(0, chatTree)
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
        class Factory(private val chatRepository: ChatRepository) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(ChatTreeViewModel::class.java)) {
                    return ChatTreeViewModel(chatRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
