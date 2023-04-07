package com.phazei.dynamicgptchat

import android.annotation.SuppressLint
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.phazei.dynamicgptchat.databinding.ChatTreeItemBinding
import com.phazei.dynamicgptchat.swipereveal.ViewBinderHelper

class ChatTreeAdapter(
    private val chatTrees: MutableList<ChatTree>,
    private val itemClickListener: ChatTreeItemClickListener
) :  RecyclerView.Adapter<ChatTreeAdapter.ChatTreeViewHolder>() {

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
        viewBinderHelper.bind(holder.binding.swipeLayout, chatTree.uuid)
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
            Log.d("TAG", "INITIALIZATION OF VIEW HOLDER")

            binding.chatTreeCardView.setOnTouchListener { view, motionEvent ->
                val chatTreeCardView = view as CardView
                val background = binding.chatTreeCardBackground

                var eID = motionEvent.action
                Log.d("TAG", "touchmotionEvent: $eID")
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
            Log.d("TAG", "INITIALIZATION OF BIND VIEW ITEM")
            binding.apply {
                chatTreeTitleText.text = chatTree.title
                chatTreeSettingsText.text = chatTree.defaultGPTSettings.toString()
            }
        }
    }

    fun restoreItem(chatTree: ChatTree, position: Int) {
        chatTrees.add(position, chatTree)
        notifyItemInserted(position)
        viewBinderHelper.closeLayout(chatTree.uuid)
    }

    interface ChatTreeItemClickListener {
        fun onDeleteClick(chatTree: ChatTree, position: Int)
        fun onEditClick(chatTree: ChatTree, position: Int)
        fun onItemClick(chatTree: ChatTree, position: Int)
    }

}