package com.phazei.dynamicgptchat.data.repo

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.completion.TextCompletion
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.api.moderation.ModerationRequest
import com.aallam.openai.api.moderation.TextModeration
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.phazei.dynamicgptchat.data.entity.ChatNode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(BetaOpenAI::class)
class OpenAIRepository @Inject constructor(private val appSettingsRepository: AppSettingsRepository) {

    private lateinit var openAI: OpenAI

    private var apiKey: String? = null

    init {
        // Set up a collector to get the API key from the AppSettingsRepository
        appSettingsRepository.openAIkeyFlow
            .onEach { openAIkey ->
                apiKey = openAIkey
                if (apiKey!!.isNotEmpty()) {
                    updateOpenAIkey(apiKey!!)
                }
            }
            .launchIn(CoroutineScope(Dispatchers.IO)) // Use the IO dispatcher for network requests
    }

    fun updateOpenAIkey(apiKey: String) {
        openAI = OpenAI(apiKey)
    }
    fun updateOpenAIkey(config: OpenAIConfig) {
        openAI = OpenAI(config)
    }

    private val _activeRequests = MutableStateFlow<Map<Long, Job>>(emptyMap())
    val activeRequests: StateFlow<Map<Long, Job>> = _activeRequests

    private val cachedModels = mutableMapOf<String, Model>()
    private var cachedModelList: List<Model>? = null


    suspend fun listModels(forceRefresh: Boolean = false): List<Model> {
        if (cachedModelList == null || forceRefresh) {
            cachedModelList = openAI.models().map { it }
        }
        return cachedModelList ?: emptyList()
    }
    suspend fun getModelById(modelId: String, forceRefresh: Boolean = false): Model? {
        if (!cachedModels.containsKey(modelId) || forceRefresh) {
            val id = ModelId(modelId)
            val model = openAI.model(id)
            cachedModels[modelId] = model
        }
        return cachedModels[modelId]
    }

    suspend fun sendChatCompletionRequest(
        chatNode: ChatNode,
        chatCompletionRequest: ChatCompletionRequest,
        onResponse: (ChatResponseWrapper) -> Unit,
        onComplete: () -> Unit,
        onError: (ChatResponseWrapper) -> Unit,
        streaming: Boolean
    ): Job {
        val chatTreeId = chatNode.chatTreeId
        val requestJob = CoroutineScope(Dispatchers.IO).launch {
            if (streaming) {
                var errorHandled = false
                openAI.chatCompletions(chatCompletionRequest)
                    .onEach { response: ChatCompletionChunk ->
                        onResponse(ChatResponseWrapper.Chunk(response))
                    }
                    .catch { e: Throwable ->
                        when (e) {
                            is CancellationException -> {
                                errorHandled = true
                                onError(ChatResponseWrapper.Error(Exception("The quest was quashed!")))
                            }
                            is Exception -> {
                                errorHandled = true
                                onError(ChatResponseWrapper.Error(e))
                            }
                            else ->
                                throw e
                        }
                    }
                    .onCompletion { e: Throwable? ->
                        if (errorHandled) return@onCompletion
                        //errors will only reach here if they skipped the catch
                        when (e) {
                            null -> onComplete()
                            is CancellationException -> onError(ChatResponseWrapper.Error(Exception("Manually Halted")))
                            else -> throw e
                        }
                    }
                    .launchIn(this)
            } else {
                val chatCompletion = openAI.chatCompletion(chatCompletionRequest)
                onResponse(ChatResponseWrapper.Complete(chatCompletion))
                onComplete()
            }
        }
        activeRequestsAdd(chatTreeId, requestJob)
        requestJob.invokeOnCompletion {
            activeRequestsRemove(chatTreeId)
        }

        return requestJob
    }

    fun cancelRequest(chatTreeId: Long) {
        _activeRequests.value[chatTreeId]?.cancel()
        activeRequestsRemove(chatTreeId)
    }

    private fun activeRequestsAdd(chatTreeId: Long, job: Job) {
        val updatedRequests = _activeRequests.value.toMutableMap()
        updatedRequests[chatTreeId] = job
        _activeRequests.value = updatedRequests
    }

    private fun activeRequestsRemove(chatTreeId: Long) {
        val updatedRequests = _activeRequests.value.toMutableMap()
        updatedRequests.remove(chatTreeId)
        _activeRequests.value = updatedRequests
    }

    fun isRequestActive(chatTreeId: Long): Boolean {
        return _activeRequests.value.containsKey(chatTreeId)
    }

    suspend fun completeText(completionRequest: CompletionRequest): TextCompletion {
        return openAI.completion(completionRequest)
    }
    fun streamCompletions(completionRequest: CompletionRequest): Flow<TextCompletion> {
        return openAI.completions(completionRequest)
    }

    suspend fun moderateContent(moderationRequest: ModerationRequest): TextModeration {
        return openAI.moderations(moderationRequest)
    }
}

@OptIn(BetaOpenAI::class)
sealed class ChatResponseWrapper {
    data class Complete(val chatComplete: ChatCompletion) : ChatResponseWrapper()
    data class Chunk(val chatChunk: ChatCompletionChunk) : ChatResponseWrapper()
    data class Error(val error: Exception) : ChatResponseWrapper()
}