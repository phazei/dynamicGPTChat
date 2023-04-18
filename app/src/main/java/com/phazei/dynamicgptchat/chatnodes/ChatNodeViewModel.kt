package com.phazei.dynamicgptchat.chatnodes

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.core.Usage
import com.aallam.openai.api.model.ModelId
import com.phazei.dynamicgptchat.data.*
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.data.entity.ChatTree
import com.phazei.dynamicgptchat.data.repo.ChatRepository
import com.phazei.dynamicgptchat.data.repo.ChatResponseWrapper
import com.phazei.dynamicgptchat.data.repo.OpenAIRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(BetaOpenAI::class)
class ChatNodeViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val openAIRepository = OpenAIRepository("123")
    private val _activeBranchUpdate = MutableSharedFlow<Pair<ChatNode, List<ChatNode>?>>()
    val activeBranchUpdate: SharedFlow<Pair<ChatNode, List<ChatNode>?>> = _activeBranchUpdate
    val activeRequests: StateFlow<Map<Long, Job>> = openAIRepository.activeRequests

    fun isRequestActive(chatTreeId: Long): Boolean {
        return openAIRepository.isRequestActive(chatTreeId)
    }
    fun makeChatCompletionRequest(chatTree: ChatTree, chatNode: ChatNode, streaming: Boolean = true) {
        if (chatTree.id != chatNode.chatTreeId) {
            throw IllegalArgumentException("chatNode must be child of chatTree")
        }
        viewModelScope.launch {
            val chatCompletionRequest = ChatCompletionRequest(
                model = ModelId(chatTree.gptSettings.model),
                messages = chatRepository.createChatMessageList(chatNode, chatTree),
                temperature = chatTree.gptSettings.temperature.toDouble(),
                maxTokens = chatTree.gptSettings.maxTokens,
                topP = chatTree.gptSettings.topP.toDouble(),
                frequencyPenalty = chatTree.gptSettings.frequencyPenalty.toDouble(),
                presencePenalty = chatTree.gptSettings.presencePenalty.toDouble(),
                stop = listOf(chatTree.gptSettings.stop),
                n = chatTree.gptSettings.n,
            )

            val job = openAIRepository.sendChatCompletionRequest(
                chatNode,
                chatCompletionRequest,
                onResponse = { ChatResponseWrapper ->
                    handleChatResponse(chatNode, ChatResponseWrapper)
                },
                onComplete = {
                    handleChatComplete(chatNode)
                },
                onError = { error ->
                    handleChatResponse(chatNode, error)
                },
                streaming = streaming
            )
            job.invokeOnCompletion {
                //handle chatAdapter
            }
        }
    }

    /**
     * Need to determine if response should be concatenated, should only if streaming
     */
    private fun handleChatResponse(chatNode: ChatNode, response: ChatResponseWrapper) {
        when (response) {
            is ChatResponseWrapper.Complete -> {
                val chatComplete = response.chatComplete
                chatNode.response = chatComplete.choices[0].message?.content.toString()
                chatNode.finishReason = chatComplete.choices[0].finishReason ?: ""
                chatNode.usage = chatComplete.usage ?: Usage()
            }
            is ChatResponseWrapper.Chunk -> {
                val chatChunk = response.chatChunk
                chatNode.response += chatChunk.choices[0].delta?.content!!
                chatNode.finishReason = chatChunk.choices[0].finishReason ?: ""
                if (chatChunk.usage == null) {
                    //TODO: count the chunks and set that as the completion tokens
                    // chatNode.usage?.completionTokens = (chatNode.usage?.completionTokens ?: 0) + 1
                } else {
                    chatNode.usage = chatChunk.usage ?: Usage()
                }
            }
            is ChatResponseWrapper.Error -> {
                val error = response.error
                chatNode.error = error.message ?: "Unknown error"
                handleChatComplete(chatNode)
            }
            else -> {
                //there is no else, the compiler made me put this here
            }
        }
        viewModelScope.launch {
            //need to trigger Adapter to update node
            _activeBranchUpdate.emit(Pair(chatNode, null))
        }

    }

    private fun handleChatComplete(chatNode: ChatNode) {
        viewModelScope.launch {
            chatRepository.saveChatNode(chatNode)
        }
    }

    fun cancelRequest(chatTreeId: Long) {
        openAIRepository.cancelRequest(chatTreeId)
        //need to see if we want to keep the latest chatNode, or dump it
        //has it even been saved?
        //if it was streaming and has data, save it and keep it as active
        //otherwise dump it
        //need to update adapter as well
    }

    fun loadChatTreeChildrenAndActiveBranch(chatTree: ChatTree) {
        viewModelScope.launch {
            chatRepository.loadChildren(chatTree.rootNode)
            updateActiveBranch(chatTree.rootNode)
        }
    }

    fun updateActiveBranch(newChatNode: ChatNode) {
        viewModelScope.launch {
            val activeBranch = chatRepository.getActiveBranch(newChatNode)
            _activeBranchUpdate.emit(Pair(newChatNode, activeBranch))
        }
    }

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
