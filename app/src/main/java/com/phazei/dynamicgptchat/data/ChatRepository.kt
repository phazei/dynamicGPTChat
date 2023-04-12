package com.phazei.dynamicgptchat.data

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(private val database: AppDatabase, private val chatTreeDao: ChatTreeDao, private val chatNodeDao: ChatNodeDao) {

    suspend fun getAllChatTrees(): MutableList<ChatTree> {
        val chatTrees = chatTreeDao.getAll()
        chatTrees.forEach { it.rootNode = chatNodeDao.getById(it.rootChatNodeId!!) }
        return chatTrees
    }

    suspend fun getChatTreeById(id: Long): ChatTree {
        val chatTree = chatTreeDao.getById(id)
        chatTree.rootNode = chatNodeDao.getById(chatTree.rootChatNodeId!!)
        return chatTree
    }

    suspend fun saveChatTree(chatTree: ChatTree) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                // Save chatTree to get chatTreeId, and update chatNode with chatTreeId
                if (chatTree.id == 0L) {
                    chatTree.id = chatTreeDao.insert(chatTree)
                } else {
                    chatTreeDao.updateWithTimestamp(chatTree)
                }

                // If rootNode is initialized and rootChatNodeId is null, save or update rootNode and set rootChatNodeId
                if (chatTree.rootChatNodeId == null) {
                    if (chatTree.rootNodeInitialized()) {
                        val rootNode = chatTree.rootNode.apply { chatTreeId = chatTree.id }

                        if (rootNode.id == 0L) {
                            rootNode.id = chatNodeDao.insert(rootNode)
                        } else {
                            chatNodeDao.updateWithTimestamp(rootNode)
                        }

                        chatTree.rootChatNodeId = rootNode.id
                        chatTreeDao.updateWithTimestamp(chatTree)
                    } else {
                        throw IllegalArgumentException("ChatTree must have a rootChatNodeId or an initialized rootNode")
                    }
                }
            }
        }
    }

    suspend fun deleteChatTree(chatTree: ChatTree) = chatTreeDao.delete(chatTree)

    suspend fun saveChatNode(chatNode: ChatNode) {
        withContext(Dispatchers.IO) {
            if (chatNode.id == 0L) {
                chatNode.id = chatNodeDao.insert(chatNode)
            } else {
                chatNodeDao.updateWithTimestamp(chatNode)
            }
        }
    }

    suspend fun loadChildren(chatNode: ChatNode) {
        withContext(Dispatchers.IO) {
            val children = chatNodeDao.getChildrenOfChatNode(chatNode.chatTreeId, chatNode.id)
            chatNode.children = children
            children.forEach { loadChildren(it) }
        }
    }

    suspend fun deleteChatNode(chatNode: ChatNode) = chatNodeDao.delete(chatNode)

}