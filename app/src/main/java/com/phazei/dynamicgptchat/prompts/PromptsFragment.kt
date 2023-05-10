package com.phazei.dynamicgptchat.prompts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.databinding.FragmentPromptsBinding

class PromptsFragment : Fragment() {

    private var _binding: FragmentPromptsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPromptsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.promptsText.setOnClickListener {
            findNavController().navigate(R.id.action_PromptsFragment_to_PromptsInfoFragment)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}