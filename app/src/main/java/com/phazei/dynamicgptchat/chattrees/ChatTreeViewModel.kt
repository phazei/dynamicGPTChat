package com.phazei.dynamicgptchat.chattrees

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.phazei.dynamicgptchat.data.*
import com.phazei.dynamicgptchat.data.entity.ChatTree
import com.phazei.dynamicgptchat.data.entity.GPTSettings
import com.phazei.dynamicgptchat.data.repo.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatTreeViewModel @Inject constructor(private val chatRepository: ChatRepository) : ViewModel() {
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

    fun saveGptSettings(gptSettings: GPTSettings) {
        viewModelScope.launch {
            chatRepository.saveGptSettings(gptSettings)
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
}
