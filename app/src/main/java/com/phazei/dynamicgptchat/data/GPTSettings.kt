package com.phazei.dynamicgptchat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gpt_settings")
data class GPTSettings(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "system_message") val systemMessage: String = "",
    @ColumnInfo(name = "mode") val mode: String = "ChatCompletion",
    @ColumnInfo(name = "model") val model: String = "gpt-3.5-turbo",
    @ColumnInfo(name = "temperature") val temperature: Float = 0.5f,
    @ColumnInfo(name = "max_tokens") val maxTokens: Int = 2000,
    @ColumnInfo(name = "top_p") val topP: Float = 0.5f,
    @ColumnInfo(name = "frequency_penalty") val frequencyPenalty: Float = 0f,
    @ColumnInfo(name = "presence_penalty") val presencePenalty: Float = 0f,
    @ColumnInfo(name = "n") val n: Int = 1,
    @ColumnInfo(name = "best_of") val bestOf: Int = 1,
    @ColumnInfo(name = "stop") val stop: String = "",
    @ColumnInfo(name = "inject_start_text") val injectStartText: String = "",
    @ColumnInfo(name = "inject_restart_text") val injectRestartText: String = ""
) {
    override fun toString(): String {
        return "model: $model, temperature: $temperature, maxTokens: $maxTokens"
    }
}
