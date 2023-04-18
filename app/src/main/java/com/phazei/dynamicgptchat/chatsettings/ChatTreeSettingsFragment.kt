package com.phazei.dynamicgptchat.chatsettings

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.*
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.chattrees.ChatTreeViewModel
import com.phazei.dynamicgptchat.data.entity.ChatTree
import com.phazei.dynamicgptchat.data.entity.GPTSettings
import com.phazei.dynamicgptchat.databinding.FragmentChatTreeSettingsBinding
import com.phazei.utils.setChangeListener
import com.tomergoldst.tooltips.ToolTip
import com.tomergoldst.tooltips.ToolTipsManager


class ChatTreeSettingsFragment : Fragment() {
    // by utilizing `_binding` which is nullable,it eliminates the need for null checks
    //    when using `binding` and allows it to be cleared in onDestroy
    private var _binding: FragmentChatTreeSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatTree: ChatTree
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val chatTreeViewModel: ChatTreeViewModel by viewModels { ChatTreeViewModel.Companion.Factory(sharedViewModel.chatRepository) }
    private var backPressedOnce = false
    private var saved = true
    private lateinit var mToolTipsManager: ToolTipsManager
    private val dispatcher by lazy { requireActivity().onBackPressedDispatcher }

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
        mToolTipsManager = ToolTipsManager()

        setupMenu()
        setupInputs()
        setupToolTips()



        // saves items
        binding.saveChatSettingsButton.setOnClickListener {
            if (previousView != null) mToolTipsManager.findAndDismiss(previousView)
            it.requestFocus()
            chatTree.gptSettings = getGPTSettingsModel(chatTree.gptSettings)
            chatTree.title = binding.titleEditText.text.toString()
            chatTreeViewModel.saveChatTree(chatTree)
            chatTreeViewModel.saveGptSettings(chatTree.gptSettings)
            checkModifiedSettings()
        }

        // if anything is modified, this will be sure to mark it as not saved
        // and change the background color as an indicator
        binding.chatSettings.setChangeListener {
            checkModifiedSettings()
        }

        //prevent accidental back when not saved
        dispatcher.addCallback(viewLifecycleOwner) {
            if (onBackPressed()) {
                // if it's a true onBackPressed, then disable this callback, and hit back again
                this.isEnabled = false
                dispatcher.onBackPressed()
            }
        }
    }

    /**
     * If anything's changed, it will update the background color to indicate something has changed
     * and set "saved" to false
     */
    private fun checkModifiedSettings() {
        val settings = getGPTSettingsModel()
        val savedSettings = chatTree.gptSettings.copy(id = 0) //ID must be zero to match default
        if (savedSettings == settings && chatTree.title == binding.titleEditText.text.toString()) {
            saved = true
            binding.chatSettings.setBackgroundColor(Color.TRANSPARENT)
        } else {
            saved = false
            binding.chatSettings.setBackgroundColor(Color.argb(20, 128, 64, 64))
        }
    }

    private fun getGPTSettingsModel(gptSettings: GPTSettings? = null): GPTSettings {
        val settings = gptSettings ?: GPTSettings()
        return settings.copy(
            systemMessage = binding.systemMessageText.text.toString(),
            mode = binding.modeSpinner.selectedItem.toString(),
            model = binding.modelSpinner.selectedItem.toString(),
            temperature = binding.temperatureSlider.value,
            maxTokens = binding.maxTokensSlider.value.toInt(),
            topP = binding.topPSlider.value,
            frequencyPenalty = binding.frequencyPenaltySlider.value,
            presencePenalty = binding.presencePenaltySlider.value,
            stop = binding.stopText.text.toString(),
            n = binding.numberOfSlider.value.toInt(),
            bestOf = binding.bestOfSlider.value.toInt(),
            injectStartText = binding.injectStartText.text.toString(),
            injectRestartText = binding.injectRestartText.text.toString()
        )
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {}
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == android.R.id.home) {
                    // work as back button to trigger saved check callback
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupInputs() {
        //first setup change listener for all the sliders
        setupSliderText()

        // Retrieve the default settings from ChatTree
        val settings = chatTree.gptSettings

        // Populate the title input
        binding.titleEditText.setText(chatTree.title)

        binding.systemMessageText.setText(settings.systemMessage)

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

        // Populate the number of results slider
        binding.stopText.setText(settings.stop)

        // Populate the number of results slider
        binding.numberOfSlider.value = settings.n.toFloat()

        // Populate the best of slider
        binding.bestOfSlider.value = settings.bestOf.toFloat()

        // Populate the inject start text input
        binding.injectStartText.setText(settings.injectStartText)

        // Populate the inject restart text input
        binding.injectRestartText.setText(settings.injectRestartText)

    }

    //Update all slider labels to include the value next to the label text
    private fun setupSliderText() {
        val sliderIds = listOf(
            R.id.temperature_slider,
            R.id.max_tokens_slider,
            R.id.top_p_slider,
            R.id.frequency_penalty_slider,
            R.id.presence_penalty_slider,
            R.id.number_of_slider,
            R.id.best_of_slider
        )
        val textViewIds = listOf(
            R.id.temperature_text,
            R.id.max_tokens_text,
            R.id.top_p_text,
            R.id.frequency_penalty_text,
            R.id.presence_penalty_text,
            R.id.number_of_text,
            R.id.best_of_text
        )
        val stringResourceIds = listOf(
            R.string.temperature_label,
            R.string.max_tokens_label,
            R.string.top_p_label,
            R.string.frequency_penalty_label,
            R.string.presence_penalty_label,
            R.string.number_of_label,
            R.string.best_of_label
        )

        for (i in sliderIds.indices) {
            val slider = binding.root.findViewById<Slider>(sliderIds[i])
            val textView = binding.root.findViewById<TextView>(textViewIds[i])
            val stringResourceId = stringResourceIds[i]
            // Set initial text

            textView.text = getString(stringResourceId, slider.value)
            slider.addOnChangeListener { _, value, _ ->
                textView.text = getString(stringResourceId, value)
            }
        }
    }

    private fun setupToolTips() {
        //items with empty strings will only close the previous tooltip when triggered
        setupTooltip(binding.titleEditText, "")
        setupTooltip(binding.systemMessageText, getString(R.string.system_message_tooltip))
        setupTooltip(binding.modeSpinner, "")
        setupTooltip(binding.modelSpinner, getString(R.string.model_tooltip))
        setupTooltip(binding.temperatureSlider, getString(R.string.temperature_tooltip))
        setupTooltip(binding.maxTokensSlider, getString(R.string.max_tokens_tooltip))
        setupTooltip(binding.topPSlider, getString(R.string.top_p_tooltip))
        setupTooltip(binding.frequencyPenaltySlider, getString(R.string.frequency_penalty_tooltip))
        setupTooltip(binding.presencePenaltySlider, getString(R.string.presence_penalty_tooltip))
        setupTooltip(binding.stopText, getString(R.string.stop_text_tooltip))
        setupTooltip(binding.numberOfSlider, getString(R.string.number_of_tooltip))
        setupTooltip(binding.bestOfSlider, getString(R.string.best_of_tooltip))
        setupTooltip(binding.injectStartText, getString(R.string.inject_start_text_tooltip))
        setupTooltip(binding.injectRestartText, getString(R.string.inject_restart_text_tooltip))
    }

    private var previousView: View? = null
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTooltip(
        view: View,
        message: String,
        position: Int = ToolTip.POSITION_ABOVE
    ) {

        com.tomergoldst.tooltips.R.style.TooltipDefaultStyle
        val parentView = binding.chatSettingsBody
        val builder = ToolTip.Builder(requireContext(), view, parentView, message, position)
        val typedValue = TypedValue()
        view.context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        builder.setBackgroundColor(typedValue.data)
        builder.setTextAppearance(R.style.TooltipTextAppearance)

        var tipView: View? = null

        fun longTouch(v : View) {
            if (tipView == null || !mToolTipsManager.isVisible(tipView)) {
                //it hasn't been created, and it's not currently being shown
                if (message != "") {
                    //need to include empty messages so Focus listener will still close previous popup
                    tipView = mToolTipsManager.show(builder.build())
                }
            }
            if (previousView != null && previousView != v) {
                //it wasn't the one just opened
                mToolTipsManager.findAndDismiss(previousView)
            }
            previousView = v
        }
        //if this is set in the XML it's just ignored and doesn't work
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        when (view) {
            is Slider -> {
                view.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(slider: Slider) {
                        longTouch(slider)
                    }
                    override fun onStopTrackingTouch(slider: Slider) {
                        mToolTipsManager.findAndDismiss(previousView)
                    }
                })
            }
            else -> {
                view.setOnLongClickListener { v ->
                    longTouch(v)
                    true
                }
            }
        }
    }

    @Suppress("LiftReturnOrAssignment")
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
