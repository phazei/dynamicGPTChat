package com.phazei.airequests

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.phazei.dynamicgptchat.data.repo.OpenAIRepository
import javax.inject.Inject
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.model.ModelId
import com.squareup.moshi.Moshi

@OptIn(BetaOpenAI::class)
class AIHelperRequests @Inject constructor(private val openAIRepository: OpenAIRepository) {
    private val jsonAdapter = Moshi.Builder().build().adapter<Any>(Any::class.java).lenient()

    /**
     * Is given an initial message from a conversation and returns a title provided by GPT
     */
    suspend fun getChatNodeTitle(message: String): String {
        val messages = listOf(
            ChatMessage(ChatRole.System, "Generate a 3 to 5 word title for the text provided."),
            ChatMessage(ChatRole.User, message),
            ChatMessage(ChatRole.System, "Provide a 3 to 5 word title for the last user message. Make it witty")
        )
        val chatCompletion = openAIRepository.chatCompleteText(getSimpleChatCompletionRequestObject(messages, "gpt-3.5-turbo"))
        return chatCompletion.choices.joinToString("") { it.message?.content?.trim()?.trim('.', '"','\'') + "" }
    }

    suspend fun getGeneralRequest(message: String, model: String?, chat: Boolean = false): String {
        return if (!chat) {
            val prompt = message.trim()
            val textCompletion = openAIRepository.completeText(getCompletionRequestObject(prompt, model))
            textCompletion.choices.joinToString("\n=========\n") { choice -> choice.text }
        } else {
            val messages = listOf( // ChatMessage(ChatRole.System, ""),
                ChatMessage(ChatRole.User, message.trim())
            )
            val chatCompletion = openAIRepository.chatCompleteText(getSimpleChatCompletionRequestObject(messages, model))
            chatCompletion.choices.joinToString("\n=========\n") {
                it.message?.role?.role + ": " + it.message?.content
            }
        }
    }

    fun getCompletionRequestObject(prompt: String, model: String? = null): CompletionRequest {
        val modelName = model ?: "gpt-3.5-turbo"
        val completionRequest = CompletionRequest(
            model = ModelId(modelName),
            prompt = prompt,
            temperature = 0.7,
            maxTokens = 50,
            // topP = 0.5,
            // frequencyPenalty = 0.0,
            // presencePenalty = 0.0,
            // stop = null,
            // logitBias = null,
            n = 1
        )
        return completionRequest
    }

    fun getSimpleChatCompletionRequestObject(messages: List<ChatMessage>, model: String? = null): ChatCompletionRequest {
        val modelName = model ?: "gpt-3.5-turbo"
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(modelName),
            messages = messages,
            temperature = 0.7,
            maxTokens = 50,
            // topP = 0.5,
            // frequencyPenalty = 0.0,
            // presencePenalty = 0.0,
            // stop = null,
            // logitBias = null,
        )
        return chatCompletionRequest
    }

}