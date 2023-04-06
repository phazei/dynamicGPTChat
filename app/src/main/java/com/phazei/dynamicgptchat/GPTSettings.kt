package com.phazei.dynamicgptchat

class GPTSettings(
    var temperature: Float,
    var maxTokens: Int,
    var model: String,
    var topP: Int,
    var frequencyPenalty: Int,
    var presencePenalty: Int
) {
    constructor() : this(0.5f, 250, "gpt-3.5-turbo", 1, 0, 0)

    override fun toString(): String {
        return "model: $model, temperature: $temperature, maxTokens: $maxTokens"
    }
}