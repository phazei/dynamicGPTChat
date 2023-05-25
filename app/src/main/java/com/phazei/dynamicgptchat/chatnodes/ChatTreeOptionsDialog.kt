package com.phazei.dynamicgptchat.chatnodes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.databinding.DialogChatTreeOptionsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatTreeOptionsDialog : Fragment() {

    private var _binding: DialogChatTreeOptionsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var sheetBehavior: BottomSheetBehavior<View>
    private lateinit var listener: ChatTreeOptionsClickListener

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

        // does it need to be reloaded any time it's opened again?  I don't believe so
        setupInputs()

        binding.toggleEnterKey.addOnButtonCheckedListener { group, checkedId, isChecked ->
            // this is run for each button with a true or false, can ignore one of the buttons since it's a toggle
            if (checkedId == binding.enterKeySubmit.id) {
                sharedViewModel.activeChatTree?.let {
                    it.options.imeSubmit = isChecked
                    listener.onChangeKeyboardEnter()
                }
            }
        }

    }

    fun setListener(listener: ChatTreeOptionsClickListener) {
        this.listener = listener
    }

    fun setSheetBehavior(behavior: BottomSheetBehavior<View>) {
        sheetBehavior = behavior
        sheetBehavior.apply {
            state = BottomSheetBehavior.STATE_COLLAPSED
            isHideable = false

            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {}
                        BottomSheetBehavior.STATE_EXPANDED -> {}
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {}
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            saveOptions()
                        }
                        BottomSheetBehavior.STATE_DRAGGING -> {}
                        BottomSheetBehavior.STATE_SETTLING -> {}
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }
    }

    fun setupInputs() {
        sharedViewModel.activeChatTree?.let {
            val options = it.options

            binding.tokensPrompt.text = getString(R.string.tokens_prompts, it.usage.promptTokens)
            binding.tokensResponse.text = getString(R.string.tokens_response, it.usage.completionTokens)
            binding.tokensTotal.text = getString(R.string.tokens_total, it.usage.totalTokens)

            binding.toggleRequestType.check(if (options.streaming) binding.requestStream.id else binding.requestStandard.id)
            binding.toggleEnterKey.check(if (options.imeSubmit) binding.enterKeySubmit.id else binding.enterKeyNewLine.id)
            binding.toggleModeration.check(if (options.moderation) binding.moderationYes.id else binding.moderationNo.id)

        }
    }

    fun saveOptions() {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface ChatTreeOptionsClickListener {
        fun onChangeKeyboardEnter()
        fun onSaveOptions()
    }


}