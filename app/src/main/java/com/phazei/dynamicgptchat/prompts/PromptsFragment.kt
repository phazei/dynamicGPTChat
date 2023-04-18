package com.phazei.dynamicgptchat.prompts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.databinding.FragmentPromptsBinding

class PromptsFragment : Fragment() {

    private var _binding: FragmentPromptsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val promptsViewModel: PromptsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPromptsBinding.inflate(inflater, container, false)

        val textView: TextView = binding.promptsText
        promptsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}