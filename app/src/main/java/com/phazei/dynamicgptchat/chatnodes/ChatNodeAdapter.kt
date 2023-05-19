package com.phazei.dynamicgptchat.chatnodes

import android.annotation.SuppressLint
import android.graphics.drawable.AnimatedVectorDrawable
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.data.entity.PromptWithTags
import com.phazei.dynamicgptchat.databinding.ChatNodeHeaderItemBinding
import com.phazei.dynamicgptchat.databinding.ChatNodeItemBinding
import com.phazei.dynamicgptchat.prompts.PromptListAdapter
import com.phazei.dynamicgptchat.prompts.PromptsListFragment
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
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

    private lateinit var markwon: Markwon
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

    override fun getItemCount(): Int {
        return chatNodes.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatNodeViewHolder {
        val binding =
            ChatNodeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        if (!::markwon.isInitialized) {
            markwon = Markwon.builder(parent.context)
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(ImagesPlugin.create())
                .usePlugin(GlideImagesPlugin.create(parent.context))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(parent.context))
                .usePlugin(MovementMethodPlugin.create(TableAwareMovementMethod.create()))
                .build()
        }

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

    @Suppress("RedundantEmptyInitializerBlock")
    inner class ChatNodeViewHolder(val binding: ChatNodeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Displays popup menu
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
                    nodeActionListener.onNodeSelected()
                } else {
                    activeNodePosition = bindingAdapterPosition
                    notifyItemChanged(bindingAdapterPosition)
                }
            }

            binding.promptTextEdit.doOnTextChanged { text, _, _, _ ->
                if (bindingAdapterPosition == activeNodePosition) {
                    editedData["prompt$bindingAdapterPosition"] = text.toString()
                }
            }
            binding.responseTextEdit.doOnTextChanged { text, _, _, _ ->
                if (bindingAdapterPosition == activeNodePosition) {
                    editedData["response$bindingAdapterPosition"] = text.toString()
                }
            }
        }

        @SuppressLint("SetTextI18n")
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
                        nodeActionListener.onNodeSelected()
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

            if (bindingAdapterPosition == activeNodePosition) {
                binding.promptTextEdit.setText(editedData["prompt$bindingAdapterPosition"] ?: chatNode.prompt)
                binding.responseTextEdit.setText(editedData["response$bindingAdapterPosition"] ?: chatNode.response)
            }

            //view text
            binding.promptTextView.text = chatNode.prompt
            markwon.setMarkdown(binding.responseTextView, chatNode.response)
            binding.responseTextView.movementMethod = LinkMovementMethod.getInstance()
            //horizontally scrolling textViews have terrible functionality compared to <HorizontalScrollView>
            binding.responseTextView.setHorizontallyScrolling(false)


            binding.nodeIndexCount.text = "${chatNode.parent.children.indexOf(chatNode)+1}/${chatNode.parent.children.size}"


            // show error
            if (!chatNode.error.isNullOrBlank()) {
                binding.errorTextView.visibility = View.VISIBLE
                binding.errorTextView.text = chatNode.error
            } else {
                binding.errorTextView.visibility = View.GONE
                binding.errorTextView.text = ""
            }

            // show moderation
            if (!chatNode.moderation.isNullOrBlank()) {
                binding.moderationTextView.visibility = View.VISIBLE
                binding.moderationTextView.text = chatNode.moderation
            } else {
                binding.moderationTextView.visibility = View.GONE
                binding.moderationTextView.text = ""
            }

        }

        fun enableEdit() {
            isEditingActive = true

            binding.promptTextEdit.visibility = View.VISIBLE
            binding.promptTextView.visibility = View.GONE

            binding.responseTextEdit.visibility = View.VISIBLE
            binding.responseTextView.visibility = View.GONE
        }

        fun disableEdit(notify: Boolean = false) {
            isEditingActive = false

            binding.promptTextEdit.visibility = View.GONE
            binding.promptTextView.visibility = View.VISIBLE

            binding.responseTextEdit.visibility = View.GONE
            binding.responseTextView.visibility = View.VISIBLE

            editedData.clear()

            if (notify) {
                activeNodePosition?.let { pos ->
                    notifyItemChanged(pos)
                }
            }
        }

        /**
         * It's possible the deactivated binding is reused with the new active binding
         * Be certain it's not called if the view is reused
         * BindingAdapter position might change
         */
        fun deactivate(force: Boolean = false) {
            editedData.clear()

            if (force || (activeNodePosition != null && bindingAdapterPosition == activeNodePosition)) {
                val oldActiveNodePosition = activeNodePosition ?: -1
                activeNodePosition = null
                notifyItemChanged(oldActiveNodePosition)
            }
        }
    }

    fun getItemPosition(chatNode: ChatNode): Int {
        return chatNodes.indexOf(chatNode)
    }

    fun getItem(position: Int): ChatNode {
        return chatNodes[position]
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
        // Notify the recycler view that items have changed
        notifyItemInserted(parentIndex + 1)
        // notifyItemRangeInserted(parentIndex + 1, 1)


        // Calculate the number of items that will be removed
        // Remove any items after the new node
        val itemsToRemove = chatNodes.size - (parentIndex + 2)
        if (itemsToRemove > 0) {
            chatNodes.subList(parentIndex + 2, chatNodes.size).clear()
            notifyItemRangeRemoved(parentIndex + 2, itemsToRemove)
        }
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

    // flow has changed, might not need to do anything, just show it with empty response
    fun cancelLastItem() {
        val lastChatNode = chatNodes[chatNodes.lastIndex]
        if (lastChatNode.id == 0L) {
        }
    }

    fun isInit(): Boolean {
        return !chatNodes.isEmpty()
    }

    interface OnNodeActionListener {
        fun onNodeSelected()
        fun onEditNode(position: Int)
        // Add other actions as needed
    }

}

class ChatNodeHeaderAdapter(
    private val fragmentManager: FragmentManager,
    private var currentSystemMessage: String,
    private val onSave: (newSystemMessage: String) -> Unit,
    private val onChange: (sysMsgHeight: Int) -> Unit
) : RecyclerView.Adapter<ChatNodeHeaderAdapter.HeaderViewHolder>() {
    private var updatedSystemMessage: String = currentSystemMessage

    init { setHasStableIds(true) }
    override fun getItemId(position: Int): Long { return -10 }

    inner class HeaderViewHolder(val binding: ChatNodeHeaderItemBinding) : RecyclerView.ViewHolder(binding.root), PromptsListFragment.OnPromptSelectedListener {
        private val promptSearchDialog = PromptSearchDialog(this@HeaderViewHolder)

        init {
            binding.apply {
                systemMessageEditText.doBeforeTextChanged { text, start, count, after ->
                    onChange(binding.root.height)
                }

                systemMessageEditText.doAfterTextChanged { _ ->
                    updatedSystemMessage = systemMessageEditText.text.toString()
                    updateSystemMessageButtons()
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
                    updateSystemMessageButtons()
                }

                systemMessageEditText.setOnFocusChangeListener { _, hasFocus ->
                    updateSystemMessageButtons()
                }

                systemMessageInsertPromptButton.setOnClickListener {
                    //open dialog
                    promptSearchDialog.show(fragmentManager, "promptSearchDialog")
                }
            }
        }

        private fun updateSystemMessageButtons() {
            binding.apply {
                val textFocused = systemMessageEditText.hasFocus()
                val hasChanges = updatedSystemMessage != currentSystemMessage
                if (hasChanges || textFocused) {
                    binding.systemMessageButtons.visibility = View.VISIBLE

                    editSystemMessageCancelButton.visibility = if (hasChanges) View.VISIBLE else View.GONE
                    editSystemMessageSubmitButton.visibility = if (hasChanges) View.VISIBLE else View.GONE

                    systemMessageInsertPromptButton.visibility = if (!hasChanges && textFocused) View.VISIBLE else View.GONE
                } else {
                    binding.systemMessageButtons.visibility = View.GONE
                }
            }
        }

        // @PromptListFragment
        override val listType: PromptListAdapter.ListType = PromptListAdapter.ListType.SELECT
        override fun onPromptSelected(promptWithTags: PromptWithTags) {
            binding.systemMessageEditText.setText(promptWithTags.prompt.body)
            promptSearchDialog.dismiss()
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