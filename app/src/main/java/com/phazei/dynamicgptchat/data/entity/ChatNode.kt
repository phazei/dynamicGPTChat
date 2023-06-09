package com.phazei.dynamicgptchat.data.entity

import androidx.room.*
import com.aallam.openai.api.core.Usage
import com.phazei.dynamicgptchat.data.pojo.MutableUsage

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
    var response: String = ""

    @ColumnInfo(name = "finish_reason") var finishReason: String = ""
    var model: String = ""
    var moderation: String? = null
    var error: String? = null

    @Embedded(prefix = "use_") var usage: MutableUsage = MutableUsage()
    @ColumnInfo(name = "active_child_index") var activeChildIndex: Int = 0

    @Ignore
    var chatTree: ChatTree? = null
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

    fun getNextSibling(): ChatNode? {
        val siblings = this.parent.children
        val currentIndex = this.parent.children.indexOf(this)
        val nextIndex = (currentIndex + 1) % siblings.size
        return try {
            siblings[nextIndex]
        } catch (e: Exception) {
            null
        }
    }

    fun getPrevSibling(): ChatNode? {
        val siblings = this.parent.children
        val currentIndex = siblings.indexOf(this)
        val previousIndex = (currentIndex - 1 + siblings.size) % siblings.size
        return try {
            siblings[previousIndex]
        } catch (e: Exception) {
            null
        }
    }
}
