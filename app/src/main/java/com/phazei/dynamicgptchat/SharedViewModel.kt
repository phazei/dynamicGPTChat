package com.phazei.dynamicgptchat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val chatTrees = MutableLiveData<MutableList<ChatTree>>()
    val modifiedChatTree = MutableLiveData<Pair<Int, ChatTree>>()
    var activeChatTree: ChatTree? = null

    // Add any other properties or methods you need to manage your ChatTree list
}