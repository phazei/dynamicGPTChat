package com.phazei.dynamicgptchat.prompts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.phazei.dynamicgptchat.data.entity.PromptWithTags
import com.phazei.dynamicgptchat.databinding.PromptListItemBinding
import com.phazei.dynamicgptchat.swipereveal.SwipeRevealLayout
import com.phazei.dynamicgptchat.swipereveal.SwipeRevealLayout.SimpleSwipeListener
import com.phazei.dynamicgptchat.swipereveal.ViewBinderHelper

class PromptListAdapter(
    private val itemClickListener: PromptItemClickListener
) : RecyclerView.Adapter<PromptListAdapter.PromptWithTagsViewHolder>() {

    private val viewBinderHelper = ViewBinderHelper()
    private val promptWithTagsList: MutableList<PromptWithTags> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromptWithTagsViewHolder {
        val binding = PromptListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PromptWithTagsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PromptWithTagsViewHolder, position: Int) {
        val pt = promptWithTagsList[position]
        viewBinderHelper.bind(holder.binding.promptRevealLayout, pt.prompt.id.toString())
        viewBinderHelper.closeLayout(pt.prompt.id.toString())
        holder.bind(pt)
    }

    override fun getItemCount(): Int = promptWithTagsList.size

    inner class PromptWithTagsViewHolder(val binding: PromptListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.promptRevealLayout.minFlingVelocity = 500
            binding.promptRevealLayout.setSwipeListener(object: SimpleSwipeListener() {
                override fun onOpened(view: SwipeRevealLayout?) {
                    super.onOpened(view)
                    itemClickListener.onDeleteClick(promptWithTagsList[bindingAdapterPosition], bindingAdapterPosition)
                }
            })

            binding.promptEdit.setOnClickListener {
                itemClickListener.onSelectClick(promptWithTagsList[bindingAdapterPosition], bindingAdapterPosition)
            }

            binding.promptCopy.setOnClickListener {
                val prompt = promptWithTagsList[bindingAdapterPosition].prompt
                val clipboard = binding.root.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Prompt", prompt.body)
                clipboard.setPrimaryClip(clip)
                Snackbar.make(binding.root, "Prompt for ${prompt.title} copied", Snackbar.LENGTH_SHORT).show()
            }
        }

        fun bind(promptWithTags: PromptWithTags) {
            Log.d("TAG", "binding ${promptWithTags.prompt.title}")
            binding.promptTitle.text = promptWithTags.prompt.title
            binding.promptBody.text = promptWithTags.prompt.body

            // Clear the previous Chip instances
            binding.chipGroupTags.removeAllViews()
            // Add a Chip for each tag
            promptWithTags.tags.forEach { tag ->
                val chip = Chip(binding.chipGroupTags.context).apply {
                    text = tag.name
                    isClickable = false
                }
                binding.chipGroupTags.addView(chip)
            }

            val typedValue = TypedValue()
            if (bindingAdapterPosition % 2 == 0) {
                this.itemView.context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, typedValue, true)
            } else {
                this.itemView.context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
            }
            binding.promptItem.setBackgroundColor(typedValue.data)
        }
    }

    fun updateData(newData: List<PromptWithTags>) {
        promptWithTagsList.clear()
        promptWithTagsList.addAll(newData)
        notifyDataSetChanged()
    }

    fun updateItem(promptWithTags: PromptWithTags) {
        val index = promptWithTagsList.indexOf(promptWithTags)
        if (index == -1) {
            promptWithTagsList.add(promptWithTags)
            notifyItemInserted(promptWithTagsList.lastIndex)
        } else {
            promptWithTagsList[index] = promptWithTags
            notifyItemChanged(index)
        }
    }

    fun restoreItem(promptWithTags: PromptWithTags, position: Int) {
        promptWithTagsList.add(position, promptWithTags)
        // notifyItemInserted(position)
        //need to notify entire thing so it will redo the background coloring
        notifyDataSetChanged()
    }

    fun deleteItem(position: Int) {
        promptWithTagsList.removeAt(position)
        // notifyItemRemoved(position)
        //need to notify entire thing so it will redo the background coloring
        notifyDataSetChanged()
    }

    interface PromptItemClickListener {
        fun onSelectClick(promptWithTags: PromptWithTags, position: Int)
        fun onDeleteClick(promptWithTags: PromptWithTags, position: Int)
    }

}
