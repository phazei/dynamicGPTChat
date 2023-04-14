package com.phazei.dynamicgptchat

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
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

    }

    private fun setupMenu() {
        (activity as AppCompatActivity).supportActionBar?.title = chatTree.title
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                // Handle for example visibility of menu items
            }
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.removeItem(R.id.action_settings)
                // menu.clear()
                menuInflater.inflate(R.menu.menu_chat_node_page, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Validate and handle the selected menu item
                if (menuItem.itemId == R.id.action_chat_settings) {
                    findNavController().navigate(R.id.action_ChatNodeListFragment_to_chatTreeSettingsFragment)
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