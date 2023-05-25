package com.phazei.dynamicgptchat.prompts

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.data.pojo.PromptWithTags
import com.phazei.dynamicgptchat.data.entity.Tag
import com.phazei.dynamicgptchat.databinding.DialogPromptFormBinding
import com.phazei.taginputview.TagInputData
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch


class PromptFormDialog(private val promptWithTags: PromptWithTags) : DialogFragment() {

    private var _binding: DialogPromptFormBinding? = null
    private val binding get() = _binding!!
    private val promptsViewModel: PromptsViewModel by activityViewModels()

    private lateinit var drawableMenuToClose: AnimatedVectorDrawable
    private lateinit var drawableCloseToMenu: AnimatedVectorDrawable

    private lateinit var listener: OnPromptFormDialogListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_DynamicGPTChat_FullScreenDialog)
        val componentDialog = super.onCreateDialog(savedInstanceState)
        componentDialog.window?.setWindowAnimations(R.style.PromptFormDialogSlide)
        return componentDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        _binding = DialogPromptFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                // this should always have a value from the initial call in the PromptsListFragment
                promptsViewModel.tags
                    .filterNotNull()
                    .collect { tags ->
                        setupPromptTags(tags)
                    }
            }
        }

        binding.apply {
            toolbar.setNavigationOnClickListener { v -> dismiss() }
            toolbar.setOnMenuItemClickListener { item ->
                savePromptWithTags()
                true
            }

            // set main toolbar icon in advance
            drawableCloseToMenu = ContextCompat.getDrawable(view.context, R.drawable.avd_close_to_menu) as AnimatedVectorDrawable
            drawableCloseToMenu.setTint(ContextCompat.getColor(view.context, R.color.md_theme_light_surface))

            // animate previous menu to close
            drawableMenuToClose = ContextCompat.getDrawable(view.context, R.drawable.avd_menu_to_close) as AnimatedVectorDrawable
            drawableMenuToClose.setTint(ContextCompat.getColor(view.context, R.color.md_theme_light_surface))
            // set both
            (activity as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(drawableMenuToClose)
            toolbar.navigationIcon = drawableMenuToClose

            drawableMenuToClose.start()
        }

        setupPromptWithTags()

    }

    fun setOnPromptFormDialogListener(listener: OnPromptFormDialogListener) {
        this.listener = listener
    }

    fun setupPromptWithTags() {

        binding.toolbar.title = if (promptWithTags.prompt.id == 0L) getString(R.string.dialog_title_add) else getString(R.string.dialog_title_edit)

        binding.promptTitle.setText(promptWithTags.prompt.title)
        binding.promptBody.setText(promptWithTags.prompt.body)

        // tags are setup after full tag list is returned for auto-complete
    }

    private fun setupPromptTags(tags: List<Tag>) {

        val tagsAdapter = TagsArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tags)

        binding.promptTags.apply {
            setAutoCompleteAdapter(tagsAdapter)
            setTagInputData(object : TagInputData<Tag>() {
                override fun inputConverter(input: String): Tag {
                    // Check if tag exists. If so, use existing tag with id.
                    val foundTag = tagsAdapter.run {
                        (0 until count).asSequence()
                            .map { getItem(it) }
                            .firstOrNull { it.name == input }
                    }
                    // If foundTag is not null, it means the tag exists.
                    foundTag?.let {
                        return it
                    }
                    // If tag doesn't exist, create new one!
                    return Tag(input.ucWords())
                }
                override fun displayConverter(tag: Tag): String {
                    val tagTag = tag as Tag
                    return tagTag.name
                }
            })
            promptWithTags.tags.forEach { addTag(it) }
        }
    }

    private fun savePromptWithTags() {
        val newTitle = binding.promptTitle.text.toString()
        val newBody = binding.promptBody.text.toString()
        val newTags = binding.promptTags.getTags() as MutableList<Tag>

        if (newTitle.isNotBlank() && newBody.isNotBlank()) {
            promptWithTags.apply {
                prompt.title = newTitle
                prompt.body = newBody
                tags.clear()
                tags.addAll(newTags)
            }
            listener.onSavePrompt(promptWithTags)
            dismiss()
        } else {
            Snackbar.make(binding.root, "Please fill in all fields.", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        (activity as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(drawableCloseToMenu)
        binding.toolbar.navigationIcon = drawableCloseToMenu
        drawableCloseToMenu.start()
        super.onDismiss(dialog)
    }

    interface OnPromptFormDialogListener {
        fun onSavePrompt(promptWithTags: PromptWithTags)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun String.ucWords(): String {
        return this.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { it.uppercase() } }
    }

}
