package com.phazei.dynamicgptchat.prompts

import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.phazei.dynamicgptchat.R
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
    private var activePromptPosition: Int? = null
    private var listType: ListType = ListType.EDIT

    private lateinit var drawableCheckedBox: AnimatedVectorDrawable

    enum class ListType { EDIT, SELECT }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromptWithTagsViewHolder {
        val binding = PromptListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        if (!::drawableCheckedBox.isInitialized) {
            drawableCheckedBox = ContextCompat.getDrawable(parent.context, R.drawable.avd_checked_box) as AnimatedVectorDrawable
        }

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

            /**
             * Need to create an active prompt so it will display the entire prompt body
             */
            binding.promptItem.setOnClickListener {
                val prevPromptPosition = activePromptPosition
                if (activePromptPosition == bindingAdapterPosition) {
                    activePromptPosition = null
                } else {
                    if (prevPromptPosition != null) {
                        notifyItemChanged(prevPromptPosition)
                    }
                    activePromptPosition = bindingAdapterPosition
                }
                notifyItemChanged(bindingAdapterPosition)
            }

            binding.promptRevealLayout.minFlingVelocity = 500
            binding.promptRevealLayout.setSwipeListener(object: SimpleSwipeListener() {
                override fun onOpened(view: SwipeRevealLayout?) {
                    super.onOpened(view)
                    itemClickListener.onDeleteClick(promptWithTagsList[bindingAdapterPosition], bindingAdapterPosition)
                }
            })
            binding.deletePromptButton.setOnClickListener {
                // potentially makes this accessible?
                // the button is the text trash can image view that's visible when swiped
                itemClickListener.onDeleteClick(promptWithTagsList[bindingAdapterPosition], bindingAdapterPosition)
            }

            binding.promptEdit.setOnClickListener {
                itemClickListener.onSelectClick(promptWithTagsList[bindingAdapterPosition], bindingAdapterPosition)
            }
            binding.promptSelect.setOnClickListener {
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
            binding.promptTitle.text = promptWithTags.prompt.title
            binding.promptBody.text = promptWithTags.prompt.body

            // active prompt simply displays the entire body
            if (activePromptPosition == bindingAdapterPosition) {
                binding.promptBody.maxLines = Int.MAX_VALUE
            } else {
                binding.promptBody.maxLines = 3
            }
            if (listType == ListType.EDIT) {
                binding.promptSelect.visibility = View.GONE
                binding.promptEdit.visibility = View.VISIBLE
                binding.promptRevealLayout.setLockDrag(false)
            } else if (listType == ListType.SELECT) {
                binding.promptSelect.visibility = View.VISIBLE
                binding.promptEdit.visibility = View.GONE
                binding.promptSelect.setImageDrawable(drawableCheckedBox)
                drawableCheckedBox.start()

                binding.promptRevealLayout.setLockDrag(true)
            }


            val typedValue = TypedValue()

            binding.promptGroupTags.apply {
                // Clear the previous Chip instances
                removeAllViews()

                // set group UI
                chipSpacingHorizontal = 15
                chipSpacingVertical = 15
                visibility = if (promptWithTags.tags.size > 0) View.VISIBLE else View.GONE

                context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)

                // Add a Chip for each tag
                promptWithTags.tags.forEach { tag ->
                    val chip = Chip(context).apply {
                        text = tag.name
                        isClickable = false
                        isFocusable = false
                        setEnsureMinTouchTargetSize(false)
                        layoutParams = ChipGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            22.dpToPx(binding.promptGroupTags.context)
                        )
                        chipStartPadding = 1.dpToPx(context).toFloat()
                        chipEndPadding = 1.dpToPx(context).toFloat()

                        setPadding(0, 0, 0, 5)
                        chipCornerRadius = 10.dpToPx(context).toFloat()
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                        chipBackgroundColor = ColorStateList.valueOf(typedValue.data)
                        rippleColor = null
                    }
                    addView(chip)
                }
            }

            // set background color of item and buttons
            //     if buttons match background, they will still have touch and selected animations
            if (bindingAdapterPosition % 2 == 0) {
                this.itemView.context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, typedValue, true)
            } else {
                this.itemView.context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
            }
            binding.promptItem.setBackgroundColor(typedValue.data)
            binding.promptEdit.backgroundTintList = ColorStateList.valueOf(typedValue.data)
            binding.promptCopy.backgroundTintList = ColorStateList.valueOf(typedValue.data)
            binding.promptSelect.backgroundTintList = ColorStateList.valueOf(typedValue.data)

        }
    }

    /**
     * Make item blink to draw eye to it after a scroll
     */
    fun flashItem(position: Int, holder: PromptWithTagsViewHolder) {
        val alphaAnimation = ObjectAnimator.ofFloat(holder.itemView, View.ALPHA, 1f, 0.5f, 1f, 0.5f, 1f)
        alphaAnimation.duration = 1000 // milliseconds
        alphaAnimation.start()
    }

    fun setListType(type: ListType) {
        listType = type
        notifyDataSetChanged()
    }

    fun getItemPosition(promptWithTags: PromptWithTags): Int {
        return promptWithTagsList.indexOf(promptWithTags)
    }

    fun updateData(newData: List<PromptWithTags>) {
        promptWithTagsList.clear()
        promptWithTagsList.addAll(newData)
        notifyDataSetChanged()
    }

    /**
     * Since the list is ordered by updateDate, always add updated items
     * to the front of the list
     */
    fun updateItem(promptWithTags: PromptWithTags) {
        val index = promptWithTagsList.indexOf(promptWithTags)
        if (index == -1) {
            // Add new item at the beginning
            promptWithTagsList.add(0, promptWithTags)
            notifyItemInserted(0)
        } else {
            // Remove the existing item
            promptWithTagsList.removeAt(index)
            notifyItemRemoved(index)
            // Add it back at the beginning
            promptWithTagsList.add(0, promptWithTags)
            notifyItemInserted(0)
        }

        //need to notify entire thing so it will redo the background coloring
        notifyDataSetChanged()
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

    private fun Int.dpToPx(context: Context): Int {
        val resources = context.resources
        val displayMetrics = resources.displayMetrics
        return (this * (displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT))
    }

}
