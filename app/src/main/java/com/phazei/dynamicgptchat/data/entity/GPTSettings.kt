package com.phazei.dynamicgptchat.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gpt_settings")
data class GPTSettings(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = "system_message") var systemMessage: String = "",
    @ColumnInfo(name = "mode") var mode: String = "ChatCompletion",
    @ColumnInfo(name = "model") var model: String = "gpt-3.5-turbo",
    @ColumnInfo(name = "temperature") var temperature: Float = 0.5f,
    @ColumnInfo(name = "max_tokens") var maxTokens: Int = 2000,
    @ColumnInfo(name = "top_p") var topP: Float = 0.5f,
    @ColumnInfo(name = "frequency_penalty") var frequencyPenalty: Float = 0f,
    @ColumnInfo(name = "presence_penalty") var presencePenalty: Float = 0f,
    @ColumnInfo(name = "n") var n: Int = 1,
    @ColumnInfo(name = "best_of") var bestOf: Int = 1,
    @ColumnInfo(name = "stop") var stop: MutableList<String> = mutableListOf(),
    @ColumnInfo(name = "logit_bias") var logitBias: MutableMap<Int, Int> = mutableMapOf(),
    @ColumnInfo(name = "inject_start_text") var injectStartText: String = "",
    @ColumnInfo(name = "inject_restart_text") var injectRestartText: String = "",
    @ColumnInfo(name = "moderate_content") var moderateContent: Boolean = false
) {

    /**
     * loops through all stop values and replaces text \\n with real \n
     */
    fun getStops(): List<String>? {
        return stop.map { it.replace("\\n", "\n") }.takeIf { it.isNotEmpty() }
    }

    fun getLogitBiasAsString(): Map<String, Int>? {
        return logitBias.takeIf { it.isNotEmpty() }?.mapKeys { it.key.toString() }
    }

    override fun toString(): String {
        return "model: $model, temperature: $temperature, maxTokens: $maxTokens"
    }



}
