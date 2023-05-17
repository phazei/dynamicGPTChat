package com.phazei.dynamicgptchat.prompts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phazei.dynamicgptchat.data.entity.Prompt
import com.phazei.dynamicgptchat.data.entity.PromptWithTags
import com.phazei.dynamicgptchat.data.entity.Tag
import com.phazei.dynamicgptchat.data.repo.PromptsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PromptsViewModel @Inject constructor(private val promptsRepository: PromptsRepository) : ViewModel() {

    private val _promptsWithTags = MutableSharedFlow<List<PromptWithTags>>()
    val promptsWithTags: Flow<List<PromptWithTags>> = _promptsWithTags.asSharedFlow()
    private val _tags = MutableSharedFlow<List<Tag>>()
    val tags: Flow<List<Tag>> = _tags.asSharedFlow()

    fun loadPromptsWithTags() {
        viewModelScope.launch {
            _promptsWithTags.emit(promptsRepository.loadPromptsWithTags())
        }
    }

    fun searchPromptsWithTags(string: String, tags: List<Tag>) {
        viewModelScope.launch {
            _promptsWithTags.emit(promptsRepository.searchPromptsWithTags(string, tags))
        }
    }

    fun loadAllTags() {
        viewModelScope.launch {
            _tags.emit(promptsRepository.loadTags())
        }
    }

    fun savePromptWithTags(promptWithTags: PromptWithTags) {
        viewModelScope.launch {
            promptsRepository.savePromptWithTags(promptWithTags)
        }
    }

    fun deletePrompt(prompt: Prompt) {
        viewModelScope.launch {
            promptsRepository.deletePrompt(prompt)
        }
    }
}