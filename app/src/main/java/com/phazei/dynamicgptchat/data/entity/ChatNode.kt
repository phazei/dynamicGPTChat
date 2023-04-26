package com.phazei.dynamicgptchat.data.entity

import androidx.room.*
import com.aallam.openai.api.core.Usage

@Entity(
    tableName = "chat_nodes",
    foreignKeys = [
        ForeignKey(
            entity = ChatTree::class,
            parentColumns = ["id"],
            childColumns = ["chat_tree_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChatNode::class,
            parentColumns = ["id"],
            childColumns = ["parent_node_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chat_tree_id"]),
        Index(value = ["parent_node_id"])
    ]
)
data class ChatNode(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = "chat_tree_id") var chatTreeId: Long = 0,
    @ColumnInfo(name = "parent_node_id") var parentNodeId: Long?,
    var prompt: String,
) : BaseEntity() {
    @ColumnInfo(name = "active_child_index") var activeChildIndex: Int = 0
    var error: String? = null
    @Ignore
    var moderation: String? = null
    @ColumnInfo(name = "finish_reason") var finishReason: String = ""
    @Embedded(prefix = "use_") var usage: Usage = Usage()
    var response: String = ""

    @Ignore
    lateinit var parent: ChatNode
    fun parentInitialized() = ::parent.isInitialized
    @Ignore
    var children: MutableList<ChatNode> = mutableListOf()

    constructor() : this(
        id = 0,
        chatTreeId = 0,
        parentNodeId = null,
        prompt = "",
    )
}
