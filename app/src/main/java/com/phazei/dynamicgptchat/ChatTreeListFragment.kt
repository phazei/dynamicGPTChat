package com.phazei.dynamicgptchat

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.view.MotionEvent
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.phazei.dynamicgptchat.databinding.FragmentChatTreeListBinding
import androidx.recyclerview.widget.LinearLayoutManager


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 * This inherits from the ChatTreeItemClickListener so "this" can be passed to the Adapter listener
 */
@Suppress("RedundantNullableReturnType")
class ChatTreeListFragment : Fragment(), ChatTreeAdapter.ChatTreeItemClickListener {

    private var _binding: FragmentChatTreeListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var chatTreeAdapter: ChatTreeAdapter
    private lateinit var chatTreeDataSource: MutableList<ChatTree>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentChatTreeListBinding.inflate(inflater, container, false)

//        if (context is MainActivity) {
//            context.newChatTree = binding.newChatTree
//        }
//        val mainActivity = activity as MainActivity

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

//        binding.chatTreeRecyclerView.setOnClickListener {
//            Log.d("TAG", "RECYCLE CLICK")
//        }
        binding.chatTreeRecyclerView.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            var eID = motionEvent.action
            Log.d("TAG", "recycler touchmotionEvent: $eID")
            false
        }

        var counter = 0
        binding.newChatTree.setOnClickListener {
            Log.d("TAG", "clicky dicky do")
            // create a new ChatTree instance
            counter++
            val chatTree = ChatTree(0, "hellz yeah $counter", "", GPTSettings(), null)
            // add it to the data source of the RecyclerView
            chatTreeDataSource.add(chatTree)
            // notify the adapter to reflect the changes in the data
            chatTreeAdapter.notifyItemInserted(chatTreeDataSource.lastIndex)

        }

//        binding.buttonFirst.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
//        }

    }

    private fun setupRecyclerView() {
        chatTreeDataSource = mutableListOf()
        chatTreeDataSource.add(ChatTree(0, "Title 1", "System Message 1", GPTSettings()))
        chatTreeDataSource.add(ChatTree(1, "Title 2", "System Message 2", GPTSettings()))

        chatTreeAdapter = ChatTreeAdapter(chatTreeDataSource, this)

        binding.chatTreeRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.chatTreeRecyclerView.adapter = chatTreeAdapter
        binding.chatTreeRecyclerView.itemAnimator = object : DefaultItemAnimator() {
            override fun getRemoveDuration(): Long {
                return 500
            }
        }
    }

    override fun onEditClick(chatTree: ChatTree, position: Int) {

        Snackbar.make(binding.root, "Edit Screen", Snackbar.LENGTH_LONG)
            .setAction(""){}.show()
//        TODO("Not yet implemented")
    }

    override fun onDeleteClick(chatTree: ChatTree, position: Int) {
        Log.d("TAG", "delete button: $position")
        if (position != -1) {
            chatTreeDataSource.removeAt(position)
            chatTreeAdapter.notifyItemRemoved(position)

            Snackbar.make(binding.root, "Item deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                // Restore the deleted item
                chatTreeAdapter.restoreItem(chatTree, position)
            }.show()

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}