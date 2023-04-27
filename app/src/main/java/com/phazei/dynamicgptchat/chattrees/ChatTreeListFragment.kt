package com.phazei.dynamicgptchat.chattrees

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.fragment.findNavController
import com.phazei.dynamicgptchat.databinding.FragmentChatTreeListBinding
import androidx.recyclerview.widget.LinearLayoutManager
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.data.entity.ChatTree
import dagger.hilt.android.AndroidEntryPoint


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 * This inherits from the ChatTreeItemClickListener so "this" can be passed to the Adapter listener
 */
@Suppress("RedundantNullableReturnType")
@AndroidEntryPoint
class ChatTreeListFragment : Fragment(), ChatTreeAdapter.ChatTreeItemClickListener {

    private var _binding: FragmentChatTreeListBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var chatTreeAdapter: ChatTreeAdapter
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val chatTreeViewModel: ChatTreeViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatTreeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatTreeViewModel.chatTrees.observe(viewLifecycleOwner) { chatTrees ->
            chatTreeAdapter.updateChatTrees(chatTrees.toMutableList())
        }
        setupRecyclerView()
        chatTreeViewModel.fetchChatTrees()

        sharedViewModel.onFabClick.value = { onAddFABClick() }

    }

    private fun setupRecyclerView() {
        chatTreeAdapter = ChatTreeAdapter(mutableListOf(), this)

        //LinearLayoutManager necessary for swipereveal
        binding.chatTreeRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatTreeAdapter
            itemAnimator = object : DefaultItemAnimator() {
                override fun getRemoveDuration(): Long {
                    return 500 //slow down removing
                }
            }
        }
    }

    fun onAddFABClick() {

        if (chatTreeViewModel.chatTrees.value == null) {
            Snackbar.make(binding.root, "Please wait while loading conversations", Snackbar.LENGTH_LONG).show()
            return
        }

        // create a new ChatTree instance
        val chatTree = ChatTree("New Tree #${chatTreeViewModel.chatTrees.value?.size}")

        binding.chatTreeRecyclerView.layoutManager?.scrollToPosition(0)
        chatTreeViewModel.addChatTree(chatTree)
        chatTreeAdapter.addItem(chatTree)

        sharedViewModel.activeChatTree = chatTree
        findNavController().navigate(R.id.action_ChatTreeListFragment_to_ChatNodeListFragment)
    }

    //@chatTreeAdapter
    override fun onItemClick(chatTree: ChatTree, position: Int) {
        sharedViewModel.activeChatTree = chatTree
        findNavController().navigate(R.id.action_ChatTreeListFragment_to_ChatNodeListFragment)
    }
    //@chatTreeAdapter
    override fun onEditClick(chatTree: ChatTree, position: Int) {
        sharedViewModel.activeChatTree = chatTree
        findNavController().navigate(R.id.action_ChatTreeListFragment_to_chatTreeSettingsFragment)
    }
    //@chatTreeAdapter Deletes chatTree item but provides a few second to undo action using Snackbar
    override fun onDeleteClick(chatTree: ChatTree, position: Int) {
        if (position != -1) {
            chatTreeAdapter.deleteItem(position)

            Snackbar.make(binding.root, "Item deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    // Restore the deleted item
                    chatTreeAdapter.restoreItem(chatTree, position)
                }.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if (event != DISMISS_EVENT_ACTION) {
                            chatTreeViewModel.deleteChatTree(chatTree, position)
                        }
                    }
                }).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}