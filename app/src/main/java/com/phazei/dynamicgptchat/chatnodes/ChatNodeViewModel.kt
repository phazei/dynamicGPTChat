package com.phazei.dynamicgptchat.chatnodes

import androidx.lifecycle.*
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.core.Usage
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.api.moderation.ModerationModel
import com.aallam.openai.api.moderation.ModerationRequest
import com.aallam.openai.api.moderation.TextModeration
import com.phazei.airequests.AIHelperRequests
import com.phazei.dynamicgptchat.data.*
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.data.entity.ChatTree
import com.phazei.dynamicgptchat.data.pojo.MutableUsage
import com.phazei.dynamicgptchat.data.pojo.toMutable
import com.phazei.dynamicgptchat.data.repo.ChatRepository
import com.phazei.dynamicgptchat.data.repo.ChatResponseWrapper
import com.phazei.dynamicgptchat.data.repo.OpenAIRepository
import com.phazei.utils.OpenAIHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(BetaOpenAI::class)
@HiltViewModel
class ChatNodeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val openAIRepository: OpenAIRepository,
    private val aiHelperRequests: AIHelperRequests,
) : ViewModel() {

    private val _activeBranchUpdate = MutableSharedFlow<Pair<ChatNode, List<ChatNode>?>?>(extraBufferCapacity = 16)
    val activeBranchUpdate: SharedFlow<Pair<ChatNode, List<ChatNode>?>?> = _activeBranchUpdate
    val activeRequests: StateFlow<Map<Long, Job>> = openAIRepository.activeRequests

    val titleUpdate = MutableStateFlow<String?>(null)


    fun isRequestActive(chatTreeId: Long): Boolean {
        return openAIRepository.manage.isRequestActive(chatTreeId)
    }

    fun makeChatCompletionRequest(chatTree: ChatTree, chatNode: ChatNode, streaming: Boolean = true) {
        if (chatTree.id != chatNode.chatTreeId) {
            throw IllegalArgumentException("chatNode must be child of chatTree")
        }
        chatNode.chatTree = chatTree

        viewModelScope.launch {
            if (chatNode.id == 0L) {
                //brand new chat node, lets save it
                if (!chatNode.parentInitialized()) {
                    throw IllegalArgumentException("chatNode must have parent set")
                }
                chatRepository.saveChatNode(chatNode, true)
            }

            //clear out previous items if they exists
            chatNode.response = ""
            chatNode.error = null
            chatNode.moderation = null

            val chatCompletionRequest = ChatCompletionRequest(
                model = ModelId(chatTree.gptSettings.model),
                messages = chatRepository.createChatMessageList(chatNode, chatTree),
                temperature = chatTree.gptSettings.temperature.toDouble(),
                maxTokens = chatTree.gptSettings.maxTokens,
                topP = chatTree.gptSettings.topP.toDouble(),
                frequencyPenalty = chatTree.gptSettings.frequencyPenalty.toDouble(),
                presencePenalty = chatTree.gptSettings.presencePenalty.toDouble(),
                stop = chatTree.gptSettings.getStops(),
                logitBias = chatTree.gptSettings.getLogitBiasAsString(),
                // no good way to display "n" right now so ignore it for now
                // n = chatTree.gptSettings.n,
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
                // Log.d("LOG", chatComplete.toString())
                chatNode.response = chatComplete.choices[0].message?.content.toString()
                chatNode.finishReason = chatComplete.choices[0].finishReason ?: ""
                chatNode.usage += chatComplete.usage?.toMutable() ?: MutableUsage()
            }
            is ChatResponseWrapper.Chunk -> {
                val chatChunk = response.chatChunk
                // Log.d("LOG", chatChunk.toString())
                chatNode.response += chatChunk.choices[0].delta?.content ?: ""
                chatNode.finishReason = chatChunk.choices[0].finishReason ?: ""
                if (chatChunk.usage == null) {
                    // streaming responses don't currently return "usage" I don't think
                } else {
                    if (chatChunk.usage != null) {
                        chatNode.usage += chatChunk.usage?.toMutable() ?: MutableUsage()
                    } else {
                        chatNode.usage.completionTokens += 1
                        chatNode.usage.totalTokens += 1
                    }
                }
            }
            is ChatResponseWrapper.Error -> {
                val error = response.error
                chatNode.error = error.message ?: "Unknown Error Type: ${error.javaClass.simpleName} : Cause: ${error.cause?.toString() ?: "No cause"}"
                handleChatComplete(chatNode)
            }
        }
        //need to trigger Adapter to update node
        emitSingleChatNodeUpdate(chatNode)
    }

    /**
     * Should only be called when entirely complete
     */
    private fun handleChatComplete(chatNode: ChatNode) {
        viewModelScope.launch {
            chatRepository.saveChatNode(chatNode)

            chatNode.chatTree?.let {
                it.usage += chatNode.usage
                viewModelScope.launch {
                    chatRepository.saveChatTree(it)
                }
            }

            if (chatNode.error == null) {
                if (!chatNode.parent.parentInitialized()) {
                    // this is first message of conversation, generate title
                    updateChatTreeTitle(chatNode)
                }
                moderateChatNodeResponse(chatNode)
            }
            //one final update just in case it glitched previously
            delay(300)
            emitSingleChatNodeUpdate(chatNode)
        }
    }

    /**
     * Once request is complete, this will submit the response and retrieve a moderation result
     */
    private fun moderateChatNodeResponse(chatNode: ChatNode) {
        if (chatNode.chatTree?.gptSettings?.moderateContent == true) {
            viewModelScope.launch {
                val moderation: TextModeration
                try {
                    moderation = openAIRepository.moderateContent(ModerationRequest(mutableListOf(chatNode.prompt, chatNode.response), ModerationModel.Latest))
                } catch (e: Exception) {
                    //ignore, just skip it
                    return@launch
                }
                chatNode.moderation = OpenAIHelper.formatModerationDetails(moderation)
                chatRepository.saveChatNode(chatNode)
                emitSingleChatNodeUpdate(chatNode)
            }
        }
    }

    private fun updateChatTreeTitle(chatNode: ChatNode) {
        viewModelScope.launch {
            chatNode.chatTree?.let {
                val msg = "User: ${chatNode.prompt}" + "\nAssistant: ${chatNode.response}"
                flow {
                    emit(aiHelperRequests.getChatNodeTitle(msg))
                }.catch {
                    // ignore failed title request
                }.collect { title ->
                    it.title = title
                    chatRepository.saveChatTree(it)
                    titleUpdate.emit(it.title)
                }
            }
        }
    }

    fun cancelRequest(chatTreeId: Long) {
        openAIRepository.manage.cancelRequest(chatTreeId)
    }

    /**
     * When streaming the responses come in way too fast for emitter and UI to keep up
     * debouncing should greatly help
     */
    private val lastEmissionTimes = mutableMapOf<Long, Long>()
    private val debounceJobs = mutableMapOf<Long, Job>()
    private fun emitSingleChatNodeUpdate(updatedChatNode: ChatNode) {
        viewModelScope.launch {
            val key = updatedChatNode.chatTreeId
            debounceJobs[key]?.cancel()
            debounceJobs[key] = launch {
                val lastEmission = lastEmissionTimes.getOrDefault(key, 0)
                val currentTime = System.currentTimeMillis()
                val timeSinceLastEmission = currentTime - lastEmission

                //we want to send at least once every 300ms even if debounce is pushing it out
                if (timeSinceLastEmission < 300) { // Debounce duration
                    delay(300 - timeSinceLastEmission)
                }
                _activeBranchUpdate.emit(Pair(updatedChatNode, null))
                lastEmissionTimes[key] = System.currentTimeMillis()
            }
        }
    }

    /**
     * Necessary for initialization of Fragment
     */
    fun loadChatTreeChildrenAndActiveBranch(chatTree: ChatTree) {
        viewModelScope.launch {
            chatRepository.loadChildren(chatTree.rootNode, chatTree)
            updateAndEmitActiveBranch(chatTree.rootNode)
        }
    }

    /**
     * Retrieves entire branch descending from submitted ChatNode
     */
    fun updateAndEmitActiveBranch(newChatNode: ChatNode) {
        viewModelScope.launch {
            val activeBranch = chatRepository.getActiveBranch(newChatNode)
            _activeBranchUpdate.emit(Pair(newChatNode, activeBranch))
        }
    }

    fun saveChatNode(chatNode: ChatNode, makeActive: Boolean = false) {
        viewModelScope.launch {
            chatRepository.saveChatNode(chatNode, makeActive)
        }
    }

    /**
     * Marks a child node as the parent nodes active child
     */
    fun updateActiveSibling(chatNode: ChatNode) {
        viewModelScope.launch {
            if (chatNode.parentInitialized()) {
                val parent = chatNode.parent
                parent.activeChildIndex = parent.children.indexOf(chatNode)
                chatRepository.updateChatNode(parent)
                updateAndEmitActiveBranch(parent)
            }
        }
    }

    /**
     * Deletes a node from it's parent
     */
    fun deleteChildNode(chatNode: ChatNode) {
        viewModelScope.launch {
            if (chatNode.parentInitialized()) {
                val parent = chatNode.parent
                chatRepository.deleteChatNode(chatNode)
                updateAndEmitActiveBranch(parent)
            }
        }
    }
}
