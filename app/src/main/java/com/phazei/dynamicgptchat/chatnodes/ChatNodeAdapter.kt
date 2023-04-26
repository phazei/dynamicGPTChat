package com.phazei.dynamicgptchat.chatnodes

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.databinding.ChatNodeHeaderItemBinding
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

            //hide the root node
            if (chatNode.parentNodeId == null) {
                binding.root.layoutParams.height = 0
                binding.root.visibility = View.GONE
            } else {
                binding.root.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.root.visibility = View.VISIBLE
            }

            binding.promptTextView.setText(chatNode.prompt)
            binding.responseTextView.text = chatNode.response

            //show error
            if (chatNode.error?.isEmpty() == false) {
                binding.errorTextView.visibility = View.VISIBLE
                binding.errorTextView.text = chatNode.error
            } else {
                binding.errorTextView.visibility = View.GONE
                binding.errorTextView.text = ""
            }

            //show moderation
            if (chatNode.moderation?.isEmpty() == false) {
                binding.moderationTextView.visibility = View.VISIBLE
                binding.moderationTextView.text = chatNode.error
            } else {
                binding.moderationTextView.visibility = View.GONE
                binding.moderationTextView.text = ""
            }

            binding.root.setOnClickListener {
                onChatNodeClick(chatNode)
            }

            binding.editPromptSubmitButton.setOnClickListener {
                // onEditPromptClick(chatNode, binding.promptTextView.text.toString())
            }

            binding.promptTextView.setOnFocusChangeListener { _, hasFocus ->
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
        // Calculate the number of items that will be removed
        // Remove any items after the new node
        val itemsToRemove = chatNodes.size - (parentIndex + 2)
        if (itemsToRemove > 0) {
            chatNodes.subList(parentIndex + 2, chatNodes.size).clear()
            notifyItemRangeRemoved(parentIndex + 2, itemsToRemove)
        }
        // Notify the recycler view that items have changed
        notifyItemInserted(parentIndex + 1)
        // notifyItemRangeInserted(parentIndex + 1, 1)
    }

    fun updateItem(chatNode: ChatNode) {
        val index = chatNodes.indexOf(chatNode)
        if (index == -1) {
            //in case it was lost from the adapter but had then been saved.
            addItem(chatNode.parent, chatNode)
        } else {
            //there's a chance that the memory reference has been lost if the fragment
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

class ChatNodeHeaderAdapter(
    private var currentSystemMessage: String,
    private val onSave: (newSystemMessage: String) -> Unit,
    private val onChange: (sysMsgheight: Int) -> Unit
) : RecyclerView.Adapter<ChatNodeHeaderAdapter.HeaderViewHolder>() {

    private var updatedSystemMessage: String = currentSystemMessage

    inner class HeaderViewHolder(val binding: ChatNodeHeaderItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ChatNodeHeaderItemBinding.inflate(inflater, parent, false)
        return HeaderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {


        fun checkSystemMessageChanged() {
            holder.binding.apply {
                val hasChanges = updatedSystemMessage != currentSystemMessage
                editSystemMessageCancelButton.visibility = if (hasChanges) View.VISIBLE else View.GONE
                editSystemMessageSubmitButton.visibility = if (hasChanges) View.VISIBLE else View.GONE
            }
        }

        holder.binding.apply {

            systemMessageEditText.doBeforeTextChanged { text, start, count, after ->
                onChange(holder.binding.root.height)
            }

            systemMessageEditText.doAfterTextChanged { _ ->
                updatedSystemMessage = systemMessageEditText.text.toString()
                checkSystemMessageChanged()
            }

            systemMessageEditText.setText(updatedSystemMessage)

            editSystemMessageCancelButton.setOnClickListener {
                //reset updated to current
                updatedSystemMessage = currentSystemMessage
                systemMessageEditText.setText(currentSystemMessage)
            }

            editSystemMessageSubmitButton.setOnClickListener {
                updatedSystemMessage = systemMessageEditText.text.toString()
                onSave(updatedSystemMessage)
                //update current to updated
                currentSystemMessage = updatedSystemMessage
                checkSystemMessageChanged()
            }
        }
    }

    override fun getItemCount(): Int = 1
}