package com.phazei.dynamicgptchat.chatnodes

import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.phazei.dynamicgptchat.chattrees.ChatTreeViewModel
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.data.entity.ChatTree
import com.phazei.dynamicgptchat.databinding.FragmentChatNodeListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
@Suppress("LiftReturnOrAssignment")
@AndroidEntryPoint
class ChatNodeListFragment : Fragment() {

    private var _binding: FragmentChatNodeListBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val chatNodeViewModel: ChatNodeViewModel by activityViewModels()
    private val chatTreeViewModel: ChatTreeViewModel by viewModels()
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatNodeViewModel.activeBranchUpdate.filterNotNull().collect { (updatedChatNode, activeBranch) ->
                    if (updatedChatNode.chatTreeId != chatTree.id) {
                        //when an request completes, it could be from a background chatTree and not the
                        //currently active one, so we don't need to process it and should just leave
                        return@collect
                    } else if (!chatNodeAdapter.isInit() && activeBranch == null) {
                        //if an individual request completes before the view loads, this will trigger right away
                        //so shouldn't be loaded yet
                        return@collect
                    }
                    if (activeBranch == null) {
                        //single node update
                        chatNodeAdapter.updateItem(updatedChatNode)
                    } else {
                        //branch update
                        chatNodeAdapter.updateData(updatedChatNode, activeBranch)
                    }
                    val position : Int
                    if (updatedChatNode.parentNodeId == null) { //rootNode
                        position = chatNodeAdapter.itemCount - 1
                    } else {
                        position = chatNodeAdapter.getItemPosition(updatedChatNode)
                    }
                    chatNodeAdapter.layoutManager.scrollToPositionWithOffset(position, 20)
                    // layoutManager.smoothScrollToPosition(position)
                }
            }
        }
        //recycler won't be populated until after this is loaded
        chatNodeViewModel.loadChatTreeChildrenAndActiveBranch(chatTree)

        setupMenu()
        setupRecycler()
        chatSubmitButtonHelper.setupChatSubmitButton()

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

        binding.promptInputEditText.setText("") //this will trigger buttonStatus listener
        chatTree.tempPrompt = ""
        chatTreeViewModel.saveChatTree(chatTree)

        chatNodeAdapter.addItem(parentLeaf, newNode)
        chatNodeAdapter.layoutManager.scrollToPosition(chatNodeAdapter.getItemPosition(newNode))
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
        //for ease of access and scrolling
        chatNodeAdapter.layoutManager = binding.chatNodeRecyclerView.layoutManager as LinearLayoutManager

        //fix item shows when keyboard opens/closes
        binding.chatNodeRecyclerView.addOnLayoutChangeListener(View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            var deltaY = oldBottom - bottom
            val keyboardOpened = deltaY > 0

            val layoutManager = chatNodeAdapter.layoutManager
            val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
            val totalItemCount = layoutManager.itemCount

            if (!keyboardOpened && totalItemCount < lastVisibleItemPosition + 2) {
                deltaY = 0 //if its close to the bottom after the keyboard has been closed, don't scroll up
            }
            binding.chatNodeRecyclerView.scrollBy(0, deltaY)
        })
    }

    private fun setupMenu() {
        (activity as AppCompatActivity).supportActionBar?.title = chatTree.title
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {}
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_chat_node_page, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.chat_settings -> {
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
        super.onDestroyView()
        _binding = null
    }

    /**
     * Helper class to help keep button methods organized
     */
    enum class Method { SEND, STOP, LOAD }
    inner class ChatSubmitButtonHelper(private val fragment: ChatNodeListFragment) {
        private val drawableSendToStop: AnimatedVectorDrawable by lazy { ContextCompat.getDrawable(requireContext(),
            R.drawable.avd_send_to_stop
        ) as AnimatedVectorDrawable }
        private val drawableStopToSend: AnimatedVectorDrawable by lazy { ContextCompat.getDrawable(requireContext(),
            R.drawable.avd_stop_to_send
        ) as AnimatedVectorDrawable }
        private val drawableLoadingStop: AnimatedVectorDrawable by lazy { ContextCompat.getDrawable(requireContext(),
            R.drawable.stop_and_load
        ) as AnimatedVectorDrawable }
        private val drawableSend = ContextCompat.getDrawable(requireContext(), R.drawable.round_send_36)
        private val drawableStop = ContextCompat.getDrawable(requireContext(), R.drawable.round_stop_36)
        private var lastSet: Method? = null
        private var temporalDisabled = false

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
                        lastSet = Method.LOAD
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
                    }
                } else {
                    //cancel active request
                    cancelChatRequest()
                }
                triggerTemporalDisabled()
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
                    checkSubmitStatusButton()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        private fun markSubmitSendable() {
            if (lastSet == Method.SEND) {
                binding.chatSubmitButton.icon = drawableSend
            } else {
                binding.chatSubmitButton.icon = drawableStopToSend
                drawableStopToSend.start()
            }

            //allow sending spaces, but not empty returns
            val empty = binding.promptInputEditText.text?.trim { it == '\r' || it == '\n' }.isNullOrEmpty()
            val isEnabled = !empty && !temporalDisabled
            binding.chatSubmitButton.isEnabled = isEnabled
            lastSet = Method.SEND
        }

        private fun markSubmitStoppable() {
            if (lastSet != Method.STOP && lastSet != Method.LOAD) {
                binding.chatSubmitButton.icon = drawableSendToStop
                drawableSendToStop.start()
            }
            binding.chatSubmitButton.isEnabled = !temporalDisabled
            lastSet = Method.STOP
        }

        /**
         * This will force the button to be disabled for a set period of time
         * without messing up status
         */
        private fun triggerTemporalDisabled() {
            binding.chatSubmitButton.isEnabled = false
            temporalDisabled = true
            lifecycleScope.launch { delay(500)
                temporalDisabled = false
                checkSubmitStatusButton()
            }
        }
    }
}