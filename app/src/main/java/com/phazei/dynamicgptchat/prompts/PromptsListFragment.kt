package com.phazei.dynamicgptchat.prompts

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.data.pojo.PromptWithTags
import com.phazei.dynamicgptchat.data.entity.Tag
import com.phazei.dynamicgptchat.databinding.FragmentPromptsListBinding
import com.phazei.taginputview.TagInputData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class PromptsListFragment() : Fragment(), PromptListAdapter.PromptItemClickListener {
    private var _binding: FragmentPromptsListBinding? = null
    private val binding get() = _binding!!
    private val promptsViewModel: PromptsViewModel by activityViewModels()

    private var promptSelectedListener: OnPromptSelectedListener? = null
    private lateinit var promptListAdapter: PromptListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPromptsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        promptSelectedListener?.let { listener ->
            promptListAdapter.setListType(listener.listType)
        }

        viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                promptsViewModel.promptsWithTags
                    .collect { promptsWithTags ->
                        promptListAdapter.updateData(promptsWithTags)
                    }
            }
            launch {
                promptsViewModel.tags
                    .filterNotNull()
                    .collect { tags ->
                        setupTagsSearch(tags)
                    }
            }
        }}
        viewLifecycleOwner.lifecycleScope.launch {
            delay(100)
            promptsViewModel.loadPromptsWithTags()
            promptsViewModel.loadAllTags()
        }

        val addToRemoveTag = ContextCompat.getDrawable(view.context, R.drawable.avd_add_to_remove_tag) as AnimatedVectorDrawable
        val removeToAddTag = ContextCompat.getDrawable(view.context, R.drawable.avd_remove_to_add_tag) as AnimatedVectorDrawable

        binding.promptSearchTags.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val searchTagHeight = binding.promptSearchTags.measuredHeight
        val minHeight = binding.promptSearchTags.minimumHeight
        binding.promptSearchTagToggle.setOnClickListener {
            binding.promptSearchTags.minimumHeight = 0
            if (binding.promptSearchTags.visibility == View.VISIBLE) {
                resizeAnimator(binding.promptSearchTags, binding.promptSearchTags.height, 1,
                    onEnd ={
                        binding.promptSearchTags.visibility = View.GONE
                        binding.promptSearchTags.minimumHeight = minHeight
                        binding.promptSearchTags.clearTags()
                    }).start()
                binding.promptSearchTagToggle.setImageDrawable(removeToAddTag)
                removeToAddTag.start()
            } else {
                resizeAnimator(binding.promptSearchTags, 1, searchTagHeight,
                    onStart ={ binding.promptSearchTags.visibility = View.VISIBLE },
                    onEnd ={
                        binding.promptSearchTags.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        binding.promptSearchTags.minimumHeight = minHeight
                    }).start()

                binding.promptSearchTagToggle.setImageDrawable(addToRemoveTag)
                addToRemoveTag.start()
            }
        }

        var textChangeJob: Job? = null
        binding.promptSearchText.doOnTextChanged { text, _, _, _ ->
            // debounce on text search
            textChangeJob?.cancel()
            textChangeJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(1000)
                searchPrompts()
            }
        }

        binding.promptSearchTags.setOnTagChangeListener { _, _ ->
            searchPrompts()
        }

    }

    private fun searchPrompts() {
        promptsViewModel.searchPromptsWithTags(binding.promptSearchText.text.toString(), binding.promptSearchTags.getTagsOfType())
    }

    private fun setupTagsSearch(tags: List<Tag>) {
        // if tags are updated, then we can to keep the selected ones selected
        val selectedTags = binding.promptSearchTags.getTagsOfType<Tag>()

        val tagsAdapter = TagsArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tags)
        binding.promptSearchTags.apply {
            setAutoCompleteAdapter(tagsAdapter)
            setTagInputData(object : TagInputData<Tag>() {
                override fun inputConverter(input: String): Tag? {
                    // Check if tag exists. If so, use existing tag with id.
                    val foundTag = tagsAdapter.run {
                        (0 until count).asSequence()
                            .map { getItem(it) }
                            .firstOrNull { it.name.lowercase() == input.lowercase() }
                    }
                    // If foundTag is not null, it means the tag exists.
                    foundTag?.let {
                        return it
                    }
                    // If tag doesn't exist, return null.
                    return null
                }
                override fun displayConverter(tag: Tag): String {
                    val tagTag = tag as Tag
                    return tagTag.name
                }
            })
            if (selectedTags.size > 0) {
                selectedTags.forEach { addTag(it) }
            }
        }
    }

    private fun setupRecyclerView() {
        promptListAdapter = PromptListAdapter(this)
        binding.promptListRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = promptListAdapter
        }
    }

    fun setOnPromptSelectedListener(listener: OnPromptSelectedListener) {
        promptSelectedListener = listener
        if (::promptListAdapter.isInitialized) {
            promptListAdapter.setListType(promptSelectedListener!!.listType)
        }
    }

    /**
     * Handle updating prompts RecyclerView after being saved
     */
    fun updatePromptWithTagsItem(promptWithTags: PromptWithTags) {
        // item always moves to top, so scroll to top

        promptListAdapter.updateItem(promptWithTags)
        binding.promptListRecyclerView.scrollToPosition(0)
        binding.promptListRecyclerView.post {
            val position = promptListAdapter.getItemPosition(promptWithTags)
            val holder = binding.promptListRecyclerView.findViewHolderForAdapterPosition(position) as PromptListAdapter.PromptWithTagsViewHolder
            promptListAdapter.flashItem(position, holder)
        }

    }

    /**
     * When recyclerView item is clicked
     */
    interface OnPromptSelectedListener {
        val listType: PromptListAdapter.ListType
        fun onPromptSelected(promptWithTags: PromptWithTags)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * From @PromptListAdapter - when item is tapped
     */
    override fun onSelectClick(promptWithTags: PromptWithTags, position: Int) {
        promptSelectedListener!!.onPromptSelected(promptWithTags)
    }

    /**
     * From @PromptListAdapter - when item is swipped left
     */
    override fun onDeleteClick(promptWithTags: PromptWithTags, position: Int) {
        if (position != -1) {
            promptListAdapter.deleteItem(position)

            Snackbar.make(binding.root, "Prompt deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    // Restore the deleted item
                    promptListAdapter.restoreItem(promptWithTags, position)
                }.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if (event != DISMISS_EVENT_ACTION) {
                            promptsViewModel.deletePrompt(promptWithTags.prompt)
                        }
                    }
                }).show()
        }
    }

    private fun resizeAnimator(view: View, start: Int, end: Int, onEnd: () -> Unit = {}, onStart: () -> Unit = {}): ValueAnimator {
        return ValueAnimator.ofInt(start, end).apply {
            duration = 300
            addUpdateListener { animation ->
                val layoutParams = view.layoutParams
                layoutParams.height = animation.animatedValue as Int
                view.layoutParams = layoutParams
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
                override fun onAnimationStart(animation: Animator) {
                    onStart()
                }
            })
        }
    }

}