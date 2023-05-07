package com.phazei.utils

import android.annotation.SuppressLint
import com.aallam.openai.api.model.Model
import java.text.SimpleDateFormat

object OpenAIHelper {
    fun filterModelList(mode: String, models: List<Model>): MutableList<String> {
        return when (mode) {
            "ChatCompletion" -> models.filter { it.id.id.contains("gpt") }
            "Completion" -> models.filter {
                it.id.id.startsWith("text-")
                        && !it.id.id.contains(":")
                        && !it.id.id.contains("text-similarity")
                        && !it.id.id.contains("text-search")
                        && !it.id.id.contains("edit")
                        && !it.id.id.contains("embedding")
            }
            else -> models
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
}