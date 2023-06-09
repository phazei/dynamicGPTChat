package com.phazei.dynamicgptchat.chattrees

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phazei.dynamicgptchat.data.entity.ChatTree
import com.phazei.dynamicgptchat.databinding.ChatTreeItemBinding
import com.phazei.dynamicgptchat.swipereveal.ViewBinderHelper
import com.phazei.utils.Solacon
import java.util.*

class ChatTreeAdapter(
    private var chatTrees: MutableList<ChatTree>,
    private val itemClickListener: ChatTreeItemClickListener
) :  RecyclerView.Adapter<ChatTreeAdapter.ChatTreeViewHolder>() {

    //used for swipe reveal
    private val viewBinderHelper = ViewBinderHelper()

    init {
        viewBinderHelper.setOpenOnlyOne(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatTreeViewHolder {
        val binding = ChatTreeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatTreeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatTreeViewHolder, position: Int) {
        val chatTree = chatTrees[position]
        viewBinderHelper.bind(holder.binding.chatTreeSwipeLayout, chatTree.id.toString())
        holder.bind(chatTree)
    }

    override fun getItemCount() = chatTrees.size

    @SuppressLint("ClickableViewAccessibility")
    inner class ChatTreeViewHolder(val binding: ChatTreeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Setting listeners in init of holder prevents redundantly setting them on each individual item
         */
        init {
            binding.chatTreeCardView.setOnTouchListener { view, motionEvent ->
                // val chatTreeCardView = view as CardView
                val background = binding.chatTreeCardBackground

                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_HOVER_ENTER -> {
                       // val typedValue = TypedValue()
                       // view.context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)
                       // chatTreeCardView.setCardBackgroundColor(typedValue.data)
                        background.alpha = 0.2f
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_HOVER_EXIT -> {
                       // val typedValue = TypedValue()
                       // view.context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)
                       // chatTreeCardView.setCardBackgroundColor(typedValue.data)
                        background.alpha = 0f
                    }
                }
                false
            }

            binding.deleteTreeButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemClickListener.onDeleteClick(chatTrees[position], position)
                }
            }

            binding.editTreeButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemClickListener.onEditClick(chatTrees[position], position)
                }
            }

            binding.chatTreeCardView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemClickListener.onItemClick(chatTrees[position], position)
                }
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        fun bind(chatTree: ChatTree) {
            binding.apply {
                chatTreeTitleText.text = chatTree.title
                chatTreeSettingsText.text = chatTree.gptSettings.toString()

                chatTreeModelIcon.setImageBitmap(Solacon.generateBitmap(chatTree.gptSettings.model, 256))

            }
        }
    }

    //Need to keep chatTrees a separate reference so it may be deleted
    @SuppressLint("NotifyDataSetChanged")
    fun updateChatTrees(chatTrees: MutableList<ChatTree>) {
        this.chatTrees.clear()
        this.chatTrees.addAll(chatTrees)
        notifyDataSetChanged()
    }

    fun addItem(chatTree: ChatTree) {
        chatTrees.add(0, chatTree)
        notifyItemInserted(0)
    }

    fun updateItem(position: Int, chatTree: ChatTree) {
        chatTrees[position] = chatTree
        notifyItemChanged(position)
    }

    fun deleteItem(position: Int) {
        chatTrees.removeAt(position)
        notifyItemRemoved(position)
    }

    fun restoreItem(chatTree: ChatTree, position: Int) {
        chatTrees.add(position, chatTree)
        notifyItemInserted(position)
        viewBinderHelper.closeLayout(chatTree.id.toString())
    }

    interface ChatTreeItemClickListener {
        fun onDeleteClick(chatTree: ChatTree, position: Int)
        fun onEditClick(chatTree: ChatTree, position: Int)
        fun onItemClick(chatTree: ChatTree, position: Int)
    }

}