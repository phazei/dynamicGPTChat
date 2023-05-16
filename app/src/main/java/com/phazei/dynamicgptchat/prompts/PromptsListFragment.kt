package com.phazei.dynamicgptchat.prompts

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.data.entity.PromptWithTags
import com.phazei.dynamicgptchat.databinding.FragmentPromptsListBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PromptsListFragment : Fragment(), PromptListAdapter.PromptItemClickListener {

    private var _binding: FragmentPromptsListBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
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

        viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            promptsViewModel.promptsWithTags
                .collect { promptsWithTags ->
                    promptListAdapter.updateData(promptsWithTags)
                }
        }}
        viewLifecycleOwner.lifecycleScope.launch {
            delay(100)
            promptsViewModel.loadPromptsWithTags()
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
    }
    fun updatePromptWithTagsItem(promptWithTags: PromptWithTags) {
        promptListAdapter.updateItem(promptWithTags)
    }

    interface OnPromptSelectedListener {
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
}