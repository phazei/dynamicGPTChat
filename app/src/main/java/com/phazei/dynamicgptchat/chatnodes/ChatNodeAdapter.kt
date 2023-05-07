package com.phazei.dynamicgptchat.chatnodes

import android.annotation.SuppressLint
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.databinding.ChatNodeHeaderItemBinding
import com.phazei.dynamicgptchat.databinding.ChatNodeItemBinding
import java.security.InvalidParameterException

@Suppress("unused")
class ChatNodeAdapter(
    private val chatNodes: MutableList<ChatNode>,
    private val nodeActionListener: OnNodeActionListener,
) : RecyclerView.Adapter<ChatNodeAdapter.ChatNodeViewHolder>() {
    // helper method for ease of access scrolling
    lateinit var layoutManager: LinearLayoutManager

    private var previousActiveNodePosition: Int? = null
    var activeNodePosition: Int? = null
    var isEditingActive = false

    private val editedData: MutableMap<String, String> = mutableMapOf()

    companion object {
        const val ITEM_TYPE_ACTIVE = 69
    }

    private lateinit var drawableMenuToClose: AnimatedVectorDrawable
    private lateinit var drawableCloseToMenu: AnimatedVectorDrawable
    // private lateinit var knightriderWaiting: AnimatedVectorDrawable

    init {
        setHasStableIds(true)
    }
    override fun getItemId(position: Int): Long {
        return chatNodes[position].id
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == activeNodePosition) {
            ITEM_TYPE_ACTIVE
        } else {
            super.getItemViewType(position) // This will return the default viewType, which is 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatNodeViewHolder {
        val binding =
            ChatNodeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        // knightriderWaiting = ContextCompat.getDrawable(parent.context, R.drawable.knightrider) as AnimatedVectorDrawable
        // binding.responseWaiting.setImageDrawable(knightriderWaiting)
        // knightriderWaiting.start()
        if (!::drawableMenuToClose.isInitialized) {
            drawableMenuToClose = ContextCompat.getDrawable(parent.context, R.drawable.avd_menu_to_close) as AnimatedVectorDrawable
        }
        if (!::drawableCloseToMenu.isInitialized) {
            drawableCloseToMenu = ContextCompat.getDrawable(parent.context, R.drawable.avd_close_to_menu) as AnimatedVectorDrawable
        }

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
            // listeners that don't need chatNode, like displaying extra menus
            binding.nodeMenuButton.setOnClickListener {
                // once node is made active, it's type becomes ITEM_TYPE_ACTIVE
                // after notification of item change, bind is called again
                // it's recreated with a new viewHolder, which is what the popup menu needs
                // to attach to

                //only needed for animated button x->menu
                previousActiveNodePosition = activeNodePosition

                if (activeNodePosition == bindingAdapterPosition) {
                    //disable it after clicking a second time
                    activeNodePosition = null
                    notifyItemChanged(bindingAdapterPosition)
                    nodeActionListener.onNodeSelected(bindingAdapterPosition)
                } else {
                    activeNodePosition = bindingAdapterPosition
                    notifyItemChanged(bindingAdapterPosition)
                }
            }

            binding.promptTextView.doOnTextChanged { text, _, _, _ ->
                if (bindingAdapterPosition == activeNodePosition) {
                    editedData["prompt$bindingAdapterPosition"] = text.toString()
                }
            }
            binding.responseTextView.doOnTextChanged { text, _, _, _ ->
                if (bindingAdapterPosition == activeNodePosition) {
                    editedData["response$bindingAdapterPosition"] = text.toString()
                }
            }
        }

        fun bind(chatNode: ChatNode) {
            // hide the root node
            if (chatNode.parentNodeId == null) {
                binding.root.layoutParams.height = 0
                binding.root.visibility = View.GONE
                return
            } else {
                binding.root.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.root.visibility = View.VISIBLE
            }

            if (bindingAdapterPosition == activeNodePosition) {
                binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    // this is called after view is complete and attached
                    override fun onGlobalLayout() {
                        // remove listener and call popup
                        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        nodeActionListener.onNodeSelected(bindingAdapterPosition)
                    }
                })

                binding.nodeMenuButton.setImageDrawable(drawableMenuToClose)
                drawableMenuToClose.start()
            }

            if (bindingAdapterPosition == previousActiveNodePosition) {
                binding.nodeMenuButton.setImageDrawable(drawableCloseToMenu)
                drawableCloseToMenu.start()
                previousActiveNodePosition = null
            }

            if (bindingAdapterPosition == activeNodePosition && isEditingActive) {
                binding.promptTextView.setText(editedData["prompt$bindingAdapterPosition"] ?: chatNode.prompt)
                binding.responseTextView.setText(editedData["response$bindingAdapterPosition"] ?: chatNode.response)
            } else {
                binding.promptTextView.setText(chatNode.prompt)
                binding.responseTextView.setText(chatNode.response)
            }

            // show error
            if (chatNode.error?.isEmpty() == false) {
                binding.errorTextView.visibility = View.VISIBLE
                binding.errorTextView.text = chatNode.error
            } else {
                binding.errorTextView.visibility = View.GONE
                binding.errorTextView.text = ""
            }

            // show moderation
            if (chatNode.moderation?.isEmpty() == false) {
                binding.moderationTextView.visibility = View.VISIBLE
                binding.moderationTextView.text = chatNode.error
            } else {
                binding.moderationTextView.visibility = View.GONE
                binding.moderationTextView.text = ""
            }

        }

        fun enableEdit() {
            if (bindingAdapterPosition == activeNodePosition) {
                isEditingActive = true
            }
            binding.promptTextView.apply {
                setBackgroundResource(R.drawable.message_editable_bg)
                isEnabled = true
            }
            binding.responseTextView.apply {
                setBackgroundResource(R.drawable.message_editable_bg)
                isEnabled = true
            }

        }

        fun disableEdit() {
            if (activeNodePosition == null || bindingAdapterPosition == activeNodePosition) {
                isEditingActive = false
            }
            binding.promptTextView.apply {
                background = null
                isEnabled = false
            }
            binding.responseTextView.apply {
                background = null
                isEnabled = false
            }
            editedData.clear()
            editedData.clear()

            notifyItemChanged(bindingAdapterPosition)
        }

        /**
         * It's possible the deactivated binding is reused with the new active binding
         * Be certain it's not called if the view is reused
         * BindingAdapter position might change
         */
        fun deactivate() {
            editedData.clear()
            editedData.clear()

            if (activeNodePosition != null && bindingAdapterPosition == activeNodePosition) {
                val oldActiveNodePosition = activeNodePosition ?: -1
                activeNodePosition = null
                notifyItemChanged(oldActiveNodePosition)
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
            // in case it was lost from the adapter but had then been saved.
            addItem(chatNode.parent, chatNode)
        } else {
            // there's a chance that the memory reference has been lost if the fragment
            // was navigated away from.  this will make sure it's updated
            chatNodes[index] = chatNode
            notifyItemChanged(index)
        }
    }

    fun cancelLastItem() {
        val lastChatNode = chatNodes[chatNodes.lastIndex]
        if (lastChatNode.id == 0L) {
            // TODO:
            // it has not been saved, so can remove it
            //
            // flow has changed, might not need to do anything, just show it with empty response
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
                    // reset updated to current
                    updatedSystemMessage = currentSystemMessage
                    systemMessageEditText.setText(currentSystemMessage)
                }

                editSystemMessageSubmitButton.setOnClickListener {
                    updatedSystemMessage = systemMessageEditText.text.toString()
                    onSave(updatedSystemMessage)
                    // update current to updated
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
        val binding = ChatNodeHeaderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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