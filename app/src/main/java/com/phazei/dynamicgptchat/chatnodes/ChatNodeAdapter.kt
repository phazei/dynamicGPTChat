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
    private val nodeActionListener: OnNodeActionListener,
) : RecyclerView.Adapter<ChatNodeAdapter.ChatNodeViewHolder>() {
    //helper method for ease of access scrolling
    lateinit var layoutManager: LinearLayoutManager
    lateinit var activeNodePool: ActiveNodeRecycledViewPool

    var activeNodePosition: Int? = null

    // private lateinit var knightriderWaiting: AnimatedVectorDrawable

    init {
        setHasStableIds(true)
    }
    override fun getItemId(position: Int): Long {
        Log.d("TAG", "getItemId pos $position")
        return chatNodes[position].id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatNodeViewHolder {

        if (viewType >= ActiveNodeRecycledViewPool.typeOffset.toInt()) {
            val viewHolder = activeNodePool.getRecycledView(viewType)
            if (viewHolder != null) {
                Log.d("TAG", "CREATED TYPE $viewType")
                return viewHolder as ChatNodeViewHolder
            } else {
                Log.d("TAG", "FAILED CREATED TYPE $viewType")
            }
        }

        val binding =
            ChatNodeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        // knightriderWaiting = ContextCompat.getDrawable(parent.context, R.drawable.knightrider) as AnimatedVectorDrawable
        // binding.responseWaiting.setImageDrawable(knightriderWaiting)
        // knightriderWaiting.start()

        return ChatNodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatNodeViewHolder, position: Int) {
        holder.bind(chatNodes[position])
    }

    override fun getItemCount(): Int {
        return chatNodes.size
    }


    @Suppress("RedundantEmptyInitializerBlock")
    inner class ChatNodeViewHolder(val binding: ChatNodeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            //listeners that don't need chatNode, like displaying extra menus
            binding.nodeMenuButton.setOnClickListener {
                activeNodePosition = bindingAdapterPosition
                activeNodePool.setActiveNodeViewHolder(this, chatNodes[bindingAdapterPosition].id)

                nodeActionListener.onNodeSelected(absoluteAdapterPosition)
            }
        }

        fun bind(chatNode: ChatNode) {

            binding.testText.text = bindingAdapterPosition.toString() + " " + chatNodes[bindingAdapterPosition].id.toString()

            if (bindingAdapterPosition == activeNodePosition) {
                return
            }

            //hide the root node
            if (chatNode.parentNodeId == null) {
                binding.root.layoutParams.height = 0
                binding.root.visibility = View.GONE
            } else {
                binding.root.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.root.visibility = View.VISIBLE
            }

            binding.promptTextView.setText(chatNode.prompt)
            binding.responseTextView.setText(chatNode.response)

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


            // binding.nodeMenuButton.setOnClickListener {
            //     Log.d("TAG", "inside bind $bindingAdapterPosition")
            // }

            // binding.root.setOnClickListener {
            //     // onChatNodeClick(chatNode)
            // }
            //
            // binding.editPromptSubmitButton.setOnClickListener {
            //     // onEditPromptClick(chatNode, binding.promptTextView.text.toString())
            // }
            //
            // binding.promptTextView.setOnFocusChangeListener { _, hasFocus ->
            // }

        }
    }

    override fun getItemViewType(position: Int): Int {
        val id = (chatNodes[position].id + ActiveNodeRecycledViewPool.typeOffset).toInt()
        Log.d("TAG", "ItemViewTypeForPOS: $position  /  ViewTypeID: $id")
        return if (position == activeNodePosition) {
            id
        } else {
            super.getItemViewType(position) // This will return the default viewType, which is 0
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
            //
            //flow has changed, might not need to do anything, just show it with empty response
        }
    }

    fun isInit(): Boolean {
        return !chatNodes.isEmpty()
    }

    interface OnNodeActionListener {
        fun onNodeSelected(position: Int)
        fun onEditNode(position: Int)
        // Add other actions as needed
    }

}

class ChatNodeHeaderAdapter(
    private var currentSystemMessage: String,
    private val onSave: (newSystemMessage: String) -> Unit,
    private val onChange: (sysMsgHeight: Int) -> Unit
) : RecyclerView.Adapter<ChatNodeHeaderAdapter.HeaderViewHolder>() {
    private var updatedSystemMessage: String = currentSystemMessage

    init { setHasStableIds(true) }
    override fun getItemId(position: Int): Long { return -10 }

    inner class HeaderViewHolder(val binding: ChatNodeHeaderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                systemMessageEditText.doBeforeTextChanged { text, start, count, after ->
                    onChange(binding.root.height)
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

        private fun checkSystemMessageChanged() {
            binding.apply {
                val hasChanges = updatedSystemMessage != currentSystemMessage
                editSystemMessageCancelButton.visibility = if (hasChanges) View.VISIBLE else View.GONE
                editSystemMessageSubmitButton.visibility = if (hasChanges) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ChatNodeHeaderItemBinding.inflate(inflater, parent, false)
        return HeaderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {}

    override fun getItemCount(): Int = 1
}

/**
 * this exists only to create padding at the bottom when the input is hidden
 */
class ChatNodeFooterAdapter(var footerHeight: Int = 0) : RecyclerView.Adapter<ChatNodeFooterAdapter.FooterViewHolder>() {
    init { setHasStableIds(true) }
    override fun getItemId(position: Int): Long { return -11 }

    inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val view = View(parent.context)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            footerHeight
        )
        return FooterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {
        holder.itemView.layoutParams.height = footerHeight
    }

    override fun getItemCount(): Int = 1

    fun updateFooterHeight(height: Int): Boolean {
        val changed = footerHeight != height
        footerHeight = height
        if (changed)
            notifyItemChanged(0)
        return changed
    }
}


/**
 * This will let an active chat node that could be being edited exist in it's own pool of views
 * so it will not be recycled with the rest of the views and can be reused.
 *
 * This uses the itemId to identify the item as a "type" so it's mandatory that setHasStableIds(true)
 */
class ActiveNodeRecycledViewPool : RecyclerView.RecycledViewPool() {
    private var activeNodeViewHolder: MutableMap<Long, RecyclerView.ViewHolder> = mutableMapOf()
    private var activeItemList: MutableList<Long> = mutableListOf()
    companion object {
        //need offset so it doesn't conflict with default items 0, 1 ,2  from ConcatAdapter
        const val typeOffset = 10L
    }

    override fun getRecycledView(viewType: Int): RecyclerView.ViewHolder? {
        return if (activeNodeViewHolder.containsKey(viewType.toLong())) {
            Log.d("TAG", "FOUND  ${viewType.toLong()}")
            activeNodeViewHolder[viewType.toLong()]
        } else {
            super.getRecycledView(viewType)
        }
    }

    override fun putRecycledView(viewHolder: RecyclerView.ViewHolder) {
        if (activeNodeViewHolder.contains(viewHolder.itemId+typeOffset)) {
            Log.d("TAG", "PUT ${viewHolder.itemId+typeOffset}")
            activeNodeViewHolder[viewHolder.itemId+typeOffset] = viewHolder
        } else {
            super.putRecycledView(viewHolder)
        }
    }

    fun setActiveNodeViewHolder(view: RecyclerView.ViewHolder, itemId: Long) {
        Log.d("TAG", "SET ${itemId+typeOffset}")
        activeItemList.add(itemId+typeOffset)
        activeNodeViewHolder[itemId+typeOffset] = view
    }
}