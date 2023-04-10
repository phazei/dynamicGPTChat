package com.phazei.dynamicgptchat

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.*
import android.widget.ArrayAdapter
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.snackbar.Snackbar
import com.phazei.dynamicgptchat.databinding.FragmentChatTreeSettingsBinding
import com.phazei.utils.setChangeListener
import androidx.activity.addCallback


class ChatTreeSettingsFragment : Fragment() {
    // by utilizing `_binding` which is nullable,it eliminates the need for null checks
    //    when using `binding` and allows it to be cleared in onDestroy
    private var _binding: FragmentChatTreeSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatTree: ChatTree
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var backPressedOnce = false
    private var saved = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatTreeSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatTree = sharedViewModel.activeChatTree!!
        setupMenu()
        setupInputs()

        // saves items
        binding.saveChatSettingsButton.setOnClickListener {
            chatTree.gptSettings = getGPTSettingsModel()
            chatTree.title = binding.titleEditText.text.toString()
            checkModifiedSettings()
        }

        // if anything is modified, this will be sure to mark it as not saved
        // and change the background color as an indicator
        binding.chatSettings.setChangeListener {
            checkModifiedSettings()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (onBackPressed()) {
                // if it's a true onBackPressed, then disable this callback, and hit back again
                this.isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }


    private fun checkModifiedSettings() {
        // Put your logic here to compare editingGPTsettings and chatTree.GPTsettings
        // and update the background color if needed
        val settings = getGPTSettingsModel()
        if (chatTree.gptSettings == settings && chatTree.title == binding.titleEditText.text.toString()) {
            saved = true
            binding.chatSettings.setBackgroundColor(Color.TRANSPARENT)
        } else {
            saved = false
            binding.chatSettings.setBackgroundColor(Color.argb(20, 128, 64, 64))
        }
    }

    private fun getGPTSettingsModel(): GPTSettings {
        return GPTSettings(
            systemMessage = binding.systemMessageText.text.toString(),
            mode = binding.modeSpinner.selectedItem.toString(),
            model = binding.modelSpinner.selectedItem.toString(),
            temperature = binding.temperatureSlider.value,
            maxTokens = binding.maxTokensSlider.value.toInt(),
            topP = binding.topPSlider.value,
            frequencyPenalty = binding.frequencyPenaltySlider.value,
            presencePenalty = binding.presencePenaltySlider.value,
            bestOf = binding.bestOfSlider.value.toInt(),
            injectStartText = binding.injectStartText.text.toString(),
            injectRestartText = binding.injectRestartText.text.toString()
        )
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                // Handle for example visibility of menu items
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.removeItem(R.id.action_settings)
                // menu.clear()
                // menuInflater.inflate(R.menu.menu_chat_node_page, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == android.R.id.home) {
                    // let this work exactly the same as the back button
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupInputs() {
        // Retrieve the default settings from ChatTree
        val settings = chatTree.gptSettings

        // Populate the title input
        binding.titleEditText.setText(chatTree.title)

        // Populate the mode spinner
        val modeOptions = resources.getStringArray(R.array.mode_options)
        val modeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            modeOptions
        )
        binding.modeSpinner.adapter = modeAdapter
        binding.modeSpinner.setSelection(modeOptions.indexOf(settings.mode))

        // Populate the model spinner
        val modelOptions = resources.getStringArray(R.array.model_options)
        val modelAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            modelOptions
        )
        binding.modelSpinner.adapter = modelAdapter
        binding.modelSpinner.setSelection(modelOptions.indexOf(settings.model))

        // Populate the temperature slider
        binding.temperatureSlider.value = settings.temperature

        // Populate the max tokens slider
        binding.maxTokensSlider.value = settings.maxTokens.toFloat()

        // Populate the TopP slider
        binding.topPSlider.value = settings.topP

        // Populate the frequency penalty slider
        binding.frequencyPenaltySlider.value = settings.frequencyPenalty

        // Populate the presence penalty slider
        binding.presencePenaltySlider.value = settings.presencePenalty

        // Populate the best of slider
        binding.bestOfSlider.value = settings.bestOf.toFloat()

        // Populate the inject start text input
        binding.injectStartText.setText(settings.injectStartText)

        // Populate the inject restart text input
        binding.injectRestartText.setText(settings.injectRestartText)
    }

    private fun onBackPressed(): Boolean {
        if (!saved && !backPressedOnce) {
            Snackbar.make(binding.root, "Not saved, hit back again to leave", Snackbar.LENGTH_LONG)
                .setAction("") {}.show()
            backPressedOnce = true
            Handler(Looper.getMainLooper()).postDelayed({ backPressedOnce = false }, 2000)
            return false
        } else {
            // true press
            return true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
