package com.phazei.dynamicgptchat.data

import androidx.room.*

@Dao
interface ChatNodeDao : BaseDao<ChatNode> {
    @Query("SELECT * FROM chat_nodes WHERE id = :id")
    suspend fun getById(id: Long): ChatNode

    @Query("SELECT * FROM chat_nodes WHERE chat_tree_id = :chatTreeId AND parent_node_id = :parentNodeId ORDER BY id ASC")
    suspend fun getChildrenOfChatNode(chatTreeId: Long, parentNodeId: Long): MutableList<ChatNode>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(chatNode: ChatNode): Long

    // @Update //this is in BaseDao
    // override suspend fun update(chatNode: ChatNode)

    @Delete
    suspend fun delete(chatNode: ChatNode)

    @Transaction
    suspend fun addChildToChatNode(parentNode: ChatNode, childNode: ChatNode) {
        childNode.parentNodeId = parentNode.id
        childNode.chatTreeId = parentNode.chatTreeId
        insert(childNode)
    }
}
