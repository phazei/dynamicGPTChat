package com.phazei.dynamicgptchat

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.*
import java.util.List

@Entity(tableName = "chat_trees")
data class ChatTree(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var title: String,
    val systemMessage: String,
    val defaultGPTSettings: GPTSettings,
    var activeChildNodeId: Long? = null
) {
    var uuid: String = UUID.randomUUID().toString()
}