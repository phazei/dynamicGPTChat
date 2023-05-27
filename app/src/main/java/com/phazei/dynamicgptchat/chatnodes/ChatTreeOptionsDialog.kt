package com.phazei.dynamicgptchat.chatnodes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.ViewTreeObserver
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.data.pojo.ChatTreeOptions
import com.phazei.dynamicgptchat.databinding.DialogChatTreeOptionsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatTreeOptionsDialog : Fragment() {

    private var _binding: DialogChatTreeOptionsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var sheetBehavior: BottomSheetBehavior<View>
    private lateinit var listener: ChatTreeOptionsClickListener
    private lateinit var parent: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogChatTreeOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sheetBehavior = BottomSheetBehavior.from(binding.root.parent as View)
        setupSheet()
        setupInputs()
        setupInputListeners()
        updateTokenTotals()

        val coordinatorLayout = getParentCoordinatorLayout()
        // calculate sheet's expanded ahd halfExpanded sizes after views are attached
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                updateSheetExpandedHeights(coordinatorLayout, view)
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        coordinatorLayout.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            val deltaY = oldBottom - bottom
            val keyboardOpened = deltaY > 220
            val keyboardClosed = deltaY < -220

            if (keyboardOpened || keyboardClosed) {
                updateSheetExpandedHeights(coordinatorLayout, view)

                if (keyboardOpened) {
                    if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED
                        && !binding.responseCustomSize.hasFocus()
                    ) {
                        sheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                    }
                }
            }
        }
    }

    private fun setupSheet() {

        sheetBehavior.apply {
            state = BottomSheetBehavior.STATE_COLLAPSED
            isHideable = false
            isFitToContents = false

            var previousState = BottomSheetBehavior.STATE_HIDDEN
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (previousState == BottomSheetBehavior.STATE_EXPANDED) {
                        // save when leaving STATE_EXPANDED
                        saveOptions()
                    }
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {}
                        BottomSheetBehavior.STATE_EXPANDED -> {}
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {}
                        BottomSheetBehavior.STATE_COLLAPSED -> {}
                        BottomSheetBehavior.STATE_DRAGGING -> {}
                        BottomSheetBehavior.STATE_SETTLING -> {}
                    }
                    previousState = newState
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            })
        }
    }

    private fun setupInputs() {
        sharedViewModel.activeChatTree?.let {
            val options = it.options

            binding.toggleRequestType.check(if (options.streaming) binding.requestStream.id else binding.requestStandard.id)
            binding.toggleEnterKey.check(if (options.imeSubmit) binding.enterKeySubmit.id else binding.enterKeyNewLine.id)
            binding.toggleModeration.check(if (options.moderation) binding.moderationYes.id else binding.moderationNo.id)

            binding.toggleResponseWrap.check(when (options.responseWrap) {
                ChatTreeOptions.Wrap.WRAP -> binding.responseWrap.id
                ChatTreeOptions.Wrap.NOWRAP -> binding.responseNoWrap.id
                ChatTreeOptions.Wrap.CUSTOM -> binding.responseCustom.id
            })
            binding.responseCustomSize.setText(options.responseWrapSize.toString())
            if (options.responseWrap == ChatTreeOptions.Wrap.CUSTOM) {
                binding.responseCustomSize.isEnabled = true
            }

        }
    }

    private fun setupInputListeners() {
        // call listener to update IME keyboard when enter key toggle changes
        binding.toggleEnterKey.addOnButtonCheckedListener { _, checkedId, isChecked ->
            // this is run for each button with a true or false, can ignore one of the buttons since it's a toggle
            if (checkedId == binding.enterKeySubmit.id) {
                sharedViewModel.activeChatTree?.let {
                    it.options.imeSubmit = isChecked
                    listener.onChangeKeyboardEnter()
                }
            }
        }

        binding.toggleResponseWrap.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (checkedId == binding.responseCustom.id) {
                binding.responseCustomSize.isEnabled = isChecked
            }
            if (isChecked) {
                sharedViewModel.activeChatTree?.let {
                    when (checkedId) {
                        binding.responseWrap.id -> it.options.responseWrap = ChatTreeOptions.Wrap.WRAP
                        binding.responseNoWrap.id -> it.options.responseWrap = ChatTreeOptions.Wrap.NOWRAP
                        binding.responseCustom.id -> it.options.responseWrap = ChatTreeOptions.Wrap.CUSTOM
                    }
                    listener.onChangeResponseWrap()
                }
            }
        }

        var wrapChangeJob: Job? = null
        binding.responseCustomSize.doOnTextChanged { text, _, _, _ ->
            // debounce on text search
            val inputValue = text.toString().toIntOrNull() ?: 500
            sharedViewModel.activeChatTree?.options?.responseWrapSize = inputValue.coerceAtLeast(500)

            wrapChangeJob?.cancel()
            wrapChangeJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(3000)
                listener.onChangeResponseWrap()
            }
        }

    }

    private fun updateSheetExpandedHeights(parent: CoordinatorLayout, child: View) {
        val parentHeight = parent.height
        val dialogHeight = child.height
        val offsetHeight = parentHeight - dialogHeight

        val halfHeight = binding.tokenUseGroup.height + binding.dialogChatDragHandle.height
        val halfRatio: Float = halfHeight.toFloat() / parentHeight

        sheetBehavior.expandedOffset = offsetHeight
        sheetBehavior.halfExpandedRatio = halfRatio

        // Trigger a recalculation of the current state
        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else if (sheetBehavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
            sheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }
    }

    fun updateTokenTotals() {
        sharedViewModel.activeChatTree?.let {
            binding.tokensPrompt.text = getString(R.string.tokens_prompts, it.usage.promptTokens)
            binding.tokensResponse.text = getString(R.string.tokens_response, it.usage.completionTokens)
            binding.tokensTotal.text = getString(R.string.tokens_total, it.usage.totalTokens)
        }
    }

    private fun saveOptions() {
        sharedViewModel.activeChatTree?.let {
            val options = it.options
            options.streaming = binding.toggleRequestType.checkedButtonId == binding.requestStream.id
            options.imeSubmit = binding.toggleEnterKey.checkedButtonId == binding.enterKeySubmit.id
            options.moderation = binding.toggleModeration.checkedButtonId == binding.moderationYes.id
            lifecycleScope.launch {
                sharedViewModel.chatRepository.saveChatTree(it)
            }
        }
    }

    private fun getParentCoordinatorLayout(): CoordinatorLayout {
        var parentView: ViewParent? = binding.root.parent
        while (parentView != null && parentView !is CoordinatorLayout) {
            parentView = parentView.parent
        }
        return parentView as CoordinatorLayout
    }

    fun setListener(listener: ChatTreeOptionsClickListener) {
        this.listener = listener
    }

    interface ChatTreeOptionsClickListener {
        fun onChangeKeyboardEnter()
        fun onChangeResponseWrap()
        fun onSaveOptions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}