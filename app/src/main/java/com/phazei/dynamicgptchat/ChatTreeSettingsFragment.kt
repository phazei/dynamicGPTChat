package com.phazei.dynamicgptchat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.fragment.app.activityViewModels
import com.phazei.dynamicgptchat.databinding.FragmentChatTreeListBinding
import com.phazei.dynamicgptchat.databinding.FragmentChatTreeSettingsBinding


class ChatTreeSettingsFragment : Fragment() {
    // by utilizing `_binding` which is nullable,it eliminates the need for null checks
    //    when using `binding` and allows it to be cleared in onDestroy
    private var _binding: FragmentChatTreeSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatTree: ChatTree
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatTreeSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatTree = sharedViewModel.activeChatTree!!

        // Initialize your settings input fields with the values from chatTree
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
