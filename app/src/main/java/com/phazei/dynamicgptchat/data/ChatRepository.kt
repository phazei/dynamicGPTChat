package com.phazei.dynamicgptchat.data

import androidx.room.withTransaction
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(BetaOpenAI::class)
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

    /**
     * Saves a ChatTree making sure a rootNode is set
     */
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

    /**
     * Updates a chatNode. Ensures parent node exists.
     * Also updates parent.activeChildIndex
     */
    suspend fun saveChatNode(chatNode: ChatNode) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                if (chatNode.parentNodeId == null) {
                    if (chatNode.parentInitialized()) {
                        chatNode.parentNodeId = chatNode.parent.id
                    } else {
                        throw IllegalArgumentException("ChatNode must have a parentNodeId or an initialized parent")
                    }
                }
                if (chatNode.id == 0L) {
                    chatNode.id = chatNodeDao.insert(chatNode)
                } else {
                    chatNodeDao.updateWithTimestamp(chatNode)
                }
                if (chatNode.parentInitialized()) {
                    chatNode.parent.children.add(chatNode)
                    chatNode.parent.activeChildIndex = chatNode.parent.children.indexOf(chatNode)
                    chatNodeDao.update(chatNode.parent)
                }
            }
        }
    }

    /**
     * Loads all children for entire branch and assigns parent as well
     */
    suspend fun loadChildren(chatNode: ChatNode) {
        withContext(Dispatchers.IO) {
            suspend fun processChildren(node: ChatNode) {
                val children = chatNodeDao.getChildrenOfChatNode(node.chatTreeId, node.id)
                node.children = children
                children.forEach { child ->
                    child.parent = node
                    processChildren(child)
                }
            }
            processChildren(chatNode)
        }
    }

    suspend fun deleteChatNode(chatNode: ChatNode) = chatNodeDao.delete(chatNode)

    fun getActiveLeaf(chatNode: ChatNode): ChatNode {
        var currentNode = chatNode
        while (currentNode.children.isNotEmpty()) {
            currentNode = currentNode.children[currentNode.activeChildIndex]
        }
        return currentNode
    }

    fun getActiveBranch(chatNode: ChatNode): MutableList<ChatNode> {
        val activeBranch = mutableListOf<ChatNode>()
        var currentNode = chatNode
        activeBranch.add(currentNode)
        while (currentNode.children.isNotEmpty()) {
            currentNode = currentNode.children[currentNode.activeChildIndex]
            activeBranch.add(currentNode)
        }
        return activeBranch
    }

    fun createChatMessageList(targetNode: ChatNode, chatTree: ChatTree): List<ChatMessage> {
        val chatMessages = mutableListOf<ChatMessage>()

        // Add the system message first
        chatMessages.add(ChatMessage(ChatRole.System, chatTree.gptSettings.systemMessage))

        // Helper function to traverse the tree and add messages to the list
        fun traverseTree(node: ChatNode) {
            if (node.parentInitialized()) {
                traverseTree(node.parent)
            } else {
                // This is the rootNode, it contains only children and no messages
                return
            }
            // Add user prompt
            chatMessages.add(ChatMessage(ChatRole.User, node.prompt))

            // Add assistant response only if the node is not the input chatNode
            if (node != targetNode) {
                chatMessages.add(ChatMessage(ChatRole.Assistant, node.response))
            }
        }
        traverseTree(targetNode)

        return chatMessages
    }
}