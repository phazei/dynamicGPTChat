package com.phazei.dynamicgptchat.chatnodes

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.databinding.ChatNodeItemBinding
import java.security.InvalidParameterException

@Suppress("unused")
class ChatNodeAdapter(
    private val chatNodes: MutableList<ChatNode>,
    private val onChatNodeClick: (ChatNode) -> Unit,
    private val onEditPromptClick: (ChatNode, String) -> Unit
) : RecyclerView.Adapter<ChatNodeAdapter.ChatNodeViewHolder>() {
    //helper method for ease of access scrolling
    lateinit var layoutManager: LinearLayoutManager

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatNodeViewHolder {
        val binding =
            ChatNodeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ChatNodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatNodeViewHolder, position: Int) {
        holder.bind(chatNodes[position])
    }

    override fun getItemCount(): Int {
        return chatNodes.size
    }

    @Suppress("RedundantEmptyInitializerBlock")
    inner class ChatNodeViewHolder(private val binding: ChatNodeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            //listeners that don't need chatNode, like displaying extra menus
        }

        fun bind(chatNode: ChatNode) {
            binding.promptTextView.text = chatNode.prompt
            binding.responseTextView.text = chatNode.response

            if (chatNode.error?.isEmpty() == false) {
                binding.chatErrorCard.visibility = View.VISIBLE
                binding.errorTextView.text = chatNode.error
            } else {
                binding.chatErrorCard.visibility = View.GONE
                binding.errorTextView.text = ""
            }

            binding.root.setOnClickListener {
                onChatNodeClick(chatNode)
            }

            binding.editPromptSubmitButton.setOnClickListener {
                onEditPromptClick(chatNode, binding.editPromptInput.text.toString())
            }


        }
    }

    fun getItemPosition(chatNode: ChatNode): Int {
        return chatNodes.indexOf(chatNode)
    }

    /**
     * Replaces entire or partial branch of active chatNodes
     * chatNode should be first item in newChatNodes list
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(chatNode: ChatNode, newChatNodes: List<ChatNode>) {
        if (chatNode.parentNodeId == null) { // this must be the root node, clear and load all
            chatNodes.clear()
            chatNodes.addAll(newChatNodes)
            notifyDataSetChanged()
        } else {
            val index = chatNodes.indexOf(chatNode)
            if (index != -1) {
                // Remove the old nodes from the current index to the end of the list
                val nodesToRemove = chatNodes.size - index
                chatNodes.subList(index, chatNodes.size).clear()

                // Add the new nodes starting from the current index
                chatNodes.addAll(index, newChatNodes)

                // Notify the adapter about the change
                notifyItemRangeRemoved(index, nodesToRemove)
                notifyItemRangeInserted(index, newChatNodes.size)
            } else {
                Log.e("ChatNodeAdapter", "updateData: chatNode not found in the list")
                throw InvalidParameterException("Unable to find nonRoot node in current active branch")
            }
        }
    }

    /**
     * This adds an item to a specific node in the activeTree list where ever it is
     * After it's added, it invalidates all nodes afterwards so it removes them
     */
    fun addItem(parentNode: ChatNode, newNode: ChatNode) {
        val parentIndex = chatNodes.indexOf(parentNode)
        if (parentIndex == -1) {
            throw IllegalArgumentException("Parent node not found in list.")
        }
        // Insert the new node after the parent node
        chatNodes.add(parentIndex + 1, newNode)
        // Remove any items after the new node
        if (parentIndex + 2 < chatNodes.size) {
            chatNodes.subList(parentIndex + 2, chatNodes.size).clear()
        }
        // Notify the recycler view that items have changed
        notifyItemRangeChanged(parentIndex + 1, chatNodes.size - parentIndex - 1)
    }

    fun updateItem(chatNode: ChatNode) {
        val index = chatNodes.indexOf(chatNode)
        if (index == -1) {
            //in case it was lost from the adapter but had then ben saved.
            addItem(chatNode.parent, chatNode)
        } else {
            //there's a chance that the memory reference has been lost of the fragment
            //was navigated away from.  this will make sure it's updated
            chatNodes[index] = chatNode
            notifyItemChanged(index)
        }
    }

    fun cancelLastItem() {
        val lastChatNode = chatNodes[chatNodes.lastIndex]
        if (lastChatNode.id == 0L) {
            //TODO:
            //it has not been saved, so can remove it
        }
    }

    fun isInit(): Boolean {
        return !chatNodes.isEmpty()
    }

}
