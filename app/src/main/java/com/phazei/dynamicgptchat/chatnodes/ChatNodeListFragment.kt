package com.phazei.dynamicgptchat.chatnodes

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.chattrees.ChatTreeViewModel
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.data.entity.ChatTree
import com.phazei.dynamicgptchat.databinding.ChatNodeFloatingMenuBinding
import com.phazei.dynamicgptchat.databinding.FragmentChatNodeListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
@Suppress("LiftReturnOrAssignment")
@AndroidEntryPoint
class ChatNodeListFragment() : Fragment(), ChatNodeAdapter.OnNodeActionListener, ChatTreeOptionsDialog.ChatTreeOptionsClickListener {

    private var _binding: FragmentChatNodeListBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val chatNodeViewModel: ChatNodeViewModel by activityViewModels()
    private val chatTreeViewModel: ChatTreeViewModel by viewModels()

    override val isActiveRequest = MutableLiveData(false)
    override lateinit var isActiveRequestLifecycleOwner: LifecycleOwner

    private lateinit var chatNodeAdapter: ChatNodeAdapter
    private var actionMode : ActionMode? = null
    private lateinit var tracker: SelectionTracker<Long>

    private lateinit var chatNodeFooterAdapter: ChatNodeFooterAdapter
    private lateinit var chatSubmitButtonHelper: ChatSubmitButtonHelper
    private lateinit var popupMenuHelper: PopupMenuHelper
    private lateinit var settingsDialogHelper: SettingsDialogHelper
    private lateinit var chatTree: ChatTree
    private val dispatcher by lazy { requireActivity().onBackPressedDispatcher }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        isActiveRequestLifecycleOwner = viewLifecycleOwner
        _binding = FragmentChatNodeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatTree = sharedViewModel.activeChatTree!!
        chatSubmitButtonHelper = ChatSubmitButtonHelper(this)
        popupMenuHelper = PopupMenuHelper(this, requireContext())
        settingsDialogHelper = SettingsDialogHelper()

        viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                chatNodeViewModel.activeBranchUpdate
                    .filterNotNull()
                    // when an request completes, it could be from a background chatTree and not the
                    // currently active one, so we don't need to process it and should just leave
                    .filter { (updatedChatNode, _) -> updatedChatNode.chatTreeId == chatTree.id }
                    // .conflate() //conflate could skip items if other trees are triggering here
                    .collect { (updatedChatNode, activeBranch) ->
                        val isInit = chatNodeAdapter.isInit()
                        if (!isInit && activeBranch == null) {
                            // if an individual request completes before the view loads, this will trigger right away
                            // so shouldn't be loaded yet
                            return@collect
                        }
                        if (activeBranch == null) {
                            // single node update
                            chatNodeAdapter.updateItem(updatedChatNode)
                            settingsDialogHelper.updateTokenTotals()
                        } else {
                            // branch update
                            chatNodeAdapter.updateData(updatedChatNode, activeBranch)
                            // needs to scroll sooner than it does after adapter binding calls popup
                            popupMenuHelper.scrollForNodeCycling()
                        }
                        val position: Int
                        if (updatedChatNode.parentNodeId == null && !isInit) { // rootNode
                            // rootNode should scroll to bottom on first load only
                            // first childNode could be cycling, so this shouldn't be triggered for that
                            position = chatNodeAdapter.itemCount - 1 // get leaf item
                            binding.chatNodeRecyclerView.scrollToPosition(position + 1) //+1 for header
                        } else {
                            // position = chatNodeAdapter.getItemPosition(updatedChatNode) // stay by edited item
                        }
                    }
            }
            launch {
                chatNodeViewModel.titleUpdate.filterNotNull().collect {
                    (activity as AppCompatActivity).supportActionBar?.title = chatTree.title
                }
            }
        }}
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
            isActiveRequest.value = chatNodeViewModel.isRequestActive(chatTree.id)
        }

        /**
         * OnBackPressedDispatcher
         */
        dispatcher.addCallback(viewLifecycleOwner) {
            if (settingsDialogHelper.sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                settingsDialogHelper.sheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            } else {
                // ensure that the temporary text typed into the prompt box is saved before leaving the page
                saveTempPrompt()
                this.isEnabled = false
                dispatcher.onBackPressed()
            }
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
    private fun editAddChatRequest(editedNode: ChatNode, prompt: String) {
        val parentLeaf = editedNode.parent
        val newNode = ChatNode(0, chatTree.id, parentLeaf.id, prompt)
        newNode.parent = parentLeaf

        chatNodeAdapter.addItem(parentLeaf, newNode)
        chatNodeViewModel.makeChatCompletionRequest(chatTree, newNode)
    }

    private fun cancelChatRequest() {
        chatNodeViewModel.cancelRequest(chatTree.id)
    }

    // @ChatNodeAdapter
    override fun onNodeSelected() {
        popupMenuHelper.stateManagement()
    }
    // @ChatNodeAdapter
    override fun onEditNode(position: Int) {

    }

    // @ChatTreeOptionsDialog
    override fun onSaveOptions() {
    }
    // @ChatTreeOptionsDialog
    override fun onChangeKeyboardEnter() {
        chatSubmitButtonHelper.setKeyboardIMEOption()
    }
    override fun onChangeResponseWrap() {
        // need to update entire list if this changes
        chatNodeAdapter.notifyDataSetChanged()
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun setupRecycler() {
        chatNodeAdapter = ChatNodeAdapter(chatNodes = mutableListOf(), this)
        chatNodeFooterAdapter = ChatNodeFooterAdapter()

        val chatNodeHeaderAdapter = ChatNodeHeaderAdapter(
            fragmentManager = childFragmentManager,
            currentSystemMessage = chatTree.gptSettings.systemMessage,
            onSave = { newSystemMessage ->
                chatTreeViewModel.saveGptSettings(chatTree.gptSettings.apply { systemMessage = newSystemMessage })
            },
            onChange = { sysMsgHeight ->
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
            recycledViewPool.setMaxRecycledViews(ChatNodeAdapter.ITEM_TYPE_ACTIVE, 0)
            itemAnimator = null // disable all animations
        }

        // Initialize SelectionTracker
        tracker = SelectionTracker.Builder<Long>(
            "mySelection",
            binding.chatNodeRecyclerView,
            StableIdKeyProvider(binding.chatNodeRecyclerView),
            ChatNodeAdapter.ItemsDetailsLookup(binding.chatNodeRecyclerView),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()

        tracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                super.onSelectionChanged()
                // don't want to include the #0 root Node in the selected count
                val itemsSelected = tracker.selection.size() - 1
                actionMode?.apply {
                    if (itemsSelected >= 0) {
                        title = getString(R.string.title_items_selected, itemsSelected)
                    } else if (actionModeCallback.simpleClear) {
                        // when items are manually cleared (via select all toggle) this is triggered
                        // right before the #0 root Node is re-added
                        actionModeCallback.simpleClear = false
                    } else {
                        finish()
                    }
                }
            }
        })

        chatNodeAdapter.tracker = tracker
        chatNodeHeaderAdapter.tracker = tracker
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
                if (popupMenuHelper.getActiveScrollPosition() != null && keyboardOpened) {
                    // move top of inputs to right above keyboard
                    popupMenuHelper.scrollForKeyboardInput()
                } else if (keyboardClosed) {
                    // keep list in place while keyboard closes
                    popupMenuHelper.scrollForKeyboardClose(deltaY)
                }
            }
        })
    }

    private val actionModeCallback = object : ActionMode.Callback {
        var simpleClear = false

        override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
            mode.menuInflater.inflate(R.menu.chat_copy_action_mode, menu)
            return true
        }
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            popupMenuHelper.deactivate()
            chatSubmitButtonHelper.setSubmitEnabled(false)
            settingsDialogHelper.sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            requireActivity().currentFocus?.clearFocus()
            chatNodeAdapter.notifyDataSetChanged() //to remove all menu buttons

            //hide keyboard
            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(binding.promptInputEditText.windowToken, 0)

            mode?.title = getString(R.string.title_items_selected, 0)

            // tracker only enabled when at least 1 item is selected
            // in the case of chatNodes, long press will be reserved for other functions, so can't utilize
            // long click to init first selected item, will start with 0 selected.
            // to work around this issue, the root chatNode which isn't displayed but does exist in the list
            // will be used as the first selected item and always maintain it's selection
            tracker.setItemsSelected(listOf(chatNodeAdapter.getItem(0).id), true)

            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_select_all -> {
                    // allow select all to toggle selection
                    if (tracker.selection.size() >= chatNodeAdapter.itemCount) {
                        simpleClear = true
                        tracker.clearSelection()
                        // must have item #0 (root node) selected to maintain active tracker
                        tracker.setItemsSelected(listOf(chatNodeAdapter.getItem(0).id), true)
                    } else {
                        tracker.setItemsSelected(chatNodeAdapter.getAllItems().map { it.id }, true)
                    }
                    true
                }
                R.id.action_copy -> {
                    copyChatNodesToClipboard()
                    mode.finish()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            chatSubmitButtonHelper.setSubmitEnabled(true)
            actionMode = null // this is mostly to be able to track if it's enabled or not
            chatNodeAdapter.notifyDataSetChanged() //to add all menu buttons

            // must be cleared to disable tracker
            tracker.clearSelection()
        }

        private fun copyChatNodesToClipboard() {
            val stringBuilder = StringBuilder()

            //need the list to be in the same order as they appear in recyclerView, not in order selected
            val orderedSelection = chatNodeAdapter.getAllItems().filter { tracker.isSelected(it.id) }

            if (tracker.isSelected(ChatNodeHeaderAdapter.ITEM_ID)) {
                stringBuilder.append("System: ${chatTree.gptSettings.systemMessage}\n\n")
            }

            for (chatNode in orderedSelection) {
                if (!chatNode.parentInitialized()) continue // I am root
                stringBuilder.append("User: ${chatNode.prompt}\n")
                stringBuilder.append("Assistant: ${chatNode.response}\n\n")
            }
            val string = stringBuilder.toString()
            if (string.isNotEmpty()) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("ChatNodes", string)
                clipboard.setPrimaryClip(clipData)
                Snackbar.make(binding.root, "Copied to clipboard", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * This makes for nicer scrolling when entering a long system message
     */
    private fun setupRecyclerHeaderScroll(sysMsgHeight : Int) {
        val llm = binding.chatNodeRecyclerView.layoutManager as LinearLayoutManager
        val totalChildrenHeight = (llm.findFirstVisibleItemPosition()..llm.findLastVisibleItemPosition())
            .mapNotNull { llm.findViewByPosition(it) }
            .sumOf { it.height }
        val rcHeight = binding.chatNodeRecyclerView.height

        if (sysMsgHeight < rcHeight && totalChildrenHeight > rcHeight) {
            if (!binding.chatNodeRecyclerView.isComputingLayout) {
                llm.stackFromEnd = false
                lifecycleScope.launch {
                    delay(200) // so if it goes to the next line it doesn't jump as much
                    llm.stackFromEnd = true
                    binding.chatNodeRecyclerView.scrollToPosition(0)
                }
            }
        }
    }

    private fun setupMenu() {
        (activity as AppCompatActivity).supportActionBar?.apply {
            title = chatTree.title
        }
        setActionBarTitleAsMarquee()
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
                    R.id.chat_copy -> {
                        actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
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

    private fun getTitleTextView(): TextView {
        val v: View = requireActivity().window.decorView
        val toolbar = v.findViewById<View>(R.id.toolbar) as Toolbar
        // might move in the future, but for now it's great
        return toolbar.getChildAt(0) as TextView
    }
    private fun setActionBarTitleAsMarquee() {
        val titleText = getTitleTextView()
        val textSize = resources.getDimension(androidx.appcompat.R.dimen.abc_text_size_medium_material)
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)

        titleText.marqueeRepeatLimit = -1
        titleText.ellipsize = TextUtils.TruncateAt.MARQUEE
        titleText.isSelected = true
    }
    private fun setActionBarTitleReset() {
        val titleText = getTitleTextView()
        val textSize = resources.getDimension(androidx.appcompat.R.dimen.abc_text_size_large_material)
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)

        titleText.ellipsize = TextUtils.TruncateAt.END
        titleText.isSelected = false
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
        setActionBarTitleReset()
    }

    override fun onResume() {
        super.onResume()

        popupMenuHelper.stateManagement()
        setActionBarTitleAsMarquee()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class SettingsDialogHelper() {
        val sheetBehavior: BottomSheetBehavior<View>
        private val chatOptionsDialogView: FragmentContainerView
        private val chatOptionsDialog: ChatTreeOptionsDialog

        init {
            chatOptionsDialogView = binding.chatOptionsDialogView
            sheetBehavior = BottomSheetBehavior.from(chatOptionsDialogView)
            chatOptionsDialog = childFragmentManager.findFragmentById(R.id.chat_options_dialog_view) as ChatTreeOptionsDialog
            chatOptionsDialog.setListener(this@ChatNodeListFragment)

            // Slide the prompt input up when opening options, makes it appear as one item
            val layoutParams = binding.inputContainer.layoutParams as ViewGroup.MarginLayoutParams
            sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {}
                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                    // slideOffset is a value between 0 (collapsed) and 1 (expanded)
                    // If bottomSheet's height is MATCH_PARENT, slideOffset directly matches the fraction of the screen height the bottomSheet takes up.

                    val maxPossibleHeight = bottomSheet.height - 20
                    layoutParams.bottomMargin = (slideOffset * maxPossibleHeight).toInt()
                    binding.promptInputEditText.requestLayout()
                }
            })

            setupInputGesture()
        }

        /**
         * Allow opening of dialog by swiping up and down on the text input
         */
        @SuppressLint("ClickableViewAccessibility")
        private fun setupInputGesture() {
            var initialY = 0f
            val threshold = 30f  // Set the swipe threshold

            binding.promptInputEditText.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // record the y-coordinate of the initial touch point
                        initialY = event.rawY
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        false // not consuming the event
                    }
                    MotionEvent.ACTION_UP -> {
                        // calculate the distance swiped
                        val finalY = event.rawY
                        val deltaY = finalY - initialY

                        if (deltaY < -threshold) {
                            // User swiped up, so expand the bottom sheet
                            if (sheetBehavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                                sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                            } else if (sheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                                sheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                            }
                            v.parent.requestDisallowInterceptTouchEvent(false)
                            true // consume the event
                        } else if (deltaY > threshold) {
                            // User swiped down, so collapse the bottom sheet
                            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                                sheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                            } else {
                                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                            }
                            v.parent.requestDisallowInterceptTouchEvent(false)
                            true // consume the event
                        } else {
                            v.parent.requestDisallowInterceptTouchEvent(false)
                            false // not consuming the event
                        }
                    }
                    else -> {
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        false // not consuming the event
                    }
                }
            }
        }

        fun updateTokenTotals() {
            chatOptionsDialog.updateTokenTotals()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class PopupMenuHelper(private val fragment: ChatNodeListFragment, private val context: Context) {
        private val popupWindow: PopupWindow
        private val popupBinding: ChatNodeFloatingMenuBinding

        private var activeHolder: ChatNodeAdapter.ChatNodeViewHolder? = null
        private var previousHolder: ChatNodeAdapter.ChatNodeViewHolder? = null
        private var previousPosition: Int? = null
        private var activePosition: Int? = null
        private var activeScrollPosition: Int? = null
        private var isCyclingChildren: Boolean = false
        private var cyclingPrevYOffset: Int = 0

        private val widthClosed = 62.dpToPx()
        private val widthOpened = 250.dpToPx()
        private val widthEdit = 156.dpToPx()

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
                isAttachedInDecor = true
                elevation = 50F
                // Set the popup to be focusable and dismiss when clicked outside
                // animationStyle = R.style.NodePopupAnimation
                // isFocusable = true
                // setBackgroundDrawable(null)
                // setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                update()
            }
            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            // popupWindow.contentView == popupView == popupBinding.root

            // hide and show when item is scrolled outside of recycler
            val fadePopupAnimator = ObjectAnimator.ofFloat(popupView, View.ALPHA, 0F).apply {
                duration = 250
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        popupView.visibility = View.INVISIBLE
                    }
                })
            }
            val unfadePopupAnimator = ObjectAnimator.ofFloat(popupView, View.ALPHA, 1F).apply {
                duration = 250
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        popupView.visibility = View.VISIBLE
                    }
                })
            }
            binding.chatNodeRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (!popupWindow.isShowing || activePosition == null) {
                        return
                    }
                    // Get the first and last visible positions of items in the RecyclerView
                    val firstVisiblePosition = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    val lastVisiblePosition = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

                    // Check if the item associated with the popup is visible on the screen
                    if (activeScrollPosition!! in firstVisiblePosition..lastVisiblePosition) {
                        // make sure it's shown
                        if (fadePopupAnimator.isRunning) { fadePopupAnimator.cancel() }
                        if (!unfadePopupAnimator.isRunning && popupView.alpha != 1F) { unfadePopupAnimator.start() }
                    } else {
                        // hide it
                        if (unfadePopupAnimator.isRunning) { unfadePopupAnimator.cancel() }
                        if (!fadePopupAnimator.isRunning && popupView.alpha != 0F) { fadePopupAnimator.start() }
                    }
                }
            })

            setupListeners()
        }

        private fun setupListeners() {

            popupWindow.setOnDismissListener {
            }

            // Set onClickListeners for the popup buttons
            popupBinding.apply {

                nodeDelete.setOnClickListener {
                    // remove item from chatNode.parent.children.  Check if activeChild
                    activePosition?.let { activePos ->
                        activeHolder?.let { holder ->
                            val chatNode = chatNodeAdapter.getItem(activePos)
                            val isLastChild = chatNode.parent.children.size <= 1
                            if (!isLastChild) {
                                // this is essentially the same as cycling until there are no more nodes left
                                isCyclingChildren = true
                                cyclingPrevYOffset = holder.binding.root.top
                            }
                            chatNodeViewModel.deleteChildNode(chatNode)
                            // It's possible this is the last child of the parent being deleted, in which case there will be no more
                            // activeHolder.  Otherwise it could be the last holder and finish will not close the popup Menu
                            if (isLastChild) {
                                finish()
                            }
                        }
                    }
                }

                nodeEdit.setOnClickListener {
                    enableEdit()
                }

                nodePrev.setOnClickListener {
                    activePosition?.let { activePos ->
                        activeHolder?.let { holder ->
                            val chatNode = chatNodeAdapter.getItem(activePos)
                            val prevNode = chatNode.getPrevSibling()
                            chatNode.parent.activeChildIndex = chatNode.parent.children.indexOf(prevNode)
                            if (prevNode != null) {
                                isCyclingChildren = true
                                cyclingPrevYOffset = holder.binding.root.top
                                chatNodeViewModel.updateActiveSibling(prevNode)

                                // After this is emitted, the activeNodePosition remains active and maintains the same
                                //  value because it's in the same position.  The ViewHolder does change, so the
                                //  popupWindow needs to be reattached.

                            }
                        }
                    }
                }

                nodeNext.setOnClickListener {
                    activePosition?.let { activePos ->
                        activeHolder?.let { holder ->
                            val chatNode = chatNodeAdapter.getItem(activePos)
                            val nextNode = chatNode.getNextSibling()
                            if (nextNode != null) {
                                isCyclingChildren = true
                                cyclingPrevYOffset = holder.binding.root.top
                                chatNodeViewModel.updateActiveSibling(nextNode)
                            }
                        }
                    }
                }

                nodeRegenerate.setOnClickListener {
                    activePosition?.let { activePos ->
                        val chatNode = chatNodeAdapter.getItem(activePos)
                        chatNodeViewModel.makeChatCompletionRequest(chatTree, chatNode)
                        finish()
                    }
                }

                nodeEditAdd.setOnClickListener {
                    activePosition?.let { activePos ->
                        activeHolder?.let { holder ->
                            val chatNode = chatNodeAdapter.getItem(activePos)
                            editAddChatRequest(chatNode, holder.binding.promptTextEdit.text.toString())

                            // close menu
                            finish(true)
                        }
                    }
                }

                nodeEditReplace.setOnClickListener {
                    // simply saves data to the chat node
                    activePosition?.let { activePos ->
                        activeHolder?.let { holder ->
                            val chatNode = chatNodeAdapter.getItem(activePos)
                            chatNode.prompt = holder.binding.promptTextEdit.text.toString()
                            chatNode.response = holder.binding.responseTextEdit.text.toString()
                            chatNodeViewModel.saveChatNode(chatNode)

                            // keep menu open and refresh its data
                            disableEdit(true)
                        }
                    }
                }

                nodeEditCancel.setOnClickListener {
                    disableEdit(true)
                }
            }
        }

        private fun enableEdit() {

            activeHolder!!.enableEdit()

            // update menu buttons to edit menu
            listOf(popupBinding.nodeEditAdd, popupBinding.nodeEditReplace, popupBinding.nodeEditCancel)
                .forEach { showButtonAnimation(it, -1) }
            listOf(popupBinding.nodePrev, popupBinding.nodeNext, popupBinding.nodeEdit, popupBinding.nodeRegenerate, popupBinding.nodeDelete)
                .forEach { hideButtonAnimation(it, -1) }
            resizeAnimator(widthOpened, widthEdit).start()

            // hide main prompt input
            val yItemOffset = activeHolder!!.binding.root.top
            binding.inputContainer.visibility = View.GONE
            chatNodeFooterAdapter.updateFooterHeight(binding.inputContainer.height)
            chatNodeAdapter.layoutManager.scrollToPositionWithOffset(
                activeScrollPosition!!,
                yItemOffset
            )
        }

        private fun disableEdit(notify: Boolean = false) {

            if (previousHolder != null && previousHolder != activeHolder) {
                previousHolder?.disableEdit()
            }
            activeHolder?.disableEdit(notify)

            if (binding.inputContainer.visibility==View.VISIBLE) {
                // not enabled so skip animations
                return
            }

            // update menu buttons back to default
            listOf(popupBinding.nodePrev, popupBinding.nodeNext, popupBinding.nodeEdit, popupBinding.nodeRegenerate, popupBinding.nodeDelete)
                .forEach { showButtonAnimation(it) }
            listOf(popupBinding.nodeEditAdd, popupBinding.nodeEditReplace, popupBinding.nodeEditCancel)
                .forEach { hideButtonAnimation(it) }
            resizeAnimator(widthEdit, widthOpened).start()

            // hide main prompt input
            var yItemOffset = 0
            if (activeHolder != null) {
                yItemOffset = activeHolder!!.binding.root.top
            } else if (previousHolder != null) {
                // if activeHolder was null, could be resetting for finish
                yItemOffset = previousHolder!!.binding.root.top
            }
            binding.inputContainer.visibility=View.VISIBLE
            chatNodeFooterAdapter.updateFooterHeight(0)
            chatNodeAdapter.layoutManager.scrollToPositionWithOffset(activeScrollPosition!!, yItemOffset)
        }

        /**
         * This can get called anytime when the adapter is re-bind
         *
         * Case 1: Menu item tapped first time
         * Case 2: Menu item tapped when open
         * Case 3: Different menu item tapped when open
         */
        fun stateManagement() {
            var showPopup = false
            // if the user didn't click anything specifically, this should determine what the state should be
            previousPosition = activePosition
            previousHolder = activeHolder

            activePosition = chatNodeAdapter.activeNodePosition
            activeHolder = null

            // menu has been closed
            if (activePosition == null) {
                if (popupWindow.isShowing) {
                    if (previousHolder != null) {
                        finish()
                    } else {
                        dismiss()
                    }
                }
            }

            // something should be open
            if (activePosition != null) {

                activeScrollPosition = activePosition?.plus(1)
                val findHolder = binding.chatNodeRecyclerView.findViewHolderForAdapterPosition(activeScrollPosition!!)
                if (findHolder != null) {
                    activeHolder = findHolder as ChatNodeAdapter.ChatNodeViewHolder
                } else {
                    Log.w(
                        "Warning",
                        "chatNodeRecyclerView.findViewHolderForAdapterPosition returned null for $activeScrollPosition"
                    )
                    chatNodeAdapter.notifyItemChanged(activePosition!!)
                    return
                }

                // nothing has changed, should stay the same
                if (activePosition == previousPosition) {
                    if (popupWindow.isShowing) {
                        // could be cycling
                        if (isCyclingChildren) {
                            showPopup = true
                        }
                    } else {
                        showPopup = true
                    }
                } else if (previousPosition != null) {
                    if (popupWindow.isShowing) {
                        // clicked new menu while already open on another item
                        finish()
                    }
                    showPopup = true
                } else if (previousPosition == null) {
                    // first time opened
                    showPopup = true
                }
            }

            previousPosition = null
            previousHolder = null

            if (showPopup) {
                show()
                return
            }
        }

        private fun show() {
            val anchorView = activeHolder!!.binding.chatNodeTopGuide

            // Show the popup below the anchor view
            val xOffset = activeHolder!!.binding.promptHolder.left
            val yOffset = -(anchorView.height + popupWindow.contentView.measuredHeight + 10)

            if (!popupWindow.isShowing) {
                val width = if (chatNodeAdapter.isEditingActive) widthEdit else widthOpened
                resizeAnimator(widthClosed, width, bounce = true,
                    onStart = {
                        popupWindow.contentView.alpha = 1F
                        popupWindow.contentView.visibility = View.VISIBLE
                    }
                ).start()
                popupWindow.showAsDropDown(anchorView, xOffset, yOffset)
            } else if (isCyclingChildren) {
                scrollForNodeCycling()
                popupWindow.animationStyle = 0 // disable animation
                popupWindow.dismiss()
                popupWindow.showAsDropDown(anchorView, xOffset, yOffset)
                lifecycleScope.launch { delay(500)
                    popupWindow.animationStyle = -1 // default animation
                }
                isCyclingChildren = false
                cyclingPrevYOffset = 0
            }
        }

        fun dismiss() {
            popupWindow.dismiss()
        }

        fun deactivate() {
            finish()
        }

        /**
         * This will complete the menu action resetting the menu and clearing active
         * Use at end of: save update/save new/cancel edit/delete
         */
        private fun finish(force: Boolean = false) {
            val quickDismiss: Boolean

            if (chatNodeAdapter.isEditingActive) {
                disableEdit()
            }

            if (previousHolder != null) {
                if (previousHolder != activeHolder) {
                    // very likely the holder is reused
                    previousHolder?.deactivate(force)
                }
                quickDismiss = activeHolder != null
            } else {
                activeHolder?.deactivate(force)
                activeHolder = null
                activePosition = null
                activeScrollPosition = null
                quickDismiss = false
            }
            if (quickDismiss) {
                popupWindow.dismiss()
            } else {
                resizeAnimator(popupWindow.contentView.width, widthClosed, onEnd = {
                    popupWindow.dismiss()
                }).start()
            }

        }

        fun getActiveScrollPosition(): Int? {
            return activeScrollPosition
        }

        fun scrollForNodeCycling() {
            if (cyclingPrevYOffset > 0) {
                chatNodeAdapter.layoutManager.scrollToPositionWithOffset(activeScrollPosition!!, cyclingPrevYOffset)
            }
        }

        /**
         * Want to scroll to line up input if keyboard is opened
         * Want to let android handle the scrolling itself if the inputs are larger than the recyclerView
         */
        fun scrollForKeyboardInput() {
            if (activeHolder != null && activePosition != null) {
                activeHolder!!.binding.apply {
                    val nodeHeight = promptHolder.height + responseHolder.height
                    if (nodeHeight < binding.chatNodeRecyclerView.height) {
                        binding.chatNodeRecyclerView.smoothScrollToBottom(activeScrollPosition!!, 100F)
                    }
                }
            }
        }

        /**
         * If too close to the top, it shouldn't scroll down
         */
        fun scrollForKeyboardClose(delta: Int) {
            if ((activeScrollPosition ?: 0) > 4)
                binding.chatNodeRecyclerView.scrollBy(0, -delta)
        }

        /**
         * Helper methods:
         */

        private fun hideButtonAnimation(view: View, spinDirection: Int = 1) {
            view.alpha = 1F
            view.rotation = 0F
            view.animate().rotation(spinDirection * 360F).alpha(0F).setDuration(400).withEndAction{view.visibility=View.GONE}.start()
        }

        private fun showButtonAnimation(view: View, spinDirection: Int = 1) {
            view.alpha = 0F
            view.rotation = 0F
            view.alpha=0F; view.animate().rotation(spinDirection * 360F).alpha(1F).setDuration(400).withStartAction{view.visibility=View.VISIBLE}.start()
        }

        private fun resizeAnimator(startWidth: Int, endWidth: Int, onEnd: () -> Unit = {}, onStart: () -> Unit = {}, bounce: Boolean = false): ValueAnimator {
            val constraintLayout = popupBinding.popupMenuContainer
            val path = Path().apply {
                moveTo(0f, 0f)
                cubicTo(0.4f, 0.1f, 0.36f, 1.1f, 0.49f, 1.14f)
                cubicTo(0.58f, 1.17f, 0.63f, 0.82f, 0.75f, 0.83f)
                cubicTo(0.86f, 0.84f, 0.83f, 0.97f, 1f, 1f)
            }
            return ValueAnimator.ofInt(startWidth, endWidth).apply {
                duration = 400 *  (if (bounce) 2L else 1L)
                if (bounce) {
                    interpolator = PathInterpolator(path)
                } else {
                    interpolator = LinearInterpolator()
                }
                addUpdateListener { animation ->
                    val layoutParams = constraintLayout.layoutParams
                    layoutParams.width = animation.animatedValue as Int
                    constraintLayout.layoutParams = layoutParams
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onEnd()
                    }
                    override fun onAnimationStart(animation: Animator) {
                        onStart()
                    }
                })
            }
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
        private var manualDisabled = false

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
                if (!binding.chatSubmitButton.isEnabled) {
                    // even if button is disabled, could be triggered via keyboard
                    return@setOnClickListener
                }

                if (!chatNodeViewModel.isRequestActive(chatTree.id)) {
                    // don't try to submit till chatNodes loaded
                    if (chatNodeAdapter.isInit()) {
                        popupMenuHelper.deactivate()

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
            setupKeyboardIMESubmit()
            setKeyboardIMEOption()
        }

        private fun setupKeyboardIMESubmit() {
            binding.promptInputEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (!chatNodeViewModel.isRequestActive(chatTree.id)) {
                        // don't want to accidentally cancel
                        binding.chatSubmitButton.performClick()
                    } else {
                        Snackbar.make(binding.root,"Can't send! Request on the line.", Snackbar.LENGTH_SHORT).show()
                    }
                    true
                } else {
                    false
                }
            }
        }
        fun setKeyboardIMEOption() {
            if (chatTree.options.imeSubmit) {
                binding.promptInputEditText.imeOptions = EditorInfo.IME_ACTION_SEND
                binding.promptInputEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
            } else {
                binding.promptInputEditText.imeOptions = EditorInfo.IME_NULL
                binding.promptInputEditText.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
            }
            // only way to update button is to move focus to another item and come back, clearFocus and closing keyboard doesn't work
            binding.refreshFocus.visibility = View.VISIBLE
            binding.refreshFocus.requestFocus()
            lifecycleScope.launch {
                delay(100)
                binding.promptInputEditText.requestFocus()
                binding.refreshFocus.visibility = View.GONE
            }
            // To hide a keyboard:
            // val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            // inputMethodManager.hideSoftInputFromWindow(binding.promptInputEditText.windowToken, 0)


        }

        fun setSubmitEnabled(isEnabled: Boolean) {
            manualDisabled = !isEnabled
            checkSubmitStatusButton()
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
            val isEnabled = !empty && !temporalDisabled && !manualDisabled
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
