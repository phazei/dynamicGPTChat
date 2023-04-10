package com.phazei.dynamicgptchat

data class GPTSettings(
    val systemMessage: String = "",
    val mode: String = "Completion",
    val model: String = "gpt-3.5-turbo",
    val temperature: Float = 0.5f,
    val maxTokens: Int = 2000,
    val topP: Float = 0.5f,
    val frequencyPenalty: Float = 0f,
    val presencePenalty: Float = 0f,
    val bestOf: Int = 1,
    val injectStartText: String = "",
    val injectRestartText: String = ""
) {
    override fun toString(): String {
        return "model: $model, temperature: $temperature, maxTokens: $maxTokens"
    }
}