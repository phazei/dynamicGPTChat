package com.phazei.dynamicgptchat

import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.phazei.dynamicgptchat.data.ChatTree
import com.phazei.dynamicgptchat.databinding.FragmentChatNodeListBinding


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ChatNodeListFragment : Fragment() {

    private var _binding: FragmentChatNodeListBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatTree: ChatTree
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val chatNodeViewModel: ChatNodeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatNodeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatTree = sharedViewModel.activeChatTree!!

        setupMenu()
        setupChatSubmitButton()

    private fun setupChatSubmitButton() {
        val drawableSendToStop = ContextCompat.getDrawable(requireContext(), R.drawable.avd_send_to_stop) as AnimatedVectorDrawable
        val drawableStopToSend = ContextCompat.getDrawable(requireContext(), R.drawable.avd_stop_to_send) as AnimatedVectorDrawable
        val drawableLoadingStop = ContextCompat.getDrawable(requireContext(), R.drawable.stop_and_load) as AnimatedVectorDrawable

        val animationCallback = object : Animatable2Compat.AnimationCallback() {
            //after button is animated to a stop, switch it to the loading stop button
            override fun onAnimationEnd(drawable: Drawable) {
                super.onAnimationEnd(drawable)
                binding.chatSubmitButton.icon = drawableLoadingStop
                drawableLoadingStop.start()
            }
        }
        AnimatedVectorDrawableCompat.registerAnimationCallback(drawableSendToStop, animationCallback)
        var sending = false

        binding.chatSubmitButton.setOnClickListener {
            // if (!chatNodeViewModel.isRequestActive(chatTree.id)) {
            //     chatNodeViewModel.makeChatCompletionRequest()
            // }

            if (!sending) {
                binding.chatSubmitButton.icon = drawableSendToStop
                drawableSendToStop.start()
                sending = true
            } else {
                binding.chatSubmitButton.icon = drawableStopToSend
                drawableStopToSend.start()
                sending = false
            }
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
                if (menuItem.itemId == R.id.action_chat_settings) {
                    findNavController().navigate(R.id.action_ChatNodeListFragment_to_ChatTreeSettingsFragment)
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}