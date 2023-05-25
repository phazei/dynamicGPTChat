package com.phazei.dynamicgptchat.data.pojo

import com.aallam.openai.api.core.Usage

class MutableUsage(
    var promptTokens: Int = 0,
    var completionTokens: Int = 0,
    var totalTokens: Int = 0,
) {
    operator fun plusAssign(other: MutableUsage) {
        promptTokens += other.promptTokens
        completionTokens += other.completionTokens
        totalTokens += other.totalTokens
    }
}

fun Usage.toMutable(): MutableUsage = MutableUsage(
    promptTokens?:0,
    completionTokens?:0,
    totalTokens?:0
)
