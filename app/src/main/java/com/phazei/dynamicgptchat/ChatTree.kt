package com.phazei.dynamicgptchat

import androidx.room.*

@Entity(tableName = "chat_trees")
data class ChatTree(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var title: String,
    @Embedded(prefix = "gpt_") var gptSettings: GPTSettings,
    @ColumnInfo(name = "root_chat_node_id") var rootChatNodeId: Long? = null,
) : BaseEntity() {
    @Ignore
    lateinit var rootNode: ChatNode
    fun rootNodeInitialized() = ::rootNode.isInitialized

    constructor(title: String, gptSettings: GPTSettings) : this(
        title = title,
        gptSettings = gptSettings,
        rootChatNodeId = null
    )
}
