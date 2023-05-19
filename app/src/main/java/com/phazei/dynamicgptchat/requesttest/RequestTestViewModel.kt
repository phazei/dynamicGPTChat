package com.phazei.dynamicgptchat.requesttest

import androidx.lifecycle.*
import com.phazei.airequests.AIHelperRequests
import com.phazei.dynamicgptchat.data.*
import com.phazei.dynamicgptchat.data.repo.OpenAIRepository
import com.phazei.utils.OpenAIHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("MemberVisibilityCanBePrivate")
@HiltViewModel
class RequestTestViewModel @Inject constructor(
    private val openAIRepository: OpenAIRepository,
    private val aiRequestHelper: AIHelperRequests,
) : ViewModel() {

    private val _requestUpdate = MutableSharedFlow<Any?>(extraBufferCapacity = 16)
    val requestUpdate: SharedFlow<Any?> = _requestUpdate

    fun makeRequest(prompt: String) {
        viewModelScope.launch {
            val textCompletion = openAIRepository.completeText(aiRequestHelper.getCompletionRequestObject(prompt))
        }
    }

    fun getConversationTitle(prompt: String): Flow<String> = flow {
        val response = aiRequestHelper.getChatNodeTitle(prompt)
        emit(response)
    }

    fun getGeneralRequest(prompt: String, model: String? = null, chat: Boolean = false): Flow<String> = flow {
        val response = aiRequestHelper.getGeneralRequest(prompt, model, chat)
        emit(response)
    }

    fun getModels(): Flow<List<String>> = flow {
        val modelIds = OpenAIHelper.filterModelList("", openAIRepository.listModels())
        emit(modelIds)
    }
}
