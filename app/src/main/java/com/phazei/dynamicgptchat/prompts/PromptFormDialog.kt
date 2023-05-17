package com.phazei.dynamicgptchat.prompts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import com.phazei.dynamicgptchat.R
import com.phazei.dynamicgptchat.data.entity.Prompt
import com.phazei.dynamicgptchat.data.entity.PromptWithTags
import com.phazei.dynamicgptchat.data.entity.Tag
import com.phazei.dynamicgptchat.databinding.DialogPromptFormBinding
import com.phazei.taginputview.TagInputData
import kotlin.math.roundToInt

class PromptFormDialog(private val promptWithTags: PromptWithTags) : DialogFragment() {

    private lateinit var listener: OnPromptFormDialogListener
    private lateinit var binding: DialogPromptFormBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogPromptFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPromptWithTags()

        // Set up the button click listener
        binding.promptSave.setOnClickListener {
            savePromptWithTags()
        }

        binding.promptClose.setOnClickListener {
            this.dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.window?.setLayout(width, height)

            it.setCanceledOnTouchOutside(false)
        }
    }

    fun setOnPromptFormDialogListener(listener: OnPromptFormDialogListener) {
        this.listener = listener
    }

    fun setupPromptWithTags() {

        binding.promptDialogTitle.text = if (promptWithTags.prompt.id == 0L) getString(R.string.dialog_title_add) else getString(R.string.dialog_title_edit)

        binding.promptTitle.setText(promptWithTags.prompt.title)
        binding.promptBody.setText(promptWithTags.prompt.body)
        binding.promptTags.apply {
            setTagInputData(object : TagInputData<Tag>() {
                override fun inputConverter(input: String): Tag? {
                    //check if tag exists, if so, use existing tag with id
                    return Tag(input.toString())
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

    interface OnPromptFormDialogListener {
        fun onSavePrompt(promptWithTags: PromptWithTags)
    }

}
