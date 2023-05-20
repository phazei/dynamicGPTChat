package com.phazei.dynamicgptchat.data.repo

import androidx.room.withTransaction
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.phazei.dynamicgptchat.data.AppDatabase
import com.phazei.dynamicgptchat.data.dao.ChatNodeDao
import com.phazei.dynamicgptchat.data.dao.ChatTreeDao
import com.phazei.dynamicgptchat.data.dao.GPTSettingsDao
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.data.entity.ChatTree
import com.phazei.dynamicgptchat.data.entity.GPTSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(BetaOpenAI::class)
@Singleton
class ChatRepository @Inject constructor(
    private val database: AppDatabase,
    private val chatTreeDao: ChatTreeDao,
    private val chatNodeDao: ChatNodeDao,
    private val gptSettingsDao: GPTSettingsDao
) {

    suspend fun getAllChatTrees(): MutableList<ChatTree> {
        val chatTrees = chatTreeDao.getAll()
        chatTrees.forEach {
            it.rootNode = chatNodeDao.getById(it.rootChatNodeId!!)
            it.gptSettings = gptSettingsDao.getById(it.gptSettingsId!!)
        }
        return chatTrees
    }

    suspend fun getChatTreeById(id: Long): ChatTree {
        val chatTree = chatTreeDao.getById(id)
        chatTree.rootNode = chatNodeDao.getById(chatTree.rootChatNodeId!!)
        chatTree.gptSettings = gptSettingsDao.getById(chatTree.gptSettingsId!!)
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
                var needsUpdate = false
                // If rootNode is initialized and rootChatNodeId is null, save or update rootNode and set rootChatNodeId
                if (chatTree.rootChatNodeId == null) {
                    val rootNode = chatTree.rootNode.apply { chatTreeId = chatTree.id }

                    if (rootNode.id == 0L) {
                        rootNode.id = chatNodeDao.insert(rootNode)
                    } else {
                        chatNodeDao.updateWithTimestamp(rootNode)
                    }
                    chatTree.rootChatNodeId = rootNode.id
                    needsUpdate = true
                }
                // If gptSettingsId is null, save or update gptSettings and set gptSettingsId
                if (chatTree.gptSettingsId == null) {
                    val gptSettings = chatTree.gptSettings

                    if (gptSettings.id == 0L) {
                        gptSettings.id = gptSettingsDao.insert(gptSettings)
                    } else {
                        gptSettingsDao.update(gptSettings)
                    }

                    chatTree.gptSettingsId = gptSettings.id
                    needsUpdate = true
                }
                if (needsUpdate) {
                    chatTreeDao.updateWithTimestamp(chatTree)
                }
            }
        }
    }

    suspend fun deleteChatTree(chatTree: ChatTree) = chatTreeDao.delete(chatTree)



    /**
     * Updates a chatNode. Ensures parent node exists.
     * Also updates parent.activeChildIndex
     */
    suspend fun saveChatNode(chatNode: ChatNode, makeActive: Boolean = false) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                var newNode = false
                if (chatNode.parentNodeId == null) {
                    if (chatNode.parentInitialized()) {
                        chatNode.parentNodeId = chatNode.parent.id
                    } else {
                        throw IllegalArgumentException("ChatNode must have a parentNodeId or an initialized parent")
                    }
                }
                if (chatNode.id == 0L) {
                    newNode = true
                    chatNode.id = chatNodeDao.insert(chatNode)
                } else {
                    chatNodeDao.updateWithTimestamp(chatNode)
                }
                if (makeActive && chatNode.parentInitialized()) {
                    if (chatNode.parent.children.indexOf(chatNode) == -1) {
                        chatNode.parent.children.add(chatNode)
                    }
                    chatNode.parent.activeChildIndex = chatNode.parent.children.indexOf(chatNode)
                    chatNodeDao.update(chatNode.parent)
                }
            }
        }
    }

    suspend fun updateChatNode(chatNode: ChatNode) {
        withContext(Dispatchers.IO) {
            chatNodeDao.update(chatNode)
        }
    }

    /**
     * When a child node is deleted, the parents active index needs to point to a valid
     * index, so check the index and modify it if necessary
     */
    suspend fun deleteChatNode(chatNode: ChatNode) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                var parent: ChatNode? = null
                if (chatNode.parentInitialized()) {
                    parent = chatNode.parent
                    parent.children.remove(chatNode)
                    if (parent.children.size == 0) {
                        parent.activeChildIndex = 0
                    } else if (parent.activeChildIndex > parent.children.lastIndex) {
                        parent.activeChildIndex = parent.children.lastIndex
                    }
                }
                chatNodeDao.delete(chatNode)
                if (parent != null) {
                    chatNodeDao.update(parent)
                }
            }
        }
    }

    suspend fun saveGptSettings(gptSettings: GPTSettings) {
        withContext(Dispatchers.IO) {
            gptSettingsDao.upsert(gptSettings)
        }
    }



    /**
     * Loads all children for entire branch and assigns parent as well
     */
    suspend fun loadChildren(chatNode: ChatNode, chatTree: ChatTree? = null) {
        withContext(Dispatchers.IO) {
            suspend fun processChildren(node: ChatNode) {
                val children = chatNodeDao.getChildrenOfChatNode(node.chatTreeId, node.id)
                node.children = children
                children.forEach { child ->
                    child.parent = node
                    child.chatTree = chatTree
                    processChildren(child)
                }
            }
            processChildren(chatNode)
        }
    }

    /**
     * The following non-suspension methods are helper functions to manage the entities after
     * the relationship models have been attached to them
     */

    /**
     * Crawls down active chatNodes till it reaches a leaf and returns it
     */
    fun getActiveLeaf(chatNode: ChatNode): ChatNode {
        var currentNode = chatNode
        while (currentNode.children.isNotEmpty()) {
            currentNode = currentNode.children[currentNode.activeChildIndex]
        }
        return currentNode
    }

    /**
     * Returns a flat list of chatNodes starting from the one provided and going till it's tail
     */
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

    /**
     * This returns chat list formatted with roles and messages for API calls
     */
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