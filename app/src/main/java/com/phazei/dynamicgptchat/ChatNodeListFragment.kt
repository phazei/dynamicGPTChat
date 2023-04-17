package com.phazei.dynamicgptchat

import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.phazei.dynamicgptchat.data.ChatNode
import com.phazei.dynamicgptchat.data.ChatTree
import com.phazei.dynamicgptchat.databinding.FragmentChatNodeListBinding
import kotlinx.coroutines.launch


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
@Suppress("LiftReturnOrAssignment")
class ChatNodeListFragment : Fragment() {

    private var _binding: FragmentChatNodeListBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val chatNodeViewModel: ChatNodeViewModel by activityViewModels()
    private val chatTreeViewModel: ChatTreeViewModel by viewModels { ChatTreeViewModel.Companion.Factory(sharedViewModel.chatRepository) }
    private lateinit var chatNodeAdapter: ChatNodeAdapter
    private lateinit var chatSubmitButtonHelper: ChatSubmitButtonHelper
    private lateinit var chatTree: ChatTree
    private val dispatcher by lazy { requireActivity().onBackPressedDispatcher }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatNodeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatTree = sharedViewModel.activeChatTree!!
        chatSubmitButtonHelper = ChatSubmitButtonHelper(this)
        //recycler won't be populated until after this is loaded
        chatNodeViewModel.loadChatTreeChildrenAndActiveBranch(chatTree)

        setupMenu()
        setupRecycler()
        chatSubmitButtonHelper.setupChatSubmitButton()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatNodeViewModel.activeBranchUpdate.collect { (updatedChatNode, activeBranch) ->
                    if (activeBranch != null) {
                        chatNodeAdapter.updateData(updatedChatNode, activeBranch)
                    } else {
                        //single node update
                        chatNodeAdapter.updateItem(updatedChatNode)
                    }
                    val position : Int
                    if (updatedChatNode.parentNodeId == null) { //rootNode
                        position = chatNodeAdapter.itemCount - 1
                    } else {
                        position = chatNodeAdapter.getItemPosition(updatedChatNode)
                    }
                    val layoutManager = binding.chatNodeRecyclerView.layoutManager as LinearLayoutManager
                    layoutManager.scrollToPositionWithOffset(position, 20)
                    // layoutManager.smoothScrollToPosition(position)
                }
            }
        }

        chatNodeViewModel.activeRequests.asLiveData().observe(viewLifecycleOwner) { data ->
            //just need to be able to update the button
            chatSubmitButtonHelper.checkSubmitStatusButton()
        }

        dispatcher.addCallback(viewLifecycleOwner) {
            //ensure that the temporary text typed into the prompt box is saved before leaving the page
            saveTempPrompt()
            this.isEnabled = false
            dispatcher.onBackPressed()
        }
    }

    /**
     * Default new chat appends child to latest active leaf
     */
    private fun prepareChatRequest() {
        //todo: maybe just grab leaf from RecyclerView data
        val parentLeaf = sharedViewModel.chatRepository.getActiveLeaf(chatTree.rootNode)
        val newNode = ChatNode(0, chatTree.id, parentLeaf.id, binding.promptInputEditText.text.toString())
        newNode.parent = parentLeaf

        binding.promptInputEditText.setText("")

        chatNodeAdapter.addItem(parentLeaf, newNode)
        chatNodeViewModel.makeChatCompletionRequest(chatTree, newNode)
    }

    /**
     * Creates a new ChatNode and adds it as a child to the parent of edited item
     */
    private fun editChatRequest(editedNode: ChatNode, prompt: String) {
        val parentLeaf = editedNode.parent
        val newNode = ChatNode(0, chatTree.id, parentLeaf.id, prompt)
        newNode.parent = parentLeaf

        chatNodeAdapter.addItem(parentLeaf, newNode)
        chatNodeViewModel.makeChatCompletionRequest(chatTree, newNode)
    }

    private fun cancelChatRequest() {
        chatNodeViewModel.cancelRequest(chatTree.id)
        chatNodeAdapter.cancelLastItem()
    }

    private fun setupRecycler() {
        chatNodeAdapter = ChatNodeAdapter(
            chatNodes = mutableListOf(),
            onChatNodeClick = { chatNode ->
                // do something with the clicked chatNode
            },
            onEditPromptClick = { chatNode, prompt ->
                editChatRequest(chatNode, prompt)
                // do something with the clicked chatNode and prompt
            }
        )
        binding.chatNodeRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatNodeAdapter
        }
    }

    private fun setupMenu() {
        (activity as AppCompatActivity).supportActionBar?.title = chatTree.title
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.action_settings).isVisible = false
            }
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_chat_node_page, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_chat_settings -> {
                        findNavController().navigate(R.id.action_ChatNodeListFragment_to_ChatTreeSettingsFragment)
                        return true
                    }
                    android.R.id.home -> {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        return true
                    }
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * If user types something but doesn't press enter, save it just in case so they don't lose it
     * Can't perform action in onPause since it's not completed before next fragment init
     */
    private fun saveTempPrompt() {
        //store anything that wasn't submitted before leaving the page
        val tempPrompt = binding.promptInputEditText.text.toString()
        if (tempPrompt != "" && tempPrompt != chatTree.tempPrompt) {
            chatTree.tempPrompt = tempPrompt
            chatTreeViewModel.saveChatTree(chatTree)
        }

        // Perform your action here before leaving the Fragment
    }

    override fun onDestroyView() {
        Log.d("TAG", "GOODBYE!!!!!!!!!!!!!!!!!!!!!")
        super.onDestroyView()
        _binding = null
    }

    /**
     * Helper class to help keep button methods organized
     */
    inner class ChatSubmitButtonHelper(private val fragment: ChatNodeListFragment) {
        private val drawableSendToStop: AnimatedVectorDrawable by lazy { ContextCompat.getDrawable(requireContext(), R.drawable.avd_send_to_stop) as AnimatedVectorDrawable }
        private val drawableStopToSend: AnimatedVectorDrawable by lazy { ContextCompat.getDrawable(requireContext(), R.drawable.avd_stop_to_send) as AnimatedVectorDrawable }
        private val drawableLoadingStop: AnimatedVectorDrawable by lazy { ContextCompat.getDrawable(requireContext(), R.drawable.stop_and_load) as AnimatedVectorDrawable }

        fun setupChatSubmitButton() {
            //callback will change SendToStop into LoadingStop after animation is complete
            val animationCallback = object : Animatable2Compat.AnimationCallback() {
                //after button is animated to a stop, switch it to the loading stop button
                override fun onAnimationEnd(drawable: Drawable) {
                    super.onAnimationEnd(drawable)
                    if (chatNodeViewModel.isRequestActive(chatTree.id)) {
                        //sometimes if the request is too fast, it will switch
                        //back to 'send' before the animation
                        //but this call back triggers after it's already be set to send
                        binding.chatSubmitButton.icon = drawableLoadingStop
                        drawableLoadingStop.start()
                    }
                }
            }
            AnimatedVectorDrawableCompat.registerAnimationCallback(drawableSendToStop, animationCallback)

            binding.chatSubmitButton.setOnClickListener {
                if (!chatNodeViewModel.isRequestActive(chatTree.id)) {
                    //don't try to submit till chatNodes loaded
                    if (chatNodeAdapter.isInit()) {
                        //create a new request
                        prepareChatRequest()
                        // Prevent accidental cancels right after submitting
                        binding.chatSubmitButton.isEnabled = false
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.chatSubmitButton.isEnabled = true
                        }, 1500)
                    }
                } else {
                    //cancel active request
                    cancelChatRequest()
                }
            }

            disableButtonOnNoPrompt()
        }

        fun checkSubmitStatusButton() {
            if (chatNodeViewModel.isRequestActive(chatTree.id)) {
                markSubmitStoppable()
            } else {
                markSubmitSendable()
            }
        }

        private fun disableButtonOnNoPrompt() {

            binding.promptInputEditText.setText(chatTree.tempPrompt)
            binding.chatSubmitButton.isEnabled = binding.promptInputEditText.text.toString() != ""

            binding.promptInputEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    binding.chatSubmitButton.isEnabled = !s.isNullOrEmpty()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        private fun markSubmitSendable() {
            binding.chatSubmitButton.icon = drawableStopToSend
            drawableStopToSend.start()
        }

        private fun markSubmitStoppable() {
            binding.chatSubmitButton.icon = drawableSendToStop
            drawableSendToStop.start()
        }
    }
}