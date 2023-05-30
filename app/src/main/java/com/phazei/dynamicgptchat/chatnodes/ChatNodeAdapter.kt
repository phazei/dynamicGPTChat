package com.phazei.dynamicgptchat.chatnodes

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.data.pojo.ChatTreeOptions
import com.phazei.dynamicgptchat.data.pojo.PromptWithTags
import com.phazei.dynamicgptchat.databinding.ChatNodeHeaderItemBinding
import com.phazei.dynamicgptchat.databinding.ChatNodeItemBinding
import com.phazei.dynamicgptchat.prompts.PromptListAdapter
import com.phazei.dynamicgptchat.prompts.PromptsListFragment
import com.phazei.utils.Solacon
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
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

    var tracker: SelectionTracker<Long>? = null

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
                .usePlugin(SoftBreakAddsNewLinePlugin.create())
                .usePlugin(HtmlPlugin.create())
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


    @SuppressLint("ClickableViewAccessibility")
    inner class ChatNodeViewHolder(val binding: ChatNodeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            nodeActionListener.isActiveRequest.observe(nodeActionListener.isActiveRequestLifecycleOwner) { isActiveRequest ->
                // better to listen and update button than to notifyDataSetChanged
                nodeMenuButtonEnable(!isActiveRequest)
            }

            // Displays popup menu
            binding.nodeMenuButton.setOnClickListener {
                if (nodeActionListener.isActiveRequest.value != null && nodeActionListener.isActiveRequest.value == true) {
                    // can't open menu when active request going on
                    return@setOnClickListener
                }

                // once node is made active, it's type becomes ITEM_TYPE_ACTIVE
                // after notification of item change, bind is called again
                // it's recreated with a new viewHolder, which is what the popup menu needs
                // to attach to

                // only needed for animated button x->menu
                previousActiveNodePosition = activeNodePosition

                if (activeNodePosition == bindingAdapterPosition) {
                    // disable it after clicking a second time
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

            binding.errorTextView.setOnLongClickListener { _ ->
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("ChatError", getItem(bindingAdapterPosition).error)
                clipboard.setPrimaryClip(clipData)
                Snackbar.make(binding.root, "Copied error to clipboard", Snackbar.LENGTH_SHORT).show()
                false
            }
            binding.errorTextView.setOnLongClickListener { _ ->
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("ChatModeration", getItem(bindingAdapterPosition).moderation)
                clipboard.setPrimaryClip(clipData)
                Snackbar.make(binding.root, "Copied moderation to clipboard", Snackbar.LENGTH_SHORT).show()
                false
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

            nodeActionListener.isActiveRequest.value?.let { isActiveRequest ->
                nodeMenuButtonEnable(!isActiveRequest)
            }

            /**
             * Setup all the view texts
             */
            binding.promptTextView.text = chatNode.prompt
            // unable to wrap text around image, so make some space with spaces on first line alone
            // (had attempted with LeadingMarginSpan.LeadingMarginSpan2 but it affects all lines)
            markwon.setMarkdown(binding.responseTextView, "\u00A0".repeat(8) + chatNode.response)
            binding.responseTextView.movementMethod = LinkMovementMethod.getInstance()

            adjustResponseWrap(chatNode)

            binding.responseProfileIcon.setImageBitmap(Solacon.generateBitmap(chatNode.model, 256))
            binding.responseProfileIcon.contentDescription = "Model: ${chatNode.model}"

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

            if (tracker != null) {
                markSelectedState()
            }
        }

        private fun markSelectedState() {
            tracker?.let {
                itemView.isActivated = it.isSelected(itemId)
                if (it.selection.size() > 0) {
                    binding.menuButtonGroup.visibility = View.GONE
                    binding.chatNodeSelected.visibility = if (itemView.isActivated) View.VISIBLE else View.GONE
                } else {
                    binding.menuButtonGroup.visibility = View.VISIBLE
                    binding.chatNodeSelected.visibility = View.GONE
                }
            }
        }

        private fun nodeMenuButtonEnable(isEnabled: Boolean) {
            binding.nodeMenuButton.isEnabled = isEnabled
            binding.nodeMenuButton.isClickable = isEnabled
            if (isEnabled) {
                binding.nodeMenuButton.backgroundTintList = ContextCompat.getColorStateList(this.itemView.context, R.color.md_theme_dark_primary)
            } else {
                binding.nodeMenuButton.backgroundTintList = ContextCompat.getColorStateList(this.itemView.context, R.color.gray)
            }
        }

        private fun adjustResponseWrap(chatNode: ChatNode) {
            val minWidth = 500
            when (chatNode.chatTree?.options?.responseWrap) {
                ChatTreeOptions.Wrap.CUSTOM -> {
                    binding.responseTextView.maxWidth = chatNode.chatTree?.options?.responseWrapSize?.coerceAtLeast(minWidth) ?: minWidth
                }
                ChatTreeOptions.Wrap.NOWRAP -> {
                    binding.responseTextView.maxWidth = Int.MAX_VALUE
                }
                else -> {
                    if (binding.responseHorizontalScroll.width > 0) {
                        // it's already been laid out
                        binding.responseTextView.maxWidth = binding.responseHorizontalScroll.width.coerceAtLeast(minWidth)
                    } else {
                        val tempWidth = (itemView.context.resources.displayMetrics.widthPixels * 0.85).toInt()
                        binding.responseTextView.maxWidth = tempWidth.coerceAtLeast(minWidth)
                        val vto = binding.responseHorizontalScroll.viewTreeObserver
                        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                binding.responseHorizontalScroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                binding.responseTextView.maxWidth = binding.responseHorizontalScroll.width.coerceAtLeast(minWidth)
                            }
                        })
                    }
                }
            }
        }

        fun enableEdit() {
            isEditingActive = true

            binding.promptTextEdit.visibility = View.VISIBLE
            binding.promptTextView.visibility = View.GONE

            binding.responseTextEdit.visibility = View.VISIBLE
            binding.responseTextView.visibility = View.GONE

            binding.responseProfileIcon.alpha = 0.3F
        }

        fun disableEdit(notify: Boolean = false) {
            isEditingActive = false

            binding.promptTextEdit.visibility = View.GONE
            binding.promptTextView.visibility = View.VISIBLE

            binding.responseTextEdit.visibility = View.GONE
            binding.responseTextView.visibility = View.VISIBLE

            binding.responseProfileIcon.alpha = 1F

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
                previousActiveNodePosition = activeNodePosition
                activeNodePosition = null
                notifyItemChanged(oldActiveNodePosition)
            }
        }

        fun getItemDetails() = object : ItemDetailsLookup.ItemDetails<Long>() {
            override fun getPosition(): Int { return absoluteAdapterPosition }
            override fun getSelectionKey(): Long { return itemId }
        }
    }

    fun getItemPosition(chatNode: ChatNode): Int {
        return chatNodes.indexOf(chatNode)
    }

    fun getItem(position: Int): ChatNode {
        return chatNodes[position]
    }

    fun getAllItems(): List<ChatNode> {
        return chatNodes.toList()
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

    fun isInit(): Boolean {
        return chatNodes.isNotEmpty()
    }

    interface OnNodeActionListener {
        val isActiveRequest: LiveData<Boolean>
        val isActiveRequestLifecycleOwner: LifecycleOwner
        fun onNodeSelected()
        fun onEditNode(position: Int)
        // Add other actions as needed
    }

    class ItemsDetailsLookup(private val recyclerView: RecyclerView): ItemDetailsLookup<Long>() {
        override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
            recyclerView.findChildViewUnder(e.x, e.y)?.let{
                val viewHolder = recyclerView.getChildViewHolder(it)
                if (viewHolder is ChatNodeAdapter.ChatNodeViewHolder) {
                    return viewHolder.getItemDetails()
                } else if (viewHolder is ChatNodeHeaderAdapter.HeaderViewHolder) {
                    return viewHolder.getItemDetails()
                }
            }
            // returning null clears selection and exits copy mode since it thinks an empty space was clicked.
            return null
        }
    }
}

class ChatNodeHeaderAdapter(
    private val fragmentManager: FragmentManager,
    private var currentSystemMessage: String,
    private val onSave: (newSystemMessage: String) -> Unit,
    private val onChange: (sysMsgHeight: Int) -> Unit
) : RecyclerView.Adapter<ChatNodeHeaderAdapter.HeaderViewHolder>() {
    private var updatedSystemMessage: String = currentSystemMessage
    var tracker: SelectionTracker<Long>? = null

    companion object {
        const val ITEM_ID = -10L
    }

    init { setHasStableIds(true) }

    override fun getItemId(position: Int): Long { return ITEM_ID }

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val binding = ChatNodeHeaderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeaderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind()
    }


    inner class HeaderViewHolder(val binding: ChatNodeHeaderItemBinding) : RecyclerView.ViewHolder(binding.root), PromptsListFragment.OnPromptSelectedListener {
        // there's only ever a single header item, so having this duplicated per holder shouldn't matter
        private val promptSearchDialog = PromptSearchDialog(this@HeaderViewHolder)

        init {
            binding.apply {
                systemMessageEditText.doBeforeTextChanged { _, _, _, _ ->
                    onChange(binding.root.height)
                }

                systemMessageEditText.doAfterTextChanged {
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

                systemMessageEditText.setOnFocusChangeListener { _, _ ->
                    updateSystemMessageButtons()
                }
                systemMessageInsertPromptButton.setOnFocusChangeListener { _, _ ->
                    updateSystemMessageButtons()
                }

                systemMessageInsertPromptButton.setOnClickListener {
                    // open dialog
                    promptSearchDialog.show(fragmentManager, "promptSearchDialog")
                }
            }
        }

        fun bind() {
            if (tracker != null) {
                markSelectedState()
            }
        }

        private fun markSelectedState() {
            tracker?.let {
                itemView.isActivated = it.isSelected(itemId)
                binding.chatNodeSelected.visibility = if (itemView.isActivated) View.VISIBLE else View.GONE
            }
        }

        private fun updateSystemMessageButtons() {
            binding.apply {
                val textFocused = systemMessageEditText.hasFocus()
                val hasChanges = updatedSystemMessage != currentSystemMessage
                val promptButtonFocused = systemMessageInsertPromptButton.isFocused
                editSystemMessageCancelButton.visibility = if (hasChanges) View.VISIBLE else View.GONE
                editSystemMessageSubmitButton.visibility = if (hasChanges) View.VISIBLE else View.GONE
                systemMessageInsertPromptButton.visibility = if ((!hasChanges && textFocused) || promptButtonFocused) View.VISIBLE else View.GONE

            }
        }

        // @PromptListFragment
        override val listType: PromptListAdapter.ListType = PromptListAdapter.ListType.SELECT
        override fun onPromptSelected(promptWithTags: PromptWithTags) {
            binding.systemMessageEditText.setText(promptWithTags.prompt.body)
            promptSearchDialog.dismiss()
        }

        fun getItemDetails() = object : ItemDetailsLookup.ItemDetails<Long>() {
            override fun getPosition(): Int { return absoluteAdapterPosition }
            override fun getSelectionKey(): Long { return ITEM_ID }
        }
    }
}

/**
 * this exists only to create padding at the bottom when the input is hidden
 */
class ChatNodeFooterAdapter(private var footerHeight: Int = 0) : RecyclerView.Adapter<ChatNodeFooterAdapter.FooterViewHolder>() {
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