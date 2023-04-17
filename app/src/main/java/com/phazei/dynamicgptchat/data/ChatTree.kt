package com.phazei.dynamicgptchat.data

import androidx.room.*

@Entity(tableName = "chat_trees")
data class ChatTree(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var title: String,
    //TODO: make settings its own table and load when retrieving tree
    @Embedded(prefix = "gpt_") var gptSettings: GPTSettings,
    @ColumnInfo(name = "root_chat_node_id") var rootChatNodeId: Long? = null,
) : BaseEntity() {
    var tempPrompt: String = ""
    @Ignore
    lateinit var rootNode: ChatNode
    //necessary when checking on save in ChatRepository, not available outside of this class:
    fun rootNodeInitialized() = ::rootNode.isInitialized

    constructor(title: String, gptSettings: GPTSettings) : this(
        title = title,
        gptSettings = gptSettings,
        rootChatNodeId = null
    )
}
