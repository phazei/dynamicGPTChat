package com.phazei.dynamicgptchat.chatnodes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.databinding.DialogChatTreeOptionsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatTreeOptionsDialog : Fragment() {

    private var _binding: DialogChatTreeOptionsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var sheetBehavior: BottomSheetBehavior<View>

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
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            //copy and configure the options
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            //save the tree
                        }
                        BottomSheetBehavior.STATE_DRAGGING -> {}
                        BottomSheetBehavior.STATE_SETTLING -> {}
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}