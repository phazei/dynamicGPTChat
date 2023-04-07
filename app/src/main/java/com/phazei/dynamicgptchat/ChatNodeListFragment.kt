package com.phazei.dynamicgptchat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.phazei.dynamicgptchat.databinding.FragmentChatNodeListBinding


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ChatNodeListFragment : Fragment() {

    private var _binding: FragmentChatNodeListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatNodeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_ChatNodeListFragment_to_ChatTreeListFragment)
        }
        binding.buttonChatSettings.setOnClickListener {
            findNavController().navigate(R.id.action_ChatNodeListFragment_to_chatTreeSettingsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}