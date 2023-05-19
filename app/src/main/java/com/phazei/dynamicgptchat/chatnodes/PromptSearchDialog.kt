package com.phazei.dynamicgptchat.chatnodes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.databinding.DialogPromptSearchBinding
import com.phazei.dynamicgptchat.prompts.PromptsListFragment


class PromptSearchDialog(private val selectedListener: PromptsListFragment.OnPromptSelectedListener) : BottomSheetDialogFragment() {

    private var _binding: DialogPromptSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var promptsListFragment: PromptsListFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        _binding = DialogPromptSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        promptsListFragment = childFragmentManager.findFragmentById(R.id.prompts_list_fragment) as PromptsListFragment
        promptsListFragment.setOnPromptSelectedListener(selectedListener)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
