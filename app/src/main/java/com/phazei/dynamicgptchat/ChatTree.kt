package com.phazei.dynamicgptchat

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "chat_trees")
data class ChatTree(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var title: String,
    var systemMessage: String,
    var gptSettings: GPTSettings,
    var activeChildNodeId: Long? = null
) {
    var uuid: String = UUID.randomUUID().toString()
}