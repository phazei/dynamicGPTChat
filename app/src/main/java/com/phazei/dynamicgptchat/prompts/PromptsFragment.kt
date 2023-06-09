package com.phazei.dynamicgptchat.prompts

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.data.entity.Prompt
import com.phazei.dynamicgptchat.data.pojo.PromptWithTags
import com.phazei.dynamicgptchat.databinding.FragmentPromptsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PromptsFragment : Fragment(), PromptsListFragment.OnPromptSelectedListener, PromptFormDialog.OnPromptFormDialogListener {

    private var _binding: FragmentPromptsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val promptsViewModel: PromptsViewModel by activityViewModels()
    private lateinit var promptsListFragment: PromptsListFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPromptsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel.onFabClick.value = { onAddFABClick() }

        promptsListFragment = childFragmentManager.findFragmentById(R.id.prompts_list_fragment) as PromptsListFragment
        promptsListFragment.setOnPromptSelectedListener(this@PromptsFragment)

    }

    fun onAddFABClick() {
        openPromptFormDialog(PromptWithTags(Prompt(), mutableListOf()))
    }

    /**
     * Dialog used for view, edit, add
     */
    fun openPromptFormDialog(promptWithTags: PromptWithTags) {
        val promptFormDialog = PromptFormDialog(promptWithTags)
        promptFormDialog.setOnPromptFormDialogListener(this)
        promptFormDialog.show(childFragmentManager, "promptFormDialog")
    }

    /**
     *  From @PromptsListFragment interface - communicate with RecyclerView, when an item is selected from the
     *  recycler this is the action that will be performed
     */
    override val listType: PromptListAdapter.ListType = PromptListAdapter.ListType.EDIT
    override fun onPromptSelected(promptWithTags: PromptWithTags) {
        openPromptFormDialog(promptWithTags)
    }

    /**
     * Save prompt submitted from dialog
     * From @PromptFormDialog interface - communicate with dialog, what happens when item is submitted from dialog
     */
    override fun onSavePrompt(promptWithTags: PromptWithTags) {
        promptsViewModel.savePromptWithTags(promptWithTags).invokeOnCompletion {
            // could be new tags added need to make sure they're in the search by tag autocomplete
            promptsViewModel.loadAllTags()
        }
        promptsListFragment.updatePromptWithTagsItem(promptWithTags)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}