package com.phazei.dynamicgptchat.chatnodes

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.PopupWindow
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
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.chattrees.ChatTreeViewModel
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.data.entity.ChatTree
import com.phazei.dynamicgptchat.databinding.ChatNodeFloatingMenuBinding
import com.phazei.dynamicgptchat.databinding.ChatNodeItemBinding
import com.phazei.dynamicgptchat.databinding.FragmentChatNodeListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.pow


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
@Suppress("LiftReturnOrAssignment")
@AndroidEntryPoint
class ChatNodeListFragment : Fragment(), ChatNodeAdapter.OnNodeActionListener {

    private var _binding: FragmentChatNodeListBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val chatNodeViewModel: ChatNodeViewModel by activityViewModels()
    private val chatTreeViewModel: ChatTreeViewModel by viewModels()
    private lateinit var chatNodeAdapter: ChatNodeAdapter
    private lateinit var chatNodeFooterAdapter: ChatNodeFooterAdapter
    private lateinit var chatSubmitButtonHelper: ChatSubmitButtonHelper
    private lateinit var popupMenuHelper: PopupMenuHelper
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
        popupMenuHelper = PopupMenuHelper(this, requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            chatNodeViewModel.activeBranchUpdate
                .filterNotNull()
                // when an request completes, it could be from a background chatTree and not the
                // currently active one, so we don't need to process it and should just leave
                .filter { (updatedChatNode, _) -> updatedChatNode.chatTreeId == chatTree.id }
                // .conflate() //conflate could skip items if other trees are triggering here
                .collect { (updatedChatNode, activeBranch) ->
                    if (!chatNodeAdapter.isInit() && activeBranch == null) {
                        // if an individual request completes before the view loads, this will trigger right away
                        // so shouldn't be loaded yet
                        return@collect
                    }
                    if (activeBranch == null) {
                        // single node update
                        chatNodeAdapter.updateItem(updatedChatNode)
                    } else {
                        // branch update
                        chatNodeAdapter.updateData(updatedChatNode, activeBranch)
                    }
                    val position : Int
                    if (updatedChatNode.parentNodeId == null) { // rootNode
                        position = chatNodeAdapter.itemCount - 1 // get leaf item
                        binding.chatNodeRecyclerView.scrollToPosition(position + 1) //+1 for header
                    } else {
                        position = chatNodeAdapter.getItemPosition(updatedChatNode) // stay by edited item
                    }
                    // TODO: fix scrolling glitchyness when streaming
                }
            }
        }
        // recycler won't be populated until after this is loaded
        viewLifecycleOwner.lifecycleScope.launch {
            // no matter what I've tried, 1 out of 20 times this triggers before the listener is attached and doesn't load
            // so forcing a delay on it
            delay(100)
            chatNodeViewModel.loadChatTreeChildrenAndActiveBranch(chatTree)
        }

        setupMenu()
        setupRecycler()
        chatSubmitButtonHelper.setupChatSubmitButton()

        chatNodeViewModel.activeRequests.asLiveData().observe(viewLifecycleOwner) { data ->
            // just need to be able to update the button
            chatSubmitButtonHelper.checkSubmitStatusButton()
        }

        dispatcher.addCallback(viewLifecycleOwner) {
            // ensure that the temporary text typed into the prompt box is saved before leaving the page
            saveTempPrompt()
            this.isEnabled = false
            dispatcher.onBackPressed()
        }
    }

    /**
     * Default new chat appends child to latest active leaf
     */
    private fun prepareChatRequest() {
        // todo: maybe just grab leaf from RecyclerView data
        val parentLeaf = sharedViewModel.chatRepository.getActiveLeaf(chatTree.rootNode)
        val newNode = ChatNode(0, chatTree.id, parentLeaf.id, binding.promptInputEditText.text.toString())
        newNode.parent = parentLeaf

        binding.promptInputEditText.setText("") // this will trigger buttonStatus listener
        chatTree.tempPrompt = ""
        chatTreeViewModel.saveChatTree(chatTree)

        chatNodeAdapter.addItem(parentLeaf, newNode)
        binding.chatNodeRecyclerView.scrollToPosition(chatNodeAdapter.getItemPosition(newNode) + 1) //+1 for header
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

    //@ChatNodeAdapter
    override fun onNodeSelected(position: Int) {
        popupMenuHelper.show(position)
    }
    //@ChatNodeAdapter
    override fun onEditNode(position: Int) {

    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun setupRecycler() {
        chatNodeAdapter = ChatNodeAdapter(chatNodes = mutableListOf(), this)
        chatNodeFooterAdapter = ChatNodeFooterAdapter()

        val chatNodeHeaderAdapter = ChatNodeHeaderAdapter(
            currentSystemMessage = chatTree.gptSettings.systemMessage,
            onSave = { newSystemMessage ->
                chatTreeViewModel.saveGptSettings(chatTree.gptSettings.apply { systemMessage = newSystemMessage })
            },
            onChange = { sysMsgHeight ->
                Log.d("TAG", "HEADER CHANGE")
                setupRecyclerHeaderScroll(sysMsgHeight)
            }
        )
        val concatAdapter = ConcatAdapter(
            ConcatAdapter.Config.Builder().setStableIdMode(ConcatAdapter.Config.StableIdMode.SHARED_STABLE_IDS).build(),
            chatNodeHeaderAdapter, chatNodeAdapter, chatNodeFooterAdapter
        )

        binding.chatNodeRecyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
                reverseLayout = false
            }
            adapter = concatAdapter
        }
        // need to not animate movement sometimes
        // binding.chatNodeRecyclerView.itemAnimator = object : DefaultItemAnimator() {
        //     override fun animateMove(holder: RecyclerView.ViewHolder?, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        //         return true
        //         // return super.animateMove(holder, fromX, fromY, toX, toY)
        //     }
        // }
        binding.chatNodeRecyclerView.itemAnimator = null

        // for ease of access and scrolling
        chatNodeAdapter.layoutManager = binding.chatNodeRecyclerView.layoutManager as LinearLayoutManager

        // fix item shows when keyboard opens/closes
        binding.chatNodeRecyclerView.addOnLayoutChangeListener(View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val deltaY = oldBottom - bottom
            val keyboardOpened = deltaY > 220 // could be input box
            val keyboardClosed = deltaY < -220

            if (deltaY == 0) {
                // keyboard didn't open, something else changed
                return@OnLayoutChangeListener
            }

            if (binding.promptInputEditText.hasFocus()) {
                // since I made this a bottom aligned list, it doesn't seem to need this.
            } else {
                // editing chatNode
                if (popupMenuHelper.currentPosition != null && keyboardOpened) {
                    // move top of inputs to right above keyboard
                    popupMenuHelper.scrollForKeyboardInput()
                } else if (keyboardClosed) {
                    // keep list in place while keyboard closes
                    popupMenuHelper.scrollForKeyboardClose(deltaY)
                }
            }
        })
    }

    /**
     * This makes for nicer scrolling when entering a long system message
     */
    private fun setupRecyclerHeaderScroll(sysMsgHeight : Int) {
        val totalChildrenHeight = (chatNodeAdapter.layoutManager.findFirstVisibleItemPosition()..chatNodeAdapter.layoutManager.findLastVisibleItemPosition())
            .mapNotNull { chatNodeAdapter.layoutManager.findViewByPosition(it) }
            .sumOf { it.height }
        val rcHeight = binding.chatNodeRecyclerView.height

        if (sysMsgHeight < rcHeight && totalChildrenHeight > rcHeight) {
            if (!binding.chatNodeRecyclerView.isComputingLayout) {
                chatNodeAdapter.layoutManager.stackFromEnd = false
                lifecycleScope.launch {
                    delay(200) // so if it goes to the next line it doesn't jump as much
                    chatNodeAdapter.layoutManager.stackFromEnd = true
                    binding.chatNodeRecyclerView.scrollToPosition(0)
                }
            }
        }
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
        // store anything that wasn't submitted before leaving the page
        val tempPrompt = binding.promptInputEditText.text.toString()
        if (tempPrompt != "" && tempPrompt != chatTree.tempPrompt) {
            chatTree.tempPrompt = tempPrompt
            chatTreeViewModel.saveChatTree(chatTree)
        }

        // Perform your action here before leaving the Fragment
    }

    override fun onPause() {
        super.onPause()
        // otherwise remains on screen on next fragment
        popupMenuHelper.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class PopupMenuHelper(private val fragment: ChatNodeListFragment, private val context: Context) {
        private val popupWindow: PopupWindow
        private val popupBinding: ChatNodeFloatingMenuBinding
        private var nodeItemBinding: ChatNodeItemBinding? = null
        private val openAnim: ValueAnimator
        private val closeAnim: ValueAnimator
        private var _currentPosition: Int? = null
        val currentPosition: Int? get() = _currentPosition

        init {
            val inflater = LayoutInflater.from(context)
            val popupView = inflater.inflate(R.layout.chat_node_floating_menu, null)
            popupBinding = ChatNodeFloatingMenuBinding.bind(popupView)

            popupWindow = PopupWindow(
                popupView,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            popupWindow.apply {
                contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                isAttachedInDecor = true
                // Set the popup to be focusable and dismiss when clicked outside
                // animationStyle = R.style.NodePopupAnimation
                // isFocusable = true
                // setBackgroundDrawable(null)
                // setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                update()
            }

            // Set the popup animation
            val constraintLayout = popupBinding.popupMenuContainer
            openAnim = ValueAnimator.ofInt(62.dpToPx(), popupWindow.contentView.measuredWidth).apply {
                duration = 500
                // interpolator = OvershootInterpolator()
                addUpdateListener { animation ->
                    val layoutParams = constraintLayout.layoutParams
                    layoutParams.width = animation.animatedValue as Int
                    constraintLayout.layoutParams = layoutParams
                }
                setInterpolator { input ->
                    // https://wiki.geogebra.org/en/FitPoly_Command
                    val xDouble = input.toDouble()
                    (152.08 * xDouble.pow(7) - 426.8 * xDouble.pow(6) + 426.47 * xDouble.pow(5) - 178.84 * xDouble.pow(4) + 26.86 * xDouble.pow(3) + 0.94 * xDouble.pow(2) + 0.29 * xDouble).toFloat()
                }
            }
            closeAnim = ValueAnimator.ofInt(popupWindow.contentView.measuredWidth, 62.dpToPx()).apply {
                duration = 400 // Adjust the duration to your preference
                interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    val layoutParams = constraintLayout.layoutParams
                    layoutParams.width = animation.animatedValue as Int
                    constraintLayout.layoutParams = layoutParams
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        popupWindow.dismiss()
                    }
                })
            }

            popupWindow.setOnDismissListener {
                nodeItemBinding!!.promptTextView.apply {
                    background = null
                    isEnabled = false
                }
                nodeItemBinding!!.responseTextView.apply {
                    background = null
                    isEnabled = false
                }
                if (currentPosition != null) {
                    val adapterPosition = currentPosition!! - 1
                    chatNodeAdapter.notifyItemChanged(adapterPosition)
                }

                //make main input visible and prevent list jump
                val yItemOffset = nodeItemBinding!!.root.top
                binding.inputContainer.visibility=View.VISIBLE
                chatNodeFooterAdapter.updateFooterHeight(0)
                chatNodeAdapter.layoutManager.scrollToPositionWithOffset(currentPosition!!, yItemOffset)

                // if (!dismissedByClick) {
                //     closePopupWindow(constraintLayout) {}
                // }

            }

            // Set onClickListeners for the popup buttons
            popupBinding.apply {

                nodeDelete.setOnClickListener {
                    // Handle next action
                    closeAnim.start()
                }

                nodeEdit.setOnClickListener {
                    // Handle next action
                    nodeItemBinding!!.promptTextView.apply {
                        setBackgroundResource(R.drawable.message_editable_bg)
                        isEnabled = true
                    }
                    nodeItemBinding!!.responseTextView.apply {
                        setBackgroundResource(R.drawable.message_editable_bg)
                        isEnabled = true
                    }

                    // binding.inputContainer.animate().alpha(0F).setDuration(400).withEndAction{binding.inputContainer.visibility=View.INVISIBLE}.start()
                    val yItemOffset = nodeItemBinding!!.root.top
                    binding.inputContainer.visibility = View.GONE
                    chatNodeFooterAdapter.updateFooterHeight(binding.inputContainer.height)
                    chatNodeAdapter.layoutManager.scrollToPositionWithOffset(
                        currentPosition!!,
                        yItemOffset
                    )

                }

                nodePrev.setOnClickListener {
                }

                nodeNext.setOnClickListener {
                    binding.chatNodeRecyclerView.smoothScrollToBottom(currentPosition!!)
                }

                nodeRegenerate.setOnClickListener {
                }
            }

            // adjust height of input box by dragging on it for testing purposes
            // var initialY = 0f
            // var initialHeight = 0
            binding.promptInputEditText.setOnTouchListener { view, event ->
            //     when (event.action) {
            //         MotionEvent.ACTION_DOWN -> {
            //             // User started touching the view
            //             initialY = event.rawY
            //             initialHeight = view.layoutParams.height
            //         }
            //         MotionEvent.ACTION_MOVE -> {
            //             // User is moving their finger on the view
            //             val deltaY = initialY - event.rawY
            //             val newHeight = (initialHeight + deltaY).toInt()
            //             view.layoutParams.height = newHeight.coerceAtLeast(200) // Make sure the new height is not negative
            //             view.requestLayout()
            //         }
            //     }
                false
            }

        }

        fun show(position: Int) {

            if (popupWindow.isShowing && position != _currentPosition) {
                // dismiss before nodeItemBinding is changed
                popupWindow.dismiss()
            }

            val viewHolder = binding.chatNodeRecyclerView.findViewHolderForAdapterPosition(position) as ChatNodeAdapter.ChatNodeViewHolder
            nodeItemBinding = viewHolder.binding
            val anchorView = nodeItemBinding!!.chatNodeTopGuide

            anchorView.let {
                _currentPosition = position

                // Show the popup below the anchor view
                val xOffset = nodeItemBinding!!.promptHolder.left
                val yOffset = -(anchorView.height + popupWindow.contentView.measuredHeight + 10)

                if (!popupWindow.isShowing) {
                    openAnim.start()
                    popupWindow.showAsDropDown(anchorView, xOffset, yOffset)
                }

            }
        }

        fun dismiss() {
            if (popupWindow.isShowing) {
                popupWindow.dismiss()
            }
        }

        /**
         * Want to scroll to line up input if keyboard is opened
         * Want to let android handle the scrolling itself if the inputs are larger than the recyclerView
         */
        fun scrollForKeyboardInput() {
            if (nodeItemBinding != null && currentPosition != null) {
                val nodeHeight = (nodeItemBinding?.promptHolder?.height ?: 0) + (nodeItemBinding?.responseHolder?.height ?: 0)
                if (nodeHeight < binding.chatNodeRecyclerView.height) {
                    binding.chatNodeRecyclerView.smoothScrollToBottom(popupMenuHelper.currentPosition!!, 100F)
                }
            }
        }

        /**
         * If too close to the top, it shouldn't scroll down
         */
        fun scrollForKeyboardClose(delta: Int) {
            if ((currentPosition?:0) > 4)
                binding.chatNodeRecyclerView.scrollBy(0, -delta)
        }

        private fun Int.dpToPx(): Int {
            val density = context.resources.displayMetrics.density
            return (this * density).toInt()
        }
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
            // callback will change SendToStop into LoadingStop after animation is complete
            val animationCallback = object : Animatable2Compat.AnimationCallback() {
                // after button is animated to a stop, switch it to the loading stop button
                override fun onAnimationEnd(drawable: Drawable) {
                    super.onAnimationEnd(drawable)
                    if (chatNodeViewModel.isRequestActive(chatTree.id)) {
                        // sometimes if the request is too fast, it will switch
                        // back to 'send' before the animation
                        // but this call back triggers after it's already be set to send
                        binding.chatSubmitButton.icon = drawableLoadingStop
                        lastSet = Method.LOAD
                        drawableLoadingStop.start()
                    }
                }
            }
            AnimatedVectorDrawableCompat.registerAnimationCallback(drawableSendToStop, animationCallback)

            binding.chatSubmitButton.setOnClickListener {
                if (!chatNodeViewModel.isRequestActive(chatTree.id)) {
                    // don't try to submit till chatNodes loaded
                    if (chatNodeAdapter.isInit()) {
                        // create a new request
                        prepareChatRequest()
                    }
                } else {
                    // cancel active request
                    cancelChatRequest()
                }
                triggerTemporalDisabled()
            }

            setupDisableButtonOnNoPrompt()
        }

        fun checkSubmitStatusButton() {
            if (chatNodeViewModel.isRequestActive(chatTree.id)) {
                markSubmitStoppable()
            } else {
                markSubmitSendable()
            }
        }

        private fun setupDisableButtonOnNoPrompt() {

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

            // allow sending spaces, but not empty returns
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

    private fun RecyclerView.smoothScrollToBottom(toPos: Int, speed: Float = 25F, offset: Int? = null) {
        val smoothScroller: SmoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_END
            }
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                return speed / displayMetrics!!.densityDpi
            }
            // override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
            //     // return boxStart - viewStart + offset
            //     return super.calculateDtToFit(viewStart, viewEnd, boxStart, boxEnd, snapPreference)
            // }
        }
        smoothScroller.targetPosition = toPos
        layoutManager?.startSmoothScroll(smoothScroller)
    }

}
