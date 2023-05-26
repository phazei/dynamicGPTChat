package com.phazei.utils

import android.annotation.SuppressLint
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.moderation.TextModeration
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import java.text.SimpleDateFormat
import kotlin.jvm.optionals.getOrNull

@OptIn(BetaOpenAI::class)
object OpenAIHelper {

    private val encodingRegistry: EncodingRegistry by lazy {
        Encodings.newLazyEncodingRegistry()
    }

    fun countTokens(text: String, model: String = "gpt-3.5-turbo"): Int {
        var encoding: Encoding? = null
        encoding = encodingRegistry.getEncodingForModel(model)
            .orElse(encodingRegistry.getEncodingForModel("gpt-3.5-turbo").getOrNull())
        return encoding.countTokensOrdinary(text)
    }

    fun countPromptTokens(messages: List<ChatMessage>, model: String): Int {
        val messageText = messages.joinToString("") { it.content }
        return countTokens(messageText, model)
    }

    fun filterModelList(mode: String, models: List<Model>): MutableList<String> {
        return when (mode) {
            "ChatCompletion" -> models.filter { it.id.id.contains("gpt") }
            "Completion" -> models.filter {
                (it.id.id.startsWith("text-") || it.id.id.contains("instruct"))
                        && !it.id.id.contains(":")
                        && !it.id.id.contains("text-similarity")
                        && !it.id.id.contains("text-search")
                        && !it.id.id.contains("edit")
                        && !it.id.id.contains("embedding")
                        && !it.id.id.contains("whisper")
                        && !it.id.id.startsWith("if-")
            }
            else -> models.filter {
                !it.id.id.contains(":")
                        && !it.id.id.contains("embedding")
                        && !it.id.id.contains("whisper")
                        && !it.id.id.startsWith("if-")
            }
        }.map { it.id.id }.sorted().toMutableList()
    }

    @SuppressLint("SimpleDateFormat")
    fun formatModelDetails(model: Model): String {
        return "ID: ${model.id.id}\n" +
                "Created: ${SimpleDateFormat("yyyy-MM-dd").format(model.created*1000)}\n" +
                "Owned by: ${model.ownedBy}\n" +
                "Permissions:\n" + model.permission.joinToString(separator = "\n") {
            val permissions = listOf(
                "Organization: ${it.organization} \n" to true,
                (if (it.isBlocking) "isBlocking" else "notBlocking") to true,
                "allowCreateEngine" to it.allowCreateEngine,
                "allowSampling" to it.allowSampling,
                "allowLogprobs" to it.allowLogprobs,
                "allowSearchIndices" to it.allowSearchIndices,
                "allowView" to it.allowView,
                "allowFineTuning" to it.allowFineTuning,
            )
            "  - ${permissions.filter { p -> p.second }.joinToString(", ") { p -> p.first }}"
        }
    }

    fun formatModerationDetails(moderation: TextModeration): String {

        val output = StringBuilder()
        moderation.results.forEachIndexed { index, moderationResult ->
            if (moderationResult.flagged) {
                val itemType = if (index == 0) "Prompt" else "Response"
                val flaggedCategoriesWithScores = mutableListOf<String>()

                val categories = moderationResult.categories
                val categoryScores = moderationResult.categoryScores

                val categoryList = listOf("hate", "hateThreatening", "selfHarm", "sexual", "sexualMinors", "violence", "violenceGraphic")

                categoryList.forEach { categoryName ->
                    // val categoryFlagged = categories::class.memberProperties.find { it.name == categoryName }?.get(categories) as? Boolean ?: false
                    // val categoryScore = categoryScores::class.memberProperties.find { it.name == categoryName }?.get(categoryScores) as? Double ?: 0.0

                    val categoryFlaggedField = categories.javaClass.getDeclaredField(categoryName)
                    categoryFlaggedField.isAccessible = true
                    val categoryFlagged = categoryFlaggedField.get(categories) as? Boolean ?: false

                    val categoryScoreField = categoryScores.javaClass.getDeclaredField(categoryName)
                    categoryScoreField.isAccessible = true
                    val categoryScore = categoryScoreField.get(categoryScores) as? Double ?: 0.0

                    if (categoryFlagged) {
                        flaggedCategoriesWithScores.add("$categoryName: ${"%.2f".format(categoryScore)}")
                    }
                }
                output.appendLine("$itemType: ${flaggedCategoriesWithScores.joinToString(", ")}")
            }
        }
        return output.toString().trim()
    }
}